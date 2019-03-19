package io.github.lukaci

import scala.util.{Failure, Try}

/**
  * created by lukaci on 16/03/2019.
  */

trait AzureBlobStorageCredentialsProvider {
  def provide(accountName: String): AzureBlobStorageCredentials
}

object AzureBlobStorageCredentialsProvider {
  class ChainedAzureBlobStorageCredentialsProvider(providers: AzureBlobStorageCredentialsProvider*) extends AzureBlobStorageCredentialsProvider {
    override def provide(accountName: String): AzureBlobStorageCredentials = {
      val res = providers.foldLeft(Failure[AzureBlobStorageCredentials](new Exception("no provider found")).asInstanceOf[Try[AzureBlobStorageCredentials]]) { (acc, elem) =>
        acc.orElse {
          val tried = Try(elem.provide(accountName))
          tried.failed.foreach { ex => println(s"failed getting credentials from [$elem] for account [$accountName]") }
          tried
        }
      }
      // NOTE: this will throw the last exeption if failed retrieving through the entire chain
      res.get
    }
    override def toString: String = s"chained provider [${providers.mkString(",")}]"
  }

  val env: AzureBlobStorageCredentialsProvider = new AzureBlobStorageCredentialsProvider {
    override def provide(accountName: String): AzureBlobStorageCredentials = AzureBlobStorageCredentials.fromEnv(accountName)
    override def toString: String = "environment provider"
  }

  val file: AzureBlobStorageCredentialsProvider = new AzureBlobStorageCredentialsProvider {
    override def provide(accountName: String): AzureBlobStorageCredentials = AzureBlobStorageCredentials.fromFile(accountName)
    override def toString: String = s"file provider"
  }

  val default: AzureBlobStorageCredentialsProvider = new ChainedAzureBlobStorageCredentialsProvider(env, file)
}
