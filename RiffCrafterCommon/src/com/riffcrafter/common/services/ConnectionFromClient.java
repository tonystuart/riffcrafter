// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.services;

import java.io.InputStream;
import java.io.OutputStream;

import com.riffcrafter.library.services.Request;
import com.riffcrafter.library.services.Response;
import com.riffcrafter.library.util.XmlReader;
import com.riffcrafter.library.util.XmlWriter;


public class ConnectionFromClient
{
  private XmlReader xmlReader;
  private XmlWriter xmlWriter;

  public ConnectionFromClient(InputStream inputStream, OutputStream outputStream)
  {
    xmlReader = new XmlReader(inputStream);
    xmlWriter = new XmlWriter(outputStream);
  }

  public Request receiveCommandRequest()
  {
    Request request = null;
    String tag = xmlReader.peekTag();
    if (tag.equals(Tags.SEARCH_REQUEST))
    {
      request = new SearchRequest(xmlReader);
    }
    else if (tag.equals(Tags.DOWNLOAD_REQUEST))
    {
      request = new DownloadRequest(xmlReader);
    }
    return request;
  }

  public void sendCommandResponse(Response commandResponse)
  {
    commandResponse.writeXml(xmlWriter, 0);
  }
}
