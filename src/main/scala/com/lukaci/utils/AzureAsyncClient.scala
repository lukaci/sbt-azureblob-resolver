package com.lukaci.utils

import java.io.{File, FileInputStream}
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.StandardOpenOption

import com.lukaci._
import com.lukaci.ivy.IvyAzureBlobStorageResource
import com.lukaci.utils.AsyncUtils._
import com.microsoft.azure.storage.blob._
import com.microsoft.azure.storage.blob.models.{BlobGetPropertiesResponse, BlobItem, ContainerListBlobHierarchySegmentResponse}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * created by lukaci on 18/03/2019.
  */
class AzureAsyncClient {
  def getResource()(implicit ref: AzureBlobStorageRef): Future[IvyAzureBlobStorageResource] = {
    val (_, f) = normalizesplit(ref.fileName)

    innerpeek map { rsp => resource(ref.fileName, rsp) } recoverWith { case _: Throwable =>
      // NOTE: simplicistic handling
      Future.successful(IvyAzureBlobStorageResource.NotFound(f))
    }
  }

  def get(destination: File)(implicit ref: AzureBlobStorageRef): Future[Unit] = {
    innerget(destination)
  }

  def list()(implicit ref: AzureBlobStorageRef): Future[Seq[IvyAzureBlobStorageResource]] = {
    innerlist(Some(ref.asDelimiter))
  }

  def put(source: File, overwrite: Boolean)(implicit ref: AzureBlobStorageRef): Future[Unit] = {
    innerput(source, overwrite)
  }

  private def innerpeek()(implicit ref: AzureBlobStorageRef): Future[BlobGetPropertiesResponse] = {
    ref.blockBlobUrl.getProperties(BlobAccessConditions.NONE, null).asScala
  }

  private def innerput(source: File, overwrite: Boolean)(implicit ref: AzureBlobStorageRef): Future[Unit] = {
    val channel = AsynchronousFileChannel.open(source.toPath)
    val url = ref.blockBlobUrl

    val check = if(!overwrite) innerpeek.transform {
      case Failure(_) => Success(())
      case Success(_) => Failure(new Exception("cannot overwrite, file exists and overwrite flag is false"))
    } else Future.successful(())

    check flatMap { _ => TransferManager.uploadFileToBlockBlob(channel, url, 8 * 1024 * 1024, null).asUnit }
  }

  private def innerget(file: File)(implicit ref: AzureBlobStorageRef): Future[Unit] = {
    val channel = AsynchronousFileChannel.open(file.toPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    val url = ref.blockBlobUrl

    TransferManager.downloadBlobToFile(channel, url, null, null).asUnit
  }

  private def innerlist(delimiter: Option[String], response: Option[ContainerListBlobHierarchySegmentResponse] = None, previtems: Seq[IvyAzureBlobStorageResource] = Nil)(implicit ref: AzureBlobStorageRef): Future[Seq[IvyAzureBlobStorageResource]] = {
    val items = previtems ++ response.flatMap(x => Option(x.body.segment)).toSeq.flatMap(_.blobItems.asScala.map(resource))
    val marker = response.flatMap(x => Option(x.body.nextMarker))

    if(response.isDefined && marker.isEmpty) Future.successful(items)
    else ref.rootContainerUrl.listBlobsHierarchySegment(marker.orNull, delimiter.orNull, new ListBlobsOptions().withMaxResults(ref.config.listBulkSize), null).asScala.flatMap(r => innerlist(delimiter, Some(r), items))
  }

  private def resource(item: BlobItem)(implicit ref: AzureBlobStorageRef): IvyAzureBlobStorageResource = {
    val (_, f) = normalizesplit(item.name)
    IvyAzureBlobStorageResource.Real(f, item.properties().lastModified().toEpochSecond * 1000, item.properties().contentLength(), { () =>
      val file = File.createTempFile("azure-storage-blob", "tmp")
      file.deleteOnExit()
      get(file)(ref.withFileName(item.name))
      new FileInputStream(file)
    })
  }

  private def resource(fullname: String, rsp: BlobGetPropertiesResponse)(implicit ref: AzureBlobStorageRef): IvyAzureBlobStorageResource = {
    val (_, f) = normalizesplit(fullname)
    IvyAzureBlobStorageResource.Real(f, rsp.headers().lastModified.toEpochSecond * 1000, rsp.headers.contentLength(), { () =>
      val file = File.createTempFile("azure-storage-blob", "tmp")
      file.deleteOnExit()
      get(file)(ref.withFileName(fullname))
      new FileInputStream(file)
    })
  }

  private def normalizesplit(arg: String)(implicit ref: AzureBlobStorageRef): (String, String) = {
    val splits = arg.stripPrefix(ref.config.prefix).stripPrefix(ref.rootContainer).split(Array('/', '\\')).filterNot(_.isEmpty)
    splits.dropRight(1).mkString("/") -> splits.last
  }
}
