package io.github.lukaci.ivy

import java.io.File

import io.github.lukaci.utils.AsyncUtils.{AtMost, _}
import io.github.lukaci.utils.AzureAsyncClient
import io.github.lukaci.{AzureBlobStorageConfig, AzureBlobStorageCredentialsProvider, AzureBlobStorageRef}
import org.apache.ivy.plugins.repository.{AbstractRepository, Resource}

import scala.collection.JavaConverters._

/**
  * created by lukaci on 15/03/2019.
  */

class IvyAzureBlobStorageRepository(accountName: String, rootContainer: String, config: AzureBlobStorageConfig = AzureBlobStorageConfig.default, provider: AzureBlobStorageCredentialsProvider) extends AbstractRepository {
  private val longOp = AtMost(config.longOpDuration)
  private val shortOp = AtMost(config.shortOpDuration)

  private lazy val client: AzureAsyncClient = new AzureAsyncClient

  override def getResource(source: String): Resource = {
    implicit val ref: AzureBlobStorageRef = AzureBlobStorageRef.fromResource(source, accountName, rootContainer, config, provider)
    implicit val within: AtMost = shortOp

    client.getResource.await
  }

  override def get(source: String, destination: File): Unit = {
    implicit val ref: AzureBlobStorageRef = AzureBlobStorageRef.fromResource(source, accountName, rootContainer, config, provider)
    implicit val within: AtMost = longOp

    client.get(destination).await
  }

  override def list(parent: String): java.util.List[String] = {
    implicit val ref: AzureBlobStorageRef = AzureBlobStorageRef.fromResource(parent, accountName, rootContainer, config, provider)
    implicit val within: AtMost = shortOp

    client.list.await.map(_.getName).asJava
  }

  override protected def put(source: File, destination: String, overwrite: Boolean): Unit = {
    implicit val ref: AzureBlobStorageRef = AzureBlobStorageRef.fromResource(destination, accountName, rootContainer, config, provider)
    implicit val within: AtMost = longOp

    client.put(source, overwrite).await
  }
}
