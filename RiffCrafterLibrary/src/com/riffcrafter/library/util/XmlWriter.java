// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;

public class XmlWriter extends PrintStream
{
  public XmlWriter(File file) throws FileNotFoundException
  {
    super(file);
  }

  public XmlWriter(OutputStream outputStream)
  {
    super(outputStream);
  }

  public static XmlWriter open(File file)
  {
    try
    {
      return new XmlWriter(file);
    }
    catch (FileNotFoundException e)
    {
      throw new RuntimeException(e);
    }
  }

  public void beginTag(String tag, int level)
  {
    println("<" + tag + ">", level);
  }

  public void endTag(String tag, int level)
  {
    println("</" + tag + ">", level);
  }

  private void println(String tag, int level)
  {
    StringBuffer buffer = new StringBuffer(level + tag.length());
    int indent = level * 2;
    while (indent-- > 0)
    {
      buffer.append(" ");
    }
    buffer.append(tag);
    println(buffer);
  }

  public void writeTaggedObject(String tag, Object value, int level)
  {
    // TODO: Encode object
    writeTaggedString(tag, value.toString(), level);
  }

  public void writeTaggedInt(String tag, int value, int level)
  {
    writeTaggedString(tag, Integer.toString(value), level);
  }

  public void writeTaggedLong(String tag, long value, int level)
  {
    writeTaggedString(tag, Long.toString(value), level);
  }

  public void writeTaggedBoolean(String tag, boolean value, int level)
  {
    writeTaggedString(tag, Boolean.toString(value), level);
  }

  public void writeTaggedString(String tag, Object value, int level)
  {
    if (value == null)
    {
      value = "";
    }
    println("<" + tag + ">" + encode(value.toString()) + "</" + tag + ">", level);
  }

  public void writeTaggedStringRaw(String tag, Object value, int level)
  {
    println("<" + tag + ">" + value.toString() + "</" + tag + ">", level);
  }

  public static String encode(String source)
  {
    int length = source.length();
    StringBuffer target = new StringBuffer(length * 2);

    for (int i = 0; i < length; i++)
    {
      char sourceChar = source.charAt(i);
      if (sourceChar == '<')
        target.append("&lt;");
      else if (sourceChar == '>')
        target.append("&gt;");
      else if (sourceChar == '&')
        target.append("&amp;");
      else if (sourceChar == '\"')
        target.append("&quot;");
      else if (sourceChar == '\'')
        target.append("&#39;"); // NB: JEditorPane does not understand &apos;
      else if (sourceChar == '\t')
        target.append("&#9;");
      else if (sourceChar == '\n')
        target.append("&#10;");
      else if (sourceChar == '\r')
        target.append("&#13;");
      else if (sourceChar < 0x7f)
        target.append(sourceChar);
      else
        target.append("&#" + Integer.toString((int)sourceChar) + ";");
    }

    return target.toString();
  }

}
