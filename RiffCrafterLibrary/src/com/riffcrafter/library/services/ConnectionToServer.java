// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.riffcrafter.library.util.XmlReader;
import com.riffcrafter.library.util.XmlWriter;


public class ConnectionToServer
{
  private URL serverUrl;

  public ConnectionToServer(URL serverUrl)
  {
    this.serverUrl = serverUrl;
  }

  public void invoke(Request request, Response response)
  {
    OutputStream outputStream = null;
    InputStream inputStream = null;

    try
    {
      HttpURLConnection connection = (HttpURLConnection)serverUrl.openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      outputStream = connection.getOutputStream();
      XmlWriter xmlWriter = new XmlWriter(outputStream);
      request.writeXml(xmlWriter, 0);
      inputStream = connection.getInputStream();
      XmlReader xmlReader = new XmlReader(inputStream);
      response.readXml(xmlReader);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
    finally
    {
      try
      {
        if (outputStream != null)
        {
          outputStream.close();
        }
        if (inputStream != null)
        {
          inputStream.close();
        }
      }
      catch (Exception e)
      {

      }
    }
  }
}
