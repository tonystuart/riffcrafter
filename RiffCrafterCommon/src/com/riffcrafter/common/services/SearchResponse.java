// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.services;

import com.riffcrafter.library.services.Response;
import com.riffcrafter.library.util.XmlReader;
import com.riffcrafter.library.util.XmlWriter;


public class SearchResponse implements Response
{
  private int status = Status.OKAY;
  private String message;
  private Table table;

  public void writeXml(XmlWriter xmlWriter, int level)
  {
    if (table == null)
    {
      table = new Table();
    }

    int nextLevel = level + 1;
    xmlWriter.beginTag(Tags.SEARCH_RESPONSE, level);
    xmlWriter.writeTaggedInt(Tags.STATUS, status, nextLevel);
    xmlWriter.writeTaggedString(Tags.MESSAGE, message, nextLevel);
    table.writeXml(xmlWriter, nextLevel);
    xmlWriter.endTag(Tags.SEARCH_RESPONSE, level);
  }

  public void readXml(XmlReader xmlReader)
  {
    xmlReader.beginTag(Tags.SEARCH_RESPONSE);
    status = xmlReader.readTaggedInt(Tags.STATUS);
    message = xmlReader.readTaggedString(Tags.MESSAGE);
    table = new Table(xmlReader);
    xmlReader.endTag(Tags.SEARCH_RESPONSE);
  }

  public String getMessage()
  {
    return message;
  }

  public int getStatus()
  {
    return status;
  }

  public Table getTable()
  {
    return table;
  }

  public void setMessage(String message)
  {
    this.message = message;
  }

  public void setStatus(int status)
  {
    this.status = status;
  }

  public void setTable(Table table)
  {
    this.table = table;
  }

}
