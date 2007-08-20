// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.services;

import com.riffcrafter.library.services.Request;
import com.riffcrafter.library.util.XmlReader;
import com.riffcrafter.library.util.XmlWriter;


public class DownloadRequest implements Request
{
  private int id;

  public DownloadRequest()
  {
  }

  public DownloadRequest(XmlReader xmlReader)
  {
    readXml(xmlReader);
  }

  public void writeXml(XmlWriter xmlWriter, int level)
  {
    int nextLevel = level + 1;
    xmlWriter.beginTag(Tags.DOWNLOAD_REQUEST, level);
    xmlWriter.writeTaggedInt(Tags.ID, id, nextLevel);
    xmlWriter.endTag(Tags.DOWNLOAD_REQUEST, level);
  }

  public void readXml(XmlReader xmlReader)
  {
    xmlReader.beginTag(Tags.DOWNLOAD_REQUEST);
    id = xmlReader.readTaggedInt(Tags.ID);
    xmlReader.endTag(Tags.DOWNLOAD_REQUEST);
  }

  public String toString()
  {
    return "[id=" + id + "]";
  }

  public void setId(int id)
  {
    this.id = id;
  }

  public int getId()
  {
    return id;
  }


}
