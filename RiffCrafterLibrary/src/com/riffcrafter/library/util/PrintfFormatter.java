// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.util;

import java.util.Formatter;
import java.util.Locale;

public class PrintfFormatter
{
  private StringBuilder sb;
  private Formatter formatter;

  public PrintfFormatter()
  {
    sb = new StringBuilder();
    formatter = new Formatter(sb, Locale.US);
  }

  public String printf(String template, Object... args)
  {
    sb.setLength(0);
    formatter.format(template, args);
    return sb.toString();
  }
}

