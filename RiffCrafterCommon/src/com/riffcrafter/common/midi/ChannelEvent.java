// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import com.riffcrafter.common.midi.MidiConstants.SMD;


public class ChannelEvent extends Midel
{
  protected ShortMessage message;

  public ChannelEvent(long tick, ShortMessage message)
  {
    super(tick, CHANNEL_EVENT_SEQUENCE);
    this.message = message;
  }

  @Override
  protected int compareDiscriminator(Midel that)
  {
    int deltaChannel = this.getChannel() - ((ChannelEvent)that).getChannel();
    if (deltaChannel != 0)
    {
      return deltaChannel;
    }
    
    return super.compareDiscriminator(that);
  }

  public void addTo(Track channelTrack)
  {
    MidiEvent event = new MidiEvent(message, tick);
    channelTrack.add(event);
  }

  public int getChannel()
  {
    return message.getChannel();
  }

  public void setChannel(int channel)
  {
    if (isReadOnly)
    {
      throw new WriteException();
    }
    Catcher.setMessage(message, message.getCommand(), channel, message.getData1(), message.getData2());
  }

  public int getCommand()
  {
    int command = message.getCommand();
    return command;
  }
  
  public int getData1()
  {
    int data1 = message.getData1();
    return data1;
  }
  
  public int getData2()
  {
    int data2 = message.getData2();
    return data2;
  }
  
  public String getToolTipText()
  {
    int command = message.getCommand();
    int data1 = message.getData1();
    int data2 = message.getData2();

    SMD smd = SMD.find(command, data1);

    String text = null;
    String synopsis = smd.getSynopsis();
    int encoding = smd.getEncoding();

    switch (encoding)
    {
      case MidiConstants.SMD_DATA2:
        text = synopsis + ": " + data2;
        break;
      case MidiConstants.SMD_DATA1_DATA2:
        text = synopsis + ": address=" + data1 + ", value=" + data2;
        break;
      case MidiConstants.SMD_14BIT:
        text = synopsis + ": " + get14BitValue();
        break;
      case MidiConstants.SMD_PC: // typically handled by ProgramChange
        text = synopsis + ": " + Instruments.getProgramName(data1);
        break;
    }
    
    return text;
  }

  public int get14BitValue()
  {
    return ((message.getData2() & 0x7f) << 7) | (message.getData1() & 0x7f);
  }
  
  public static int get14BitValue(int data1, int data2)
  {
    return ((data2 & 0x7f) << 7) | (data1 & 0x7f);
  }

  public static void from14BitValue(String text, int[] data)
  {
    int value = Integer.parseInt(text);
    data[0] = value & 0x7f;
    data[1] = (value >> 7) & 0x7f;
  }

  public String toString()
  {
    return "[" + super.toString() + ", channel=" + message.getChannel() + ", command=" + message.getCommand() + ", data1=" + message.getData1() + ", data2=" + message.getData2() + "]";
  }

}
