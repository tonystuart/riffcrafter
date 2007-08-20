// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.services;

import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.library.services.Response;
import com.riffcrafter.library.util.XmlReader;
import com.riffcrafter.library.util.XmlWriter;


public class DownloadResponse implements Response
{
  private int status = Status.OKAY;
  private String message;
  private Midi midi;
  private String title;

  public void writeXml(XmlWriter xmlWriter, int level)
  {
    int nextLevel = level + 1;
    xmlWriter.beginTag(Tags.DOWNLOAD_RESPONSE, level);
    xmlWriter.writeTaggedInt(Tags.STATUS, status, nextLevel);
    xmlWriter.writeTaggedString(Tags.MESSAGE, message, nextLevel);
    xmlWriter.writeTaggedString(Tags.DOWNLOAD_TITLE, title, nextLevel);
    //midi.writeXml(xmlWriter, nextLevel);
    xmlWriter.endTag(Tags.DOWNLOAD_RESPONSE, level);
  }

  public void readXml(XmlReader xmlReader)
  {
    xmlReader.beginTag(Tags.DOWNLOAD_RESPONSE);
    status = xmlReader.readTaggedInt(Tags.STATUS);
    message = xmlReader.readTaggedString(Tags.MESSAGE);
    title = xmlReader.readTaggedString(Tags.DOWNLOAD_TITLE);
    midi = new Midi(xmlReader);
    xmlReader.endTag(Tags.DOWNLOAD_RESPONSE);
  }

  public String getMessage()
  {
    return message;
  }

  public int getStatus()
  {
    return status;
  }

  public String getTitle()
  {
    return title;
  }

  public Midi getNoteList()
  {
    return midi;
  }

  public void setMessage(String message)
  {
    this.message = message;
  }

  public void setStatus(int status)
  {
    this.status = status;
  }

  public void setTitle(String title)
  {
    this.title = title;
  }
  
  public void setNoteList(Midi midi)
  {
    this.midi = midi;
  }

}
