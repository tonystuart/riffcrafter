// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;

// See http://www.midi.org/about-midi/table1.shtml

public class MidiConstants
{
  public static final long FIRST_NOTE = -1; // time base is relative to first note retrieved
  public static final long TRACK_START = 0; // time base is relative to beginning of track
  public static final int AUTO_GENERATE = -1; // automatically generate tick value

  public static final int MAX_CHANNELS = 16;
  public static final int MAX_MIDI_KEYS = 128;
  public static final int MAX_PROGRAMS = 128;

  // See http://en.wikipedia.org/wiki/Musical_interval and Miller p. 114
  public static final int SEMITONES_PER_MINOR_SECOND = 1;
  public static final int SEMITONES_PER_MAJOR_SECOND = 2;
  public static final int SEMITONES_PER_MINOR_THIRD = 3;
  public static final int SEMITONES_PER_MAJOR_THIRD = 4;
  public static final int SEMITONES_PER_PERFECT_FOURTH = 5;
  public static final int SEMITONES_PER_DIMINISHED_FIFTH = 6;
  public static final int SEMITONES_PER_PERFECT_FIFTH = 7;
  public static final int SEMITONES_PER_AUGMENTED_FIFTH = 8;
  public static final int SEMITONES_PER_MAJOR_SIXTH = 9;
  public static final int SEMITONES_PER_MINOR_SEVENTH = 10;
  public static final int SEMITONES_PER_MAJOR_SEVENTH = 11;
  public static final int SEMITONES_PER_OCTAVE = 12;

  public static final int END_OF_NOTE_LIST = 46;
  public static final int END_OF_TRACK = 47;

  public static final int SINGLE_TRACK = 0;
  public static final int MULTIPLE_TRACK = 1;

  public static final int FIRST_CHANNEL = Channel.FIRST_CHANNEL_NUMBER;
  public static final int LAST_CHANNEL = Channel.LAST_CHANNEL_NUMBER;
  public static final int DEFAULT_CHANNEL = FIRST_CHANNEL;

  public static final int FIRST_KEY = 0;
  public static final int LAST_KEY = 127;
  public static final int DEFAULT_KEY = 60; // middle c

  public static final int FIRST_VELOCITY = 0;
  public static final int LAST_VELOCITY = 127;
  public static final int DEFAULT_VELOCITY = 64;

  public static final int DURATION_THIRTYSECOND_NOTE = 32;
  public static final int DURATION_SIXTEENTH_NOTE = 62;
  public static final int DURATION_EIGHTH_NOTE = 125;
  public static final int DURATION_QUARTER_NOTE = 250;
  public static final int DURATION_DOTTED_QUARTER_NOTE = 375;
  public static final int DURATION_HALF_NOTE = 500;
  public static final int DURATION_DOTTED_HALF_NOTE = 750;
  public static final int DURATION_WHOLE_NOTE = 1000;

  public static final long[] REST_DURATIONS = 
  {
    //DURATION_SIXTEENTH_NOTE, //
    DURATION_EIGHTH_NOTE, //
    DURATION_QUARTER_NOTE, //
    DURATION_HALF_NOTE, //
    DURATION_WHOLE_NOTE, //
  };

  public static final int SYSEX_STATUS = 0xf0;

  public static final int SM_COMMAND_NOTE_ON = 0x80;
  public static final int SM_COMMAND_NOTE_OFF = 0x90;
  public static final int SM_COMMAND_POLYPHONIC_AFTERTOUCH = 0xa0;
  public static final int SM_COMMAND_CONTROL_CHANGE = 0xb0;
  public static final int SM_COMMAND_PROGRAM_CHANGE = 0xc0;
  public static final int SM_COMMAND_CHANNEL_AFTERTOUCH = 0xd0;
  public static final int SM_COMMAND_PITCH_WHEEL = 0xe0;

  public static final int CC_UNDEFINED = -1;
  public static final int CC_VOLUME = 7;

  public static final int MM_SEQUENCE_NUMBER = 0;
  public static final int MM_TEXT = 1;
  public static final int MM_COPYRIGHT = 2;
  public static final int MM_TRACK_NUMBER = 3;
  public static final int MM_INSTRUMENT_NAME = 4;
  public static final int MM_LYRIC = 5;
  public static final int MM_MARKER = 6;
  public static final int MM_CUE_POINT = 7;
  public static final int MM_CHANNEL = 0x20;
  public static final int MM_PORT = 0x21;
  public static final int MM_END_OF_TRACK = 0x2F;
  public static final int MM_TEMPO = 0x51;
  public static final int MM_SMTPE_OFFSET = 0x54;
  public static final int MM_TIME_SIGNATURE = 0x58;
  public static final int MM_KEY_SIGNATURE = 0x59;
  public static final int MM_VENDOR_SPECIFIC = 0x7F;

  public static final int SMD_DATA2 = 1;
  public static final int SMD_DATA1_DATA2 = 2;
  public static final int SMD_14BIT = 3;
  public static final int SMD_PC = 4;

  public static final int MME_NONE = 0;
  public static final int MME_HEX = 1;
  public static final int MME_STRING = 2;
  public static final int MME_BPM = 3;
  public static final int MME_TSC = 4;

  // http://www.midi.org/about-midi/table3.shtml
  public static final SMD[] ccds = new SMD[] { // Control Change Descriptors (an array of Short Message Descriptors)
      new SMD(SM_COMMAND_CONTROL_CHANGE, 0, SMD_DATA2, "Bank Select"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 1, SMD_DATA2, "Modulation Wheel"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 2, SMD_DATA2, "Breath Controller"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 3, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 4, SMD_DATA2, "Foot Controller"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 5, SMD_DATA2, "Portamento Time"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 6, SMD_DATA2, "Data Entry"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 7, SMD_DATA2, "Channel Volume"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 8, SMD_DATA2, "Balance"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 9, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 10, SMD_DATA2, "Pan"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 11, SMD_DATA2, "Expression Controller"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 12, SMD_DATA2, "Effect Control 1"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 13, SMD_DATA2, "Effect Control 2"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 14, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 15, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 16, SMD_DATA2, "General Purpose 1"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 17, SMD_DATA2, "General Purpose 2"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 18, SMD_DATA2, "General Purpose 3"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 19, SMD_DATA2, "General Purpose 4"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 20, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 21, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 22, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 23, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 24, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 25, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 26, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 27, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 28, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 29, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 30, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 31, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 32, SMD_DATA2, "Bank Select LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 33, SMD_DATA2, "Modulation Wheel LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 34, SMD_DATA2, "Breath Controller LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 35, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 36, SMD_DATA2, "Foot Controller LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 37, SMD_DATA2, "Portamento Time LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 38, SMD_DATA2, "Data Entry LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 39, SMD_DATA2, "Channel Volume LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 40, SMD_DATA2, "Balance LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 41, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 42, SMD_DATA2, "Pan LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 43, SMD_DATA2, "Expression Controller LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 44, SMD_DATA2, "Effect Control 1 LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 45, SMD_DATA2, "Effect Control 2 LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 46, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 47, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 48, SMD_DATA2, "General Purpose 1 LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 49, SMD_DATA2, "General Purpose 2 LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 50, SMD_DATA2, "General Purpose 3 LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 51, SMD_DATA2, "General Purpose 4 LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 52, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 53, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 54, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 55, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 56, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 57, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 58, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 59, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 60, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 61, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 62, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 63, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 64, SMD_DATA2, "Sustain"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 65, SMD_DATA2, "Portamento"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 66, SMD_DATA2, "Sustenuto"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 67, SMD_DATA2, "Soft Pedal"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 68, SMD_DATA2, "Legato Footswitch"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 69, SMD_DATA2, "Hold 2"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 70, SMD_DATA2, "Sound Variation"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 71, SMD_DATA2, "Timbre / Harmonic Intensity"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 72, SMD_DATA2, "Release Time"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 73, SMD_DATA2, "Attack Time"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 74, SMD_DATA2, "Brightness"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 75, SMD_DATA2, "Decay Time"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 76, SMD_DATA2, "Vibrato Rate"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 77, SMD_DATA2, "Vibrato Depth"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 78, SMD_DATA2, "Vibrato Delay"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 79, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 80, SMD_DATA2, "General Purpose Controller 5"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 81, SMD_DATA2, "General Purpose Controller 6"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 82, SMD_DATA2, "General Purpose Controller 7"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 83, SMD_DATA2, "General Purpose Controller 8"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 84, SMD_DATA2, "Portamento Control"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 85, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 86, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 87, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 88, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 89, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 90, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 91, SMD_DATA2, "Effects Depth 1 / Reverb"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 92, SMD_DATA2, "Effects Depth 2 / Tremulo"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 93, SMD_DATA2, "Effects Depth 3 / Chorus"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 94, SMD_DATA2, "Effects Depth 4 / Celeste"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 95, SMD_DATA2, "Effects Depth 5 / Phaser"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 96, SMD_DATA2, "Data Increment"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 97, SMD_DATA2, "Data Decrement"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 98, SMD_DATA2, "Unregistered Parameter LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 99, SMD_DATA2, "Unregistered Parameter"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 100, SMD_DATA2, "Registered Parameter LSB"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 101, SMD_DATA2, "Registered Parameter"), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 102, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 103, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 104, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 105, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 106, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 107, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 108, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 109, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 110, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 111, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 112, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 113, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 114, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 115, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 116, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 117, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 118, SMD_DATA2, null), //
      new SMD(SM_COMMAND_CONTROL_CHANGE, 119, SMD_DATA2, null), //
  };

  public static final SMD[] smds = new SMD[] { // Short Message Descriptors
      new SMD(SM_COMMAND_POLYPHONIC_AFTERTOUCH, CC_UNDEFINED, SMD_DATA1_DATA2, "Polyphonic Aftertouch"), //
      new SMD(SM_COMMAND_PROGRAM_CHANGE, CC_UNDEFINED, SMD_PC, "Program Change"), //
      new SMD(SM_COMMAND_CHANNEL_AFTERTOUCH, CC_UNDEFINED, SMD_DATA1_DATA2, "Channel Aftertouch"), //
      new SMD(SM_COMMAND_PITCH_WHEEL, CC_UNDEFINED, SMD_14BIT, "Pitch Wheel"), //
  };

  public static final MMD[] mmds = new MMD[] { // Meta Message Descriptors
      new MMD(MM_SEQUENCE_NUMBER, MME_HEX, "Sequence Number"), //
      new MMD(MM_TEXT, MME_STRING, "Text"), //
      new MMD(MM_COPYRIGHT, MME_STRING, "Copyright"), //
      new MMD(MM_TRACK_NUMBER, MME_STRING, "Track Name"), //
      new MMD(MM_INSTRUMENT_NAME, MME_STRING, "Instrument Name"), //
      new MMD(MM_LYRIC, MME_STRING, "Lyrics"), //
      new MMD(MM_MARKER, MME_STRING, "Marker"), //
      new MMD(MM_CUE_POINT, MME_STRING, "Cue Point"), //
      new MMD(MM_CHANNEL, MME_HEX, "Channel"), //
      new MMD(MM_PORT, MME_HEX, "Port"), //
      new MMD(MM_END_OF_TRACK, MME_NONE, "End of Track"), //
      new MMD(MM_TEMPO, MME_BPM, "Tempo"), //
      new MMD(MM_SMTPE_OFFSET, MME_HEX, "SMTPE Offset"), //
      new MMD(MM_TIME_SIGNATURE, MME_TSC, "Time Signature"), //
      new MMD(MM_KEY_SIGNATURE, MME_HEX, "Key Signature"), //
      new MMD(MM_VENDOR_SPECIFIC, MME_HEX, "Vendor Specific"), //
  };

  public static class SMD // Short Message Descriptor
  {
    private int command;
    private int subCommand;
    private int encoding;
    private String synopsis;
    private static SMD[] smdBySynopsis = null;

    public SMD(int command, int subCommand, int encoding, String synopsis)
    {
      this.command = command;
      this.subCommand = subCommand;
      this.encoding = encoding;
      this.synopsis = synopsis != null ? synopsis : "Undefined Control " + format(subCommand);
    }

    private String format(int subCommand)
    {
      String text;
      if (subCommand < 10)
      {
        text = "00" + subCommand;
      }
      else if (subCommand < 100)
      {
        text = "0" + subCommand;
      }
      else
      {
        text = Integer.toString(subCommand);
      }
      return text;
    }

    public static SMD find(int command, int subType)
    {
      if (command == SM_COMMAND_CONTROL_CHANGE)
      {
        if (subType >= 0 && subType < ccds.length && ccds[subType] != null)
        {
          return ccds[subType];
        }
        else
        {
          return new SMD(command, 0, SMD_DATA1_DATA2, "Control Change " + command);
        }
      }

      for (SMD mmd : smds)
      {
        if (mmd.command == command)
        {
          return mmd;
        }
      }

      return new SMD(command, 0, SMD_DATA1_DATA2, "Command " + command);
    }

    public synchronized static SMD[] getSmdBySynopsis()
    {
      if (smdBySynopsis == null)
      {
        int normalizedLength = ccds.length;
        for (int i = 0; i < ccds.length; i++)
        {
          if (ccds[i].synopsis == null)
          {
            normalizedLength--;
          }
        }
        smdBySynopsis = new SMD[normalizedLength + smds.length];
        for (int i = 0, j = 0; i < ccds.length; i++)
        {
          if (ccds[i].synopsis != null)
          {
            smdBySynopsis[j++] = ccds[i];
          }
        }
        System.arraycopy(smds, 0, smdBySynopsis, normalizedLength, smds.length);
        Arrays.sort(smdBySynopsis, new Comparator<SMD>()
        {
          public int compare(SMD o1, SMD o2)
          {
            return o1.synopsis.compareTo(o2.synopsis);
          }
        });
      }
      return smdBySynopsis;
    }

    public int getEncoding()
    {
      return encoding;
    }

    public int getSubCommand()
    {
      return subCommand;
    }

    public String getSynopsis()
    {
      return synopsis;
    }

    public int getCommand()
    {
      return command;
    }

    public String toString()
    {
      return getSynopsis();
    }

  }

  public static class MMD // Meta Message Descriptor
  {
    private int type;
    private int encoding;
    private String synopsis;
    private static MMD[] mmdBySynopsis = null;

    public MMD(int type, int encoding, String description)
    {
      this.type = type;
      this.encoding = encoding;
      this.synopsis = description;
    }

    public static MMD find(int type)
    {
      for (MMD mmd : mmds)
      {
        if (mmd.type == type)
        {
          return mmd;
        }
      }
      return new MMD(type, MME_HEX, "Unknown, command=" + type);
    }

    public synchronized static MMD[] getMmdBySynopsis()
    {
      if (mmdBySynopsis == null)
      {
        mmdBySynopsis = mmds.clone();
        Arrays.sort(mmdBySynopsis, new Comparator<MMD>()
        {

          public int compare(MMD o1, MMD o2)
          {
            return o1.synopsis.compareTo(o2.synopsis);
          }
        });
      }
      return mmdBySynopsis;
    }

    public String getSynopsis()
    {
      return synopsis;
    }

    public int getEncoding()
    {
      return encoding;
    }

    public int getType()
    {
      return type;
    }

    public String toString()
    {
      return getSynopsis();
    }

  }

}
