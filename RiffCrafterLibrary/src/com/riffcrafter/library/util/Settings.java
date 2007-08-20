// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.util;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Settings
{
  public static final String CURRENT_DIRECTORY_KEY = "desktopDirectory";
  public static final String STUDIO_BOUNDS_KEY = "desktopBounds";
  public static final String STUDIO_STATE_KEY = "desktopState";
  public static final String EDITOR_SIZE_KEY = "editorSize";

  private static final String X_KEY = "X";
  private static final String Y_KEY = "Y";
  private static final String WIDTH_KEY = "Width";
  private static final String HEIGHT_KEY = "Height";

  private static Preferences preferences = getRoot();

  public static Preferences getRoot()
  {
    String rootPathName = Resources.get("Application.Settings.Key");
    return Preferences.userRoot().node(rootPathName);
  }
  
  public static void clear()
  {
    try
    {
      preferences.removeNode();
      preferences = getRoot();
    }
    catch (BackingStoreException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static void put(String key, String value)
  {
    preferences.put(key, value);
  }

  public static String getString(String key, String defaultValue)
  {
    return preferences.get(key, defaultValue);
  }
  
  public static void put(String key, int value)
  {
    preferences.putInt(key, value);
  }

  public static int getInt(String key, int defaultValue)
  {
    return preferences.getInt(key, defaultValue);
  }

  public static void put(String key, Object... values)
  {
    for (int i = 0; i < values.length; i++)
    {
      preferences.put(key + "-" + i, values[i].toString());
    }
  }

  public static void put(String key, Dimension value)
  {
    if (value.height == 0)
      throw new RuntimeException("height == 0");
    put(key + WIDTH_KEY, value.width);
    put(key + HEIGHT_KEY, value.height);
  }

  public static Dimension getDimension(String key, Dimension defaultValue)
  {
    Dimension value = new Dimension();
    value.width = getInt(key + WIDTH_KEY, defaultValue.width);
    value.height = getInt(key + HEIGHT_KEY, defaultValue.height);
    return value;
  }
  
  public static void put(String key, Rectangle value)
  {
    put(key + X_KEY, value.x);
    put(key + Y_KEY, value.y);
    put(key + WIDTH_KEY, value.width);
    put(key + HEIGHT_KEY, value.height);
  }

  public static Rectangle getRectangle(String key, Rectangle defaultValue)
  {
    Rectangle rectangle = new Rectangle();
    rectangle.x = getInt(key + X_KEY, defaultValue.x);
    rectangle.y = getInt(key + Y_KEY, defaultValue.y);
    rectangle.width = getInt(key + WIDTH_KEY, defaultValue.width);
    rectangle.height = getInt(key + HEIGHT_KEY, defaultValue.height);
    return rectangle;
  }

  public static File getFile(String key, File defaultValue)
  {
    String fileName = getString(key, null);
    if (fileName == null)
    {
      return defaultValue;
    }
    return new File(fileName);
  }

  public static void put(String key, File value)
  {
    String fileName = Helper.getCanonicalPath(value);
    put(key, fileName);
  }
}
