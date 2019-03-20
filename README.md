# Sbt Azure BlobStorage resolver

[![Build Status](https://travis-ci.com/lukaci/sbt-azureblob-resolver.svg?branch=master)](https://travis-ci.com/lukaci/sbt-azureblob-resolver)

Plugin to ease resolving dependencies from and publish to Azure BlobStorage containers, using custom url syntax blob:// (default).

Thanks to [`ohnosequences`](https://github.com/ohnosequences/sbt-s3-resolver) [`gkatzioura`](https://github.com/gkatzioura/CloudStorageMaven) [`frugalmechanic`](https://github.com/frugalmechanic/fm-sbt-s3-resolver) for the job done on other storage providers

## SBT 1.1+ Support

SBT 1.1 support is available using version `>= 0.10.0`:

```scala
addSbtPlugin("io.github.lukaci" %% "sbt-azureblob-resolver" % "0.10.0")
```

## Examples

### Resolving

Maven Style:

```scala
resolvers += "Blob Snapshots" at "blob://youraccountname/snapshots"
```

Ivy Style:

```scala
resolvers += Resolver.url("Blob Snapshots", url("blob://youraccountname/snapshots"))(Resolver.ivyStylePatterns)
```

### Publishing

Maven Style:

```scala
publishMavenStyle := true
publishTo := Some("Blob Snapshots" at "blob://youraccountname/snapshots")
```

Ivy Style:

```scala
publishMavenStyle := false
publishTo := Some(Resolver.url("Blob Snapshots", url("blob://youraccountname/snapshots"))(Resolver.ivyStylePatterns))
```

### Valid blob:// URL Formats

    blob://[ACCOUNTNAME]/[ROOT_CONTAINER]

## Usage

### Add this to your project/plugins.sbt file:

```scala
addSbtPlugin("io.github.lukaci" %% "sbt-azureblob-resolver" % "0.10.0")
```

### Azure BlobStorage Credentials

Credentials are checked in 
 1. Environment Variable 
 2. Specific account name property files

#### Environment Variable

    BLOB_CREDENTIALS=<ACCOUNT_NAME_1>:<SECRET_KEY_1>:<ACCOUNT_NAME_2>:<SECRET_KEY_2>:...
    
#### Specific Property Files

```shell
.<account_name>.blob-credentials
```

containing

```shell
accountKey=XXXXXX
```

### Custom Credentials

If the default credential providers are not enough for you you can specify your own CredentialsProvider using the `blobCredentialsProvider` SettingKey in your `build.sbt` file:

```scala
blobCredentialsProvider := { (accountName: String) =>
   ...
   AzureBlobStorageCredentials(name = accountName, key = "YYYY")
}
```

## Authors

lukaci (<a href="https://github.com/lukaci" rel="author">GitHub</a>)

## License

[GNU General Public License, Version 3.0](https://www.gnu.org/licenses/gpl.txt)