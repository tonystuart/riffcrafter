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
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

import com.riffcrafter.library.util.Hex;


public class SysexEvent extends Midel
{
  private SysexMessage message;
  private int channel = Midel.DEFAULT_CHANNEL;

  public SysexEvent(long tick, SysexMessage message)
  {
    super(tick, SYSEX_EVENT_SEQUENCE);
    this.message = message;
  }

  public byte[] getData()
  {
    return message.getData();
  }

  // Assigned channels... used for track management.
  
  @Override
  public int getChannel()
  {
    return channel;
  }

  @Override
  public void setChannel(int channel)
  {
    this.channel  = channel;
  }

  public void addTo(Track track)
  {
    MidiEvent event = new MidiEvent(message, tick);
    track.add(event);
  }

  public String getToolTipText()
  {
    byte[] data = message.getData();
    String hexString = Hex.toHexString(data);
    return "System Exclusive: Length=" + data.length + ", Data=" + hexString;
  }

  public String toString()
  {
    byte[] data = message.getData();
    String hexString = Hex.toHexString(data);
    return "[" + super.toString() + ", length=" + data.length + ", data=" + hexString + "]";
  }

}
