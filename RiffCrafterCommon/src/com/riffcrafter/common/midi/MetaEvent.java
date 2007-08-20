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
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Track;

import com.riffcrafter.common.midi.Midel.WriteException;
import com.riffcrafter.common.midi.MidiConstants.MMD;
import com.riffcrafter.library.util.Hex;


public class MetaEvent extends Midel
{
  protected MetaMessage message;
  private int channel = Midel.DEFAULT_CHANNEL;

  public MetaEvent(long tick, MetaMessage metaMessage)
  {
    super(tick, META_EVENT_SEQUENCE);
    this.message = metaMessage;
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
    if (isReadOnly)
    {
      throw new WriteException();
    }
    this.channel = channel;
  }

  @Override
  protected int compareDiscriminator(Midel that)
  {
    int deltaChannel = this.channel - ((MetaEvent)that).channel;
    if (deltaChannel != 0)
    {
      return deltaChannel;
    }
    
    return super.compareDiscriminator(that);
  }

  public int getType()
  {
    return message.getType();
  }

  public byte[] getData()
  {
    return message.getData();
  }

  public void addTo(Track track)
  {
    MidiEvent event = new MidiEvent(message, tick);
    track.add(event);
  }

  public String getToolTipText()
  {
    int type = message.getType();
    byte[] data = message.getData();

    MMD mmd = MMD.find(type);

    String text = null;
    String synopsis = mmd.getSynopsis();
    int encoding = mmd.getEncoding();

    switch (encoding)
    {
      case MidiConstants.MME_STRING:
        text = synopsis + ": " + new String(data);
        break;
      case MidiConstants.MME_HEX:
        text = synopsis + ": " + Hex.toHexString(data);
        break;
      case MidiConstants.MME_NONE:
        text = synopsis;
        break;
      case MidiConstants.MME_BPM: // typically handled by TempoChange
        text = synopsis + ": " + TempoChange.toString(data) + " bpm";
        break;
      case MidiConstants.MME_TSC: // typically handled by TimeSignatureChange
        text = synopsis + ": " + TimeSignatureChange.toString(data);
    }
    return text;
  }

  public String toString()
  {
    return "[" + super.toString() + ", " + getToolTipText() + "]";
  }

  public String getText()
  {
    return new String(message.getData());
  }

}
