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


public class SearchRequest implements Request
{
  private String title;
  private String artist;
  private String afterDate;
  private String beforeDate;
  private String user;

  public SearchRequest()
  {
  }

  public SearchRequest(XmlReader xmlReader)
  {
    readXml(xmlReader);
  }

  public void writeXml(XmlWriter xmlWriter, int level)
  {
    int nextLevel = level + 1;
    xmlWriter.beginTag(Tags.SEARCH_REQUEST, level);
    xmlWriter.writeTaggedString(Tags.TITLE, title, nextLevel);
    xmlWriter.writeTaggedString(Tags.ARTIST, artist, nextLevel);
    xmlWriter.writeTaggedString(Tags.AFTER_DATE, afterDate, nextLevel);
    xmlWriter.writeTaggedString(Tags.BEFORE_DATE, beforeDate, nextLevel);
    xmlWriter.writeTaggedString(Tags.USER, user, nextLevel);
    xmlWriter.endTag(Tags.SEARCH_REQUEST, level);
  }

  public void readXml(XmlReader xmlReader)
  {
    xmlReader.beginTag(Tags.SEARCH_REQUEST);
    title = xmlReader.readTaggedString(Tags.TITLE);
    artist = xmlReader.readTaggedString(Tags.ARTIST);
    afterDate = xmlReader.readTaggedString(Tags.AFTER_DATE);
    beforeDate = xmlReader.readTaggedString(Tags.BEFORE_DATE);
    user = xmlReader.readTaggedString(Tags.USER);
    xmlReader.endTag(Tags.SEARCH_REQUEST);
  }

  public String toString()
  {
    return "[title=" + title + ", artist=" + artist + ", afterDate=" + afterDate + ", beforeDate=" + beforeDate + ", user=" + user + "]";
  }

  public void setTitle(String title)
  {
    this.title = title;
  }

  public void setArtist(String artist)
  {
    this.artist = artist;
  }

  public void setBeforeDate(String beforeDate)
  {
    this.beforeDate = beforeDate;
  }

  public void setAfterDate(String afterDate)
  {
    this.afterDate = afterDate;
  }

  public void setUser(String user)
  {
    this.user = user;
  }

  public String getAfterDate()
  {
    return afterDate;
  }

  public String getArtist()
  {
    return artist;
  }

  public String getBeforeDate()
  {
    return beforeDate;
  }

  public String getTitle()
  {
    return title;
  }

  public String getUser()
  {
    return user;
  }

}
