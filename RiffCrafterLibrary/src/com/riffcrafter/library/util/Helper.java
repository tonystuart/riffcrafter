// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public class Helper
{
  public static String getCanonicalPath(File file)
  {
    try
    {
      return file.getCanonicalPath();
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static URI getUri(String uriName)
  {
    try
    {
      return new URI(uriName);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  public static void browse(URI uri)
  {
    try
    {
      Desktop.getDesktop().browse(uri);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

}
