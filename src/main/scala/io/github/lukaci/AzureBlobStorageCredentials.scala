package io.github.lukaci

import java.io.{File, FileInputStream}
import java.util.Properties

/**
  * created by lukaci on 16/03/2019.
  */

object AzureBlobStorageCredentials {

  def fromEnv(accountName: String, envName: String): Either[List[String], String] = {
    sys.env.get(envName).toRight {
      List(s"[$envName] environment variable is not defined")
    }.flatMap { creds =>
      creds.split(':').toList.grouped(2).collectFirst {
        case `accountName` :: key :: Nil => key
      } toRight {
          List(s"Failed to extract credentials for [$accountName] from [$envName] environment variable. " +
            s"It should has following format: <ACCOUNT_NAME_1>:<SECRET_KEY_1>:<ACCOUNT_NAME_2>:<SECRET_KEY_2>:...")
      }
    }
  }


  def fromFile(fileName: String): Either[List[String], String] = {
    val file = new File(fileName)

    if (!file.exists()) {
      Left(List(s"File [$fileName] does not exist"))
    } else {
      val props = new Properties()
      val stm = new FileInputStream(file)
      try {
        props.load(stm)
      } finally {
        stm.close()
      }

      Option(props.getProperty("accountKey")).toRight {
        List(s"File [$fileName] does not contain 'accountKey' property")
      }
    }
  }
}
