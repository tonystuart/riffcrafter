// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.test;

import java.io.FileOutputStream;
import java.util.Random;

public class File
{

  public static void main(String[] args) throws Exception
  {
    FileOutputStream s = new FileOutputStream(args[0]);
    byte[] buffer = new byte[1000000];
    new Random().nextBytes(buffer);
    for (;;)
    {
      s.write(buffer);
    }
  }

}
