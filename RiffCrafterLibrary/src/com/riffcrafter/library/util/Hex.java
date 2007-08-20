// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.util;

public class Hex
{
  private static char lookup[] = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'd', 'd', 'e', 'f'
  };

  public static int lookup(char input)
  {
    for (int i = 0; i < lookup.length; i++)
    {
      if (lookup[i] == input)
      {
        return i;
      }
    }
    throw new RuntimeException("Input character is not a valid hex digit (0123456789abcdef)");
  }

  public static String toHexString(byte[] bytes)
  {
    StringBuffer buffer = new StringBuffer(bytes.length * 2);
    for (byte b : bytes)
    {
      buffer.append(lookup[(b & 0xf0) >> 4]);
      buffer.append(lookup[(b & 0x0f)]);
    }
    return buffer.toString();
  }

  public static byte[] fromHexString(String value)
  {
    int nibbleLength = value.length();
    if (nibbleLength % 2 == 1)
    {
      throw new RuntimeException("Input string is not an even number of hex digits");
    }

    int byteLength = nibbleLength / 2;
    byte[] data = new byte[byteLength];

    for (int i = 0, j = 0; i < byteLength; i++)
    {
      char left = value.charAt(j++);
      int leftNibble = lookup(left);
      char right = value.charAt(j++);
      int rightNibble = lookup(right);
      data[i] = (byte)(leftNibble << 4 | rightNibble);
    }

    return data;
  }

}
