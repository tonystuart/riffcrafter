// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

public class Converter
{
  private static final int A4_MIDI_KEY = 69;
  private static final int A4_FREQUENCY = 440;

  public static int convertFrequencyToKey(double f)
  {
    int key = (int)Math.round(12 * log2(f / A4_FREQUENCY)) + A4_MIDI_KEY;
    return key;
  }

  public static double convertKeyToFrequency(int key)
  {
    double f = A4_FREQUENCY * Math.pow(2, ((double)key - A4_MIDI_KEY) / 12);
    return f;
  }

  public static int roundDuration(long duration)
  {
    int d = (int)log2(duration);
    return d;
  }
  
  public static double log2(double d)
  {
    return Math.log(d) / Math.log(2.0);
  }

}
