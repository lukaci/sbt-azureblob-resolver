package io.github.lukaci.ivy

import java.io.{File, FileInputStream, InputStream}
import java.net._

import io.github.lukaci.utils.AsyncUtils.{AtMost, _}
import io.github.lukaci.utils.AzureAsyncClient
import io.github.lukaci.{AzureBlobStorageConfig, AzureBlobStorageCredentialsProvider, AzureBlobStorageRef}
import org.apache.ivy.util.url.{URLHandler, URLHandlerDispatcher, URLHandlerRegistry}
import org.apache.ivy.util.{CopyProgressEvent, CopyProgressListener}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.util.{Success, Try}

/**
  * created by lukaci on 15/03/2019.
  */

class IvyAzureBlobStorageURLHandler(config: AzureBlobStorageConfig = AzureBlobStorageConfig.default, provider: AzureBlobStorageCredentialsProvider = AzureBlobStorageCredentialsProvider.default) extends URLHandler {
  private lazy val client = new AzureAsyncClient()

  override def isReachable(url: URL): Boolean = isReachable(url, config.shortOpDuration.toMillis.toInt)
  override def getContentLength(url: URL): Long = getContentLength(url, config.shortOpDuration.toMillis.toInt)
  override def getLastModified(url: URL): Long = getLastModified(url, config.shortOpDuration.toMillis.toInt)
  override def getURLInfo(url: URL): URLHandler.URLInfo = getURLInfo(url, config.shortOpDuration.toMillis.toInt)
  override def setRequestMethod(requestMethod: Int): Unit = ()

  override def isReachable(url: URL, timeout: Int): Boolean = getURLInfo(url, timeout).isReachable
  override def getContentLength(url: URL, timeout: Int): Long = getURLInfo(url, timeout).getContentLength
  override def getLastModified(url: URL, timeout: Int): Long = getURLInfo(url, timeout).getLastModified

  override def openStream(url: URL): InputStream = {
    implicit val tmo: AtMost = AtMost(config.longOpDuration)
    implicit val ref: AzureBlobStorageRef = AzureBlobStorageRef.fromUrl(url, config, provider)
    val file = File.createTempFile("azure-storage-blob", "tmp")
    this.client.get(file).await
    new FileInputStream(file)
  }
  override def download(src: URL, dest: File, l: CopyProgressListener): Unit = {
    implicit val tmo: AtMost = AtMost(config.longOpDuration)
    implicit val ref: AzureBlobStorageRef = AzureBlobStorageRef.fromUrl(src, config, provider)

    val evt = new CopyProgressEvent()

    if(l != null) l.start(evt)
    val res = this.client.get(dest)
    res.onComplete(_ => if(l != null) l.end(evt))
    res.await
  }
  override def upload(src: File, dest: URL, l: CopyProgressListener): Unit = {
    implicit val tmo: AtMost = AtMost(config.longOpDuration)
    implicit val ref: AzureBlobStorageRef = AzureBlobStorageRef.fromUrl(dest, config, provider)

    val evt = new CopyProgressEvent()

    if(l != null) l.start(evt)
    val res = this.client.put(src, overwrite = true)
    res.onComplete(_ => if(l != null) l.end(evt))
    res.await
  }
  override def getURLInfo(url: URL, timeout: Int): URLHandler.URLInfo = urlInfo { Try {
    implicit val tmo: AtMost = AtMost(timeout.milliseconds)
    implicit val ref: AzureBlobStorageRef = AzureBlobStorageRef.fromUrl(url, config, provider)
    this.client.getResource.await
  } }

  private def urlInfo(resource: Try[IvyAzureBlobStorageResource]): URLHandler.URLInfo = resource match {
    case Success(IvyAzureBlobStorageResource.Real(_, lastModified, contentLength, _)) => new URLInfoImpl(true, contentLength, lastModified)
    case _ => URLHandler.UNAVAILABLE
  }

  private class URLInfoImpl(available: Boolean, contentLength: Long, lastModified: Long) extends URLHandler.URLInfo(available, contentLength, lastModified)
}

object IvyAzureBlobStorageURLHandler {
  def install(config: AzureBlobStorageConfig = AzureBlobStorageConfig.default, provider: AzureBlobStorageCredentialsProvider = AzureBlobStorageCredentialsProvider.default): Unit = {
    installStreamHandler(config, provider)
    installDispatcher(config, provider)
  }

  private def installDispatcher(config: AzureBlobStorageConfig = AzureBlobStorageConfig.default, provider: AzureBlobStorageCredentialsProvider = AzureBlobStorageCredentialsProvider.default): Unit = {
    val dispatcher: URLHandlerDispatcher = URLHandlerRegistry.getDefault match {
      case disp: URLHandlerDispatcher =>
        println("Using the existing Ivy URLHandlerDispatcher to handle blob:// URLs")
        disp
      case default =>
        println("Creating a new Ivy URLHandlerDispatcher to handle blob:// URLs")
        val disp: URLHandlerDispatcher = new URLHandlerDispatcher()
        disp.setDefault(default)
        URLHandlerRegistry.setDefault(disp)
        disp
    }

    dispatcher.setDownloader("blob", new IvyAzureBlobStorageURLHandler(config, provider))
  }

  private def installStreamHandler(config: AzureBlobStorageConfig = AzureBlobStorageConfig.default, provider: AzureBlobStorageCredentialsProvider = AzureBlobStorageCredentialsProvider.default): Unit = {
    Try {
      new URL(s"${config.scheme}://test")
      println(s"The ${config.scheme} URLStreamHandler is already installed")
    } recoverWith { case _: java.net.MalformedURLException =>
      println(s"Installing the ${config.scheme} URLStreamHandler via java.net.URL.setURLStreamHandlerFactory")
      URL.setURLStreamHandlerFactory(proto =>
        if(config.scheme != proto) null
        else (url: URL) => {
          println(s"opening connection for proto [$proto] url [$url]")
          throw new UnsupportedOperationException(s"factory not implemented, only placeholder to allow registering [${config.scheme}] urls")
        })
      Success(())
    }
  }
}
