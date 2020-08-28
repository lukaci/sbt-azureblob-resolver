package io.github.lukaci

import com.microsoft.azure.storage.blob.{AnonymousCredentials, ICredentials, SharedKeyCredentials, TokenCredentials}
import sbt.ConsoleLogger

import scala.collection.concurrent.TrieMap

/**
 * created by lukaci on 16/03/2019.
 */

object CredentialsTypes extends Enumeration {
  val Token: CredentialsTypes.Value = Value("token")
  val SharedKey: CredentialsTypes.Value = Value("shared-key")
  val Anon: CredentialsTypes.Value = Value("anon")
  val All: CredentialsTypes.Value = Value("all")
}

trait AzureBlobStorageCredentialsProvider {
  def credentialsType: CredentialsTypes.Value

  def provide(accountName: String): Either[List[String], ICredentials]

  protected def toCredentials(accountName: String, value: String): ICredentials = {
    credentialsType match {
      case CredentialsTypes.Token => new TokenCredentials(value)
      case CredentialsTypes.SharedKey => new SharedKeyCredentials(accountName, value)
      case t => throw new IllegalArgumentException(s"Unable to create credentials of type [$t]")
    }
  }

  private val credentialsCache = TrieMap[String, Either[List[String], ICredentials]]()

  protected def withCache(accountName: String)
                         (value: => Either[List[String], ICredentials]): Either[List[String], ICredentials] = {
    credentialsCache.getOrElseUpdate(accountName, value)
  }
}

object AzureBlobStorageCredentialsProvider {

  private val logger = ConsoleLogger(System.out)

  class ChainedAzureBlobStorageCredentialsProvider(providers: AzureBlobStorageCredentialsProvider*)
    extends AzureBlobStorageCredentialsProvider {

    override val credentialsType: CredentialsTypes.Value = CredentialsTypes.All

    override def provide(accountName: String): Either[List[String], ICredentials] =
      withCache(accountName) {
        providers
          .foldLeft[Either[List[String], ICredentials]](Left(List())) { (result, provider) =>
            result.left.flatMap { previousErrors =>
              provider.provide(accountName) match {
                case Right(credentials) =>
                  if (previousErrors.nonEmpty) {
                    logger.info(
                      s"Errors from other failed credentials providers for [$accountName] Azure BLOB storage were: "
                    )
                    previousErrors.foreach(error => logger.info(s"\t$error"))
                  }
                  Right(credentials)
                case Left(errors) => Left(previousErrors ++ errors)
              }
            }
          }
          .left.map { errors =>
          if (errors.isEmpty) {
            List("No credentials providers given!")
          } else {
            errors
          }
        }
      }

    override def toString: String = s"chained provider [${providers.mkString(",")}]"
  }

  class EnvironmentAzureBlobStorageCredentialsProvider(variableName: String,
                                                       val credentialsType: CredentialsTypes.Value) extends AzureBlobStorageCredentialsProvider {


    override def provide(accountName: String): Either[List[String], ICredentials] = withCache(accountName) {
      AzureBlobStorageCredentials
        .fromEnv(accountName, variableName)
        .map { key =>
          logger.info(s"Using $credentialsType credentials from [$variableName] environment variable " +
            s"to access [$accountName] Azure BLOB storage")
          toCredentials(accountName, key)
        }
    }

    override def toString: String = s"environment ($credentialsType) provider"
  }

  class FileAzureBlobStorageCredentialsProvider(filePostfix: String,
                                                val credentialsType: CredentialsTypes.Value) extends AzureBlobStorageCredentialsProvider {

    override def provide(accountName: String): Either[List[String], ICredentials] = withCache(accountName) {
      val fileName = s".$accountName.$filePostfix"

      AzureBlobStorageCredentials
        .fromFile(fileName)
        .map { key =>
          logger.info(s"Using $credentialsType credentials from [$fileName] file " +
            s"to access [$accountName] Azure BLOB storage")
          toCredentials(accountName, key)
        }
    }

    override def toString: String = s"file ($credentialsType) provider"
  }

  val tokenEnv: AzureBlobStorageCredentialsProvider =
    new EnvironmentAzureBlobStorageCredentialsProvider("SBT_BLOB_TOKEN_CREDENTIALS", CredentialsTypes.Token)

  val sharedKeyEnv: AzureBlobStorageCredentialsProvider =
    new EnvironmentAzureBlobStorageCredentialsProvider("SBT_BLOB_SHARED_KEY_CREDENTIALS", CredentialsTypes.SharedKey)

  val tokenFile: AzureBlobStorageCredentialsProvider =
    new FileAzureBlobStorageCredentialsProvider("sbt-blob-token-credentials", CredentialsTypes.Token)

  val sharedKeyFile: AzureBlobStorageCredentialsProvider =
    new FileAzureBlobStorageCredentialsProvider("sbt-blob-shared-key-credentials", CredentialsTypes.SharedKey)

  val anon: AzureBlobStorageCredentialsProvider =
    new AzureBlobStorageCredentialsProvider {

      override val credentialsType: CredentialsTypes.Value = CredentialsTypes.Anon

      override def provide(accountName: String): Either[List[String], ICredentials] = withCache(accountName) {
        logger.info(s"Using anonymous credentials to access [$accountName] Azure BLOB storage")
        Right(new AnonymousCredentials())
      }

      override def toString: String = s"anon provider"
    }

  val default: AzureBlobStorageCredentialsProvider =
    new ChainedAzureBlobStorageCredentialsProvider(tokenEnv, sharedKeyEnv, tokenFile, sharedKeyFile, anon)
}
