package io.github.lukaci

import io.github.lukaci.ivy.IvyAzureBlobStorageURLHandler
import sbt.Keys._
import sbt.{AutoPlugin, PluginTrigger, Setting, settingKey, _}

object SBTAzureBlobStorageResolver extends AutoPlugin {
  object autoImport {
    lazy val blobCredentialsProvider = settingKey[AzureBlobStorageCredentialsProvider]("Blob credentials provider")
    lazy val blobConfig = settingKey[AzureBlobStorageConfig]("Blob configuration")
  }

  import autoImport._

  // automatic plugin loading
  override def trigger: PluginTrigger = allRequirements

  // default settings
  override def projectSettings: Seq[Setting[_]] = Seq(
    onLoad in Global := (onLoad in Global).value  andThen { state =>
      val extracted: Extracted = Project.extract(state)
      val config = extracted.getOpt(blobConfig).getOrElse(AzureBlobStorageConfig.default)
      val provider = extracted.getOpt(blobCredentialsProvider).getOrElse(AzureBlobStorageCredentialsProvider.default)

      println("installing BLOB storage url handler")

      IvyAzureBlobStorageURLHandler.install(config, provider)

      state
    }
  )
}
