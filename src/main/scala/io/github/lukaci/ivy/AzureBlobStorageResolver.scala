package io.github.lukaci.ivy

import io.github.lukaci.{AzureBlobStorageConfig, AzureBlobStorageCredentialsProvider}
import org.apache.ivy.plugins.resolver.RepositoryResolver

/**
  * created by lukaci on 16/03/2019.
  */

class AzureBlobStorageResolver(name: String, accountName: String, rootContainer: String, config: AzureBlobStorageConfig = AzureBlobStorageConfig.default, provider: AzureBlobStorageCredentialsProvider = AzureBlobStorageCredentialsProvider.default) extends RepositoryResolver {
  setName(name)
  setRepository(new IvyAzureBlobStorageRepository(accountName, rootContainer, config, provider))
  override def getTypeName = "azure-blob"
}
