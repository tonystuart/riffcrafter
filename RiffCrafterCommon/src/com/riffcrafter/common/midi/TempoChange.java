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

public class TempoChange extends MetaEvent
{
  public static final int USEC_PER_MINUTE = 60000000;
  
  private int usecPerQuarterNote;
  private int quarterNotesPerMinute;

  public TempoChange(long tick, MetaMessage message)
  {
    super(tick, message);
    byte[] bytes = message.getMessage();
    usecPerQuarterNote = ((bytes[3] & 0xff) << 16) | ((bytes[4] & 0xff) << 8) | (bytes[5] & 0xff);
    quarterNotesPerMinute = USEC_PER_MINUTE / usecPerQuarterNote;
  }

  public static String toString(byte[] data)
  {
    int usecPerQuarterNote = ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
    int quarterNotesPerMinute = USEC_PER_MINUTE / usecPerQuarterNote;
    return Integer.toString(quarterNotesPerMinute);
  }
  
  public static byte[] fromString(String value)
  {
    byte[] data = new byte[3];
    int quarterNotesPerMinute = Integer.parseInt(value);
    int usecPerQuarterNote = USEC_PER_MINUTE / quarterNotesPerMinute;
    data[0] = (byte)(usecPerQuarterNote >> 16 & 0xff);
    data[1] = (byte)(usecPerQuarterNote >>  8 & 0xff);
    data[2] = (byte)(usecPerQuarterNote       & 0xff);
    return data;
  }
  
  public String getToolTipText()
  {
    return "Tempo: " + quarterNotesPerMinute + " bpm";
  }

  public int getTempoInBPM()
  {
    return quarterNotesPerMinute;
  }

  public boolean equalsTempo(TempoChange tempoChange)
  {
    return tempoChange != null && tempoChange.quarterNotesPerMinute == this.quarterNotesPerMinute;
  }

}
