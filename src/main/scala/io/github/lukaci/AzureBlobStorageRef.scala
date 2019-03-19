package io.github.lukaci

import java.net.{URI, URL}

import com.microsoft.azure.storage.blob._

/**
  * created by lukaci on 18/03/2019.
  */

case class AzureBlobStorageRef(accountName: String, rootContainer: String, fileName: String, config: AzureBlobStorageConfig, provider: AzureBlobStorageCredentialsProvider) {
  def serviceUrl: ServiceURL = {
    val cred = provider.provide(accountName)
    val creds: SharedKeyCredentials = new SharedKeyCredentials(accountName, cred.key)
    new ServiceURL(new URL(config.endpoint.replace("{ACCOUNT_NAME}", accountName)), StorageURL.createPipeline(creds, new PipelineOptions()))
  }

  def rootContainerUrl: ContainerURL = serviceUrl.createContainerURL(rootContainer)

  def blockBlobUrl: BlockBlobURL = rootContainerUrl.createBlockBlobURL(normalize(fileName))

  def asDelimiter: String = normalize(fileName)

  def withFileName(file: String): AzureBlobStorageRef = copy(fileName = file)

  private def normalize(arg: String): String = {
    arg.stripPrefix(config.prefix).stripPrefix(rootContainer).split(Array('/', '\\')).filterNot(_.isEmpty).mkString("/")
  }
}

object AzureBlobStorageRef {
  def fromUrl(url: URL, config: AzureBlobStorageConfig = AzureBlobStorageConfig.default, provider: AzureBlobStorageCredentialsProvider = AzureBlobStorageCredentialsProvider.default): AzureBlobStorageRef = {
    val uri: URI = url.toURI

    if (uri.getScheme != config.scheme) throw new IllegalArgumentException(s"illegal uri scheme: [${uri.getScheme}] expected [${config.scheme}]")

    val accName = uri.getHost
    val pathName = uri.getPath
    val (rootContainer, fileName) = splitpath(pathName)

    AzureBlobStorageRef(accName, rootContainer, fileName, config, provider)
  }

  def fromResource(fileName: String, accountName: String, rootContainer: String, config: AzureBlobStorageConfig = AzureBlobStorageConfig.default, provider: AzureBlobStorageCredentialsProvider = AzureBlobStorageCredentialsProvider.default): AzureBlobStorageRef = {
    AzureBlobStorageRef(accountName, rootContainer, fileName, config, provider)
  }

  private def splitpath(arg: String): (String, String) = {
    val splits = arg.split(Array('/', '\\')).filterNot(_.isEmpty)
    splits.head -> splits.tail.mkString("/")
  }
}
