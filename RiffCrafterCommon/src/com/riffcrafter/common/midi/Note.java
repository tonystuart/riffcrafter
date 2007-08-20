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

public class Note extends Midel
{
  private int channel;
  private int key;
  private int velocity;
  private long duration;
  private String noteName;

  public Note(Note note)
  {
    this(note.channel, note.key, note.velocity, note.tick, note.duration);
  }

  public Note(int channel, int key, int velocity, long tick, long duration)
  {
    super(tick, NOTE_SEQUENCE);
    this.channel = channel;
    this.key = key;
    this.velocity = velocity;
    this.duration = duration;
  }

  public Note copy()
  {
    return new Note(channel, key, velocity, tick, duration);
  }

  public boolean equals(Object object)
  {
    if (!(object instanceof Note))
    {
      return false;
    }

    return compareTo((Note)object) == 0;
  }

  protected int compareDiscriminator(Midel thatMidel)
  {
    Note that = (Note)thatMidel;
    int deltaChannel = this.channel - that.channel;
    if (deltaChannel != 0)
    {
      return deltaChannel;
    }

    int deltaKey = this.key - that.key;
    if (deltaKey != 0)
    {
      return deltaKey;
    }

    int deltaVelocity = this.velocity - that.velocity;
    if (deltaVelocity != 0)
    {
      return deltaVelocity;
    }

    long deltaDuration = this.duration - that.duration;
    if (deltaDuration != 0)
    {
      return convertLongResultToInteger(deltaDuration);
    }

    return 0;
  }

  public Note clone()
  {
    Note note = (Note)super.clone();
    note.noteName = null;
    return note;
  }

  public MidiEvent getNoteOnEvent()
  {
    ShortMessage message = new ShortMessage();
    Catcher.setMessage(message, ShortMessage.NOTE_ON, channel, key, velocity);
    MidiEvent event = new MidiEvent(message, tick);
    return event;
  }

  public MidiEvent getNoteOffEvent()
  {
    ShortMessage message = new ShortMessage();
    Catcher.setMessage(message, ShortMessage.NOTE_OFF, channel, key, 0);
    MidiEvent event = new MidiEvent(message, tick + duration);
    return event;
  }

  public void addTo(Track channelTrack)
  {
    channelTrack.add(getNoteOnEvent());
    channelTrack.add(getNoteOffEvent());
  }

  public int getKey()
  {
    return key;
  }

  public long getDuration()
  {
    return duration;
  }

  public int getVelocity()
  {
    return velocity;
  }

  public long getEndingTick()
  {
    return tick + duration;
  }

  public void setKey(int key)
  {
    if (isReadOnly)
    {
      throw new WriteException();
    }
    this.key = key;
    this.noteName = null;
  }

  public void setDuration(long duration)
  {
    if (isReadOnly)
    {
      throw new WriteException();
    }
    this.duration = duration;
  }

  public void setVelocity(int velocity)
  {
    if (isReadOnly)
    {
      throw new WriteException();
    }
    this.velocity = velocity;
  }

  public boolean closeTo(long thatTick)
  {
    return (tick > thatTick ? tick - thatTick : thatTick - tick) < 30;
  }

  public int getChannel()
  {
    return channel;
  }

  public void setChannel(int channel)
  {
    if (isReadOnly)
    {
      throw new WriteException();
    }
    this.channel = channel;
  }

  private String getNoteName()
  {
    if (noteName == null)
    {
      noteName = NoteName.getNoteName(channel, key);
    }
    return noteName;
  }

  public String getDescription()
  {
    getNoteName();
    return "Midi Key: " + key + ", Note: " + noteName + ", Velocity: " + velocity + ", Duration: " + duration;
  }

  public String getToolTipText()
  {
    getNoteName();
    return "Note: " + noteName + " (" + key + "), Start: " + tick + ", Duration: " + duration + ", Velocity: " + velocity;
  }

  public String toString()
  {
    return "[" + super.toString() + ", channel=" + channel + ", key=" + key + ", velocity=" + velocity + ", duration=" + duration + "]";
  }

  public boolean isOverlappingTicks(Note that)
  {
    return ((that.tick >= this.tick) && (that.tick < this.tick + this.duration)) || ((this.tick >= that.tick) && (this.tick < that.tick + that.duration));
  }
  
  public boolean isAdjacentKeys(Note that)
  {
    return (this.key - that.key == 1) || (that.key - this.key == 1);
  }
}
