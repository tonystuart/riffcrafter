// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

// Piano Keyboard       Midi Keyboard     Frequency
//
//                       0 / C0             8.1 Hz           First key on midi keyboard
// A0                   21 / A1            27.5 Hz           First key on piano keyboard / 22nd key on midi keyboard
// A#0                  22 / A#1           29.1 Hz           Second key on piano keyboard / 23rd key on midi keyboard
// B0                   23 / B1            30.8 Hz           Third key on piano keyboard / 24th key on midi keyboard
// C1                   24 / C2            32.7 Hz           Fourth key on piano keyboard / 25th key on midi keyboard
// C4                   60 / C5           261.6 Hz           Middle C
// A4                   69 / A5           440.0 Hz           A above middle C
//
// The piano keyboard starts at A, and octave numbering starts at C, so the first four white keys on the piano are A0, B0, C1, D1

public class NoteName
{
  private static final String[] sharpNotes = new String[] { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
  private static final String[] flatNotes = new String[] { "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B" };

  public static String getNoteName(int key)
  {
    return getNoteName(key, true);
  }

  public static String getNoteName(int channel, int key)
  {
    String noteName;
    if (channel == Instruments.DRUM_CHANNEL)
    {
      noteName = Instruments.getDrumName(key);
    }
    else
    {
      noteName = getNoteName(key, true);
    }
    return noteName;
  }

  public static String getNoteName(int key, boolean isPiano)
  {
    int index = key % MidiConstants.SEMITONES_PER_OCTAVE;
    String note = sharpNotes[index];
    int octave = key / MidiConstants.SEMITONES_PER_OCTAVE;
    if (isPiano)
    {
      octave--;
    }
    String name = note + octave;
    return name;
  }

  public static String getNoteNameWithoutOctave(int key)
  {
    return sharpNotes[key % MidiConstants.SEMITONES_PER_OCTAVE];
  }

  public static String getSharp(int semitone)
  {
    return sharpNotes[semitone];
  }

  public static String getFlat(int semitone)
  {
    return flatNotes[semitone];
  }

  public static String getKeyName(int tonic, boolean isMajor, int halfNotes)
  {
    String note = (halfNotes < 0 ? getFlat(tonic) : getSharp(tonic));
    String mode = isMajor ? " Major" : " minor";
    String key = note + mode;
    return key;
  }

  public static String getSynopsis(int halfNotes)
  {
    String nickName;
    if (halfNotes < 0)
    {
      nickName = getPlural(-halfNotes, "flat");
    }
    else if (halfNotes > 0)
    {
      nickName = getPlural(halfNotes, "sharp");
    }
    else
    {
      nickName = "";
    }
    return nickName;
  }

  // TODO: Find an efficient way to NLS enable this

  private static String getPlural(int number, String text)
  {
    String plural;

    if (number == 0)
    {
      plural = "no " + text + "s";
    }
    else if (number > 1)
    {
      plural = number + " " + text + "s";
    }
    else
    {
      plural = "1 " + text;
    }
    return plural;
  }

}
