// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

import javax.sound.midi.MetaMessage;

public class TimeSignatureChange extends MetaEvent
{
  private int beatsPerMeasure;
  private int beatUnit;

  public TimeSignatureChange(long tick, MetaMessage message)
  {
    super(tick, message);
    byte[] bytes = message.getMessage();
    beatsPerMeasure = bytes[3] & 0xff;
    beatUnit = 1 << (bytes[4] & 0xff);
  }

  public static byte[] fromString(String value)
  {
    String[] values = value.split("/");
    if (values.length != 2)
    {
      throw new RuntimeException("Expected time signature in b/u notation");
    }

    int beatsPerMeasure = Integer.parseInt(values[0]);
    int beatUnit = Integer.parseInt(values[1]);

    int powerOf2 = 0;

    while (beatUnit > 1)
    {
      beatUnit /= 2;
      powerOf2++;
    }

    byte[] data = new byte[2];

    data[0] = (byte)beatsPerMeasure;
    data[1] = (byte)powerOf2;

    return data;
  }

  public static String toString(byte[] data)
  {
    int beatsPerMeasure = data[0] & 0xff;
    int beatUnit = 1 << (data[1] & 0xff);
    String value = beatsPerMeasure + "/" + beatUnit;
    return value;
  }

  public int getBeatsPerMeasure()
  {
    return beatsPerMeasure;
  }

  public String getToolTipText()
  {
    return "Time: " + beatsPerMeasure + "/" + beatUnit;
  }

}
