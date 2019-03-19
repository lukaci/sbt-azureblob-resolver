package io.github.lukaci

import scala.concurrent.duration._

/**
  * created by lukaci on 16/03/2019.
  */

case class AzureBlobStorageConfig(listBulkSize: Int, longOpDuration: Duration, shortOpDuration: Duration, endpoint: String = "https://{ACCOUNT_NAME}.blob.core.windows.net", scheme: String = "blob") {
  def prefix: String = s"$scheme://"
}

object AzureBlobStorageConfig {
  val default: AzureBlobStorageConfig = AzureBlobStorageConfig(40, 30.minutes, 30.seconds)
}
