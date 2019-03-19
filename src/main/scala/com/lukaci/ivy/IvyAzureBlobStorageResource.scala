package com.lukaci.ivy

import java.io.{IOException, InputStream}

import org.apache.ivy.plugins.repository.Resource

/**
  * created by lukaci on 16/03/2019.
  */

sealed trait IvyAzureBlobStorageResource extends Resource

object IvyAzureBlobStorageResource {
  case class Real(name: String, lastModified: Long, contentLength: Long, getter: () => InputStream) extends IvyAzureBlobStorageResource {
    override def getName: String = name
    override def getLastModified: Long = lastModified
    override def getContentLength: Long = contentLength
    override def isLocal: Boolean = false
    override def exists: Boolean = true
    override def clone(cloneName: String): Resource = copy(name = cloneName)
    override def openStream(): InputStream = getter()
  }

  case class NotFound(name: String) extends IvyAzureBlobStorageResource {
    override def getName: String = name
    override def getLastModified: Long = 0
    override def getContentLength: Long = 0
    override def exists(): Boolean = false
    override def isLocal: Boolean = false
    override def clone(cloneName: String): Resource = copy(name = cloneName)
    override def openStream(): InputStream = throw new IOException(s"cannot open resource [$name], does not exists")
  }
}