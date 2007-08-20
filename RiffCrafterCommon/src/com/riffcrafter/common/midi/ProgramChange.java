// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

import java.util.Comparator;

import javax.sound.midi.ShortMessage;

public class ProgramChange extends ChannelEvent
{
  public ProgramChange(long tick, ShortMessage message)
  {
    super(tick, message);
  }

  /**
   * This constructor creates a ProgramChange that can be used with
   * floor to search for a controlling ProgramChange. ProgramChanges
   * created by this constructor do not include enough information to
   * produce a valid Program Change MIDI event and should not be
   * stored in a Midi or other Midel list.
   */

  public ProgramChange(long tick, int channel)
  {
    super(tick, new ProgramChangeShortMessage(new byte[] { (byte)(ShortMessage.PROGRAM_CHANGE | channel), 0, 0 } ));
    sequence = HIGHEST_IN_SEQUENCE;
  }

  public static ProgramChange create(long tick, int channel, int program)
  {
    ShortMessage message = new ShortMessage();
    Catcher.setMessage(message, MidiConstants.SM_COMMAND_PROGRAM_CHANGE, channel, program, 0);
    return new ProgramChange(tick, message);
  }

  public int getProgram()
  {
    return message.getData1();
  }

  /**
   * This Comparator is used in at least two distinct ways:
   * 
   * 1) Finding the controlling program change using floor
   * 2) Finding an exact instance of a program change for delete
   * 
   * When finding the controlling program change, the sequence
   * number ensures that the real ProgramChange, containing the
   * program info, sorts out first and can be picked up by the
   * floor function.
   */
  
  public static class ProgramChangeComparator implements Comparator<ProgramChange>
  {
    public int compare(ProgramChange left, ProgramChange right)
    {
      int deltaChannel = left.getChannel() - right.getChannel();
      if (deltaChannel != 0)
      {
        return deltaChannel;
      }

      long deltaTick = left.getTick() - right.getTick();
      if (deltaTick != 0)
      {
        return ProgramChange.convertLongResultToInteger(deltaTick);
      }

      int deltaSequence = left.sequence - right.sequence;
      if (deltaSequence != 0)
      {
        return deltaSequence;
      }

      return 0;
    }
  }
  
  /**
   * This class is to be used with the special version of the constructor
   * to create a ShortMessage to be used with floor when searching for the
   * controlling ProgramChange. This is necessary because our base class
   * uses the MIDI message as the container for the channel.
   */
  public static class ProgramChangeShortMessage extends ShortMessage
  {
    public ProgramChangeShortMessage(byte[] data)
    {
      super(data);
    }
  }

}

