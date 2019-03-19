package com.lukaci

import scala.concurrent.duration._

/**
  * created by lukaci on 16/03/2019.
  */

case class AzureBlobStorageConfig(prefix: String, listBulkSize: Int, longOpDuration: Duration, shortOpDuration: Duration, endpoint: String = "https://{ACCOUNT_NAME}.blob.core.windows.net", scheme: String = "blob")

object AzureBlobStorageConfig {
  val default: AzureBlobStorageConfig = AzureBlobStorageConfig("blob://", 40, 30.minutes, 30.seconds)
}
