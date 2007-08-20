// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class XmlReader extends BufferedInputStream
{
  public enum States { BEGIN, AMPERSAND
  
  }

  private int inputPosition;
  private String unreadBuffer = "";
  private int unreadIndex;

  public XmlReader(File file) throws FileNotFoundException
  {
    super(new FileInputStream(file));
  }

  public XmlReader(InputStream inputStream)
  {
    super(inputStream);
  }

  public static XmlReader open(File file)
  {
    try
    {
      return new XmlReader(file);
    }
    catch (FileNotFoundException e)
    {
      throw new RuntimeException(e);
    }
  }
  
  public void close()
  {
    try
    {
      super.close();
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  public void beginTag(String expectedTag)
  {
    String tag = getNextTag();
    if (!tag.equals(expectedTag))
    {
      throw unexpectedInputException(expectedTag, tag);
    }
  }

  public void endTag(String expectedTag)
  {
    String tag = getNextTag();
    if (tag.charAt(0) != '/' || !tag.substring(1).equals(expectedTag))
    {
      throw unexpectedInputException(expectedTag, tag);
    }
  }

  public String peekTag()
  {
    mark(128);
    String tag = getNextTag();
    reset();
    return tag;
  }

  private String getNextTag()
  {
    int c;
    while ((c = read()) != -1 && Character.isWhitespace(c))
    {

    }

    if (c != '<')
    {
      throw unexpectedInputException("<", Character.toString((char)c));
    }

    StringBuffer tagBuffer = new StringBuffer();

    while ((c = read()) != -1 && c != '>')
    {
      tagBuffer.append((char)c);
    }

    return tagBuffer.toString();
  }

  public Object readTaggedObject(String tag)
  {
    // TODO: Decode object
    return readTaggedString(tag);
  }

  public int readTaggedInt(String tag)
  {
    String value = readTaggedString(tag);
    return Integer.parseInt(value);
  }
  
  public long readTaggedLong(String tag)
  {
    String value = readTaggedString(tag);
    return Long.parseLong(value);
  }

  public boolean readTaggedBoolean(String tag)
  {
    String value = readTaggedString(tag);
    return Boolean.parseBoolean(value);
  }

  public String readTaggedString(String tag)
  {
    beginTag(tag);

    StringBuffer buffer = new StringBuffer();

    int c;

    States state = States.BEGIN;
    StringBuffer encodingBuffer = new StringBuffer();
    
    while ((c = read()) != -1 && c != '<')
    {
      switch (state)
      {
        case BEGIN:
          if (c == '&')
          {
            state = States.AMPERSAND;
          }
          else
          {
            buffer.append((char)c);
          }
          break;
        case AMPERSAND:
          if (c == ';')
          {
            buffer.append(decode(encodingBuffer.toString()));
            encodingBuffer.setLength(0);
            state = States.BEGIN;
          }
          else
          {
            encodingBuffer.append((char)c);
          }
          break;
      }
    }

    unread(c);

    endTag(tag);

    return buffer.toString();
  }

  private char decode(String codedValue)
  {
    char c;
    if (codedValue.equals("lt"))
    {
      c = '<';
    }
    else if (codedValue.equals("gt"))
    {
      c = '>';
    }
    else if (codedValue.equals("amp"))
    {
      c = '&';
    }
    else if (codedValue.equals("quot"))
    {
      c = '"';
    }
    else if (codedValue.equals("apos"))
    {
      c = '\'';
    }
    else if (codedValue.charAt(0) == '#')
    {
      c = (char)Integer.parseInt(codedValue.substring(1));
    }
    else
    {
      throw unexpectedInputException("coded value", codedValue);
    }
    return c;
  }

  public int read()
  {
    inputPosition++;

    if (unreadIndex < unreadBuffer.length())
    {
      return unreadBuffer.charAt(unreadIndex++);
    }

    try
    {
      return super.read();
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  public void reset()
  {
    try
    {
      inputPosition -= pos - markpos;
      super.reset();
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  private void unread(String value)
  {
    unreadBuffer = value;
    unreadIndex = 0;
    inputPosition -= value.length();
  }

  private void unread(int c)
  {
    unread(Character.toString((char)c));
  }

  public RuntimeException unexpectedInputException(String expected, String actual)
  {
    return new RuntimeException("Expected " + expected + ", got " + actual + " at position " + inputPosition);
  }

}
