package info.lukaci

import java.io.{File, FileInputStream}
import java.util.Properties

/**
  * created by lukaci on 16/03/2019.
  */

case class AzureBlobStorageCredentials(name: String, key: String)

object AzureBlobStorageCredentials {
  def fromEnv(accountName: String): AzureBlobStorageCredentials = {
    val credentials = sys.env.getOrElse("BLOB_CREDENTIALS", { throw new IllegalArgumentException(s"environment variable BLOB_CREDENTIALS not found") })

    val keyVal = credentials.split(':').toList.grouped(2).collectFirst { case `accountName` :: key :: Nil => key } getOrElse { throw new IllegalArgumentException(s"property [$accountName:<key>] not found environment variable BLOB_CREDENTIALS") }

    AzureBlobStorageCredentials(key = keyVal, name = accountName)
  }

  def fromFile(accountName: String, postfix: String = "blob-credentials"): AzureBlobStorageCredentials = {
    val filename = s".$accountName.$postfix"
    val fil = new File(filename)

    if(!fil.exists()) throw new IllegalArgumentException(s"given file [$filename] does not exist")

    val props = new Properties()
    val stm = new FileInputStream(fil)
    props.load(stm)
    stm.close()

    val keyVal = Option(props.getProperty("accountKey")) getOrElse { throw new IllegalArgumentException(s"given file [$filename] does not contain property 'accountKey'") }

    AzureBlobStorageCredentials(key = keyVal, name = accountName)
  }
}
