// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

import javax.sound.midi.Track;

public class Midel implements Comparable<Midel>, Cloneable
{
  // NB: All non-Notes sort earlier than Notes so that more complex Note comparisons work.

  public static final int LOWEST_IN_SEQUENCE = 0;
  public static final int SYSEX_EVENT_SEQUENCE = 1;
  public static final int META_EVENT_SEQUENCE = 2;
  public static final int CHANNEL_EVENT_SEQUENCE = 3;
  public static final int NOTE_SEQUENCE = 4;
  public static final int HIGHEST_IN_SEQUENCE = 5;

  public static final int DEFAULT_CHANNEL = -1;

  protected long tick;
  protected int sequence;
  protected boolean isReadOnly;

  private int serialNumber;
  private static int maxSerialNumber;

  public Midel(long tick, int sequence)
  {
    this.tick = tick;
    this.sequence = sequence;
    this.serialNumber = ++maxSerialNumber;
  }

  public int getSerialNumber()
  {
    return serialNumber;
  }

  public void setSerialNumber(int serialNumber)
  {
    this.serialNumber = serialNumber;
  }

  public int compareTo(Midel that)
  {
    long deltaTick = this.tick - that.tick;
    if (deltaTick != 0)
    {
      return convertLongResultToInteger(deltaTick);
    }

    int deltaSequence = this.sequence - that.sequence; // like comparing classes, only better!
    if (deltaSequence != 0)
    {
      return deltaSequence;
    }

    return compareDiscriminator(that);
  }

  protected int compareDiscriminator(Midel that)
  {
    int deltaSerialNumber = this.serialNumber - that.serialNumber;
    if (deltaSerialNumber != 0)
    {
      return deltaSerialNumber;
    }
    
    // 20070602: A selection may contain ChannelEvents (e.g. ProgramChanges)
    // that describe Midels in the underlying Midi, but right now these do not
    // compare as equal because the ChannelEvent compareDiscriminator invokes
    // this method and this method resolves to hashCode. I suspect based on
    // our recent implementation of serial numbers, that if the serial numbers
    // are the same, the objects should be considered the same.
    
    // return this.hashCode() - that.hashCode();
    
    return 0;
  }

  public static int convertLongResultToInteger(long delta)
  {
    return delta < 0 ? -1 : delta > 0 ? 1 : 0;
  }

  public void setReadOnly()
  {
    this.isReadOnly = true;
  }

  public long getTick()
  {
    return tick;
  }

  public void setTick(long tick)
  {
    if (isReadOnly)
    {
      throw new WriteException();
    }
    this.tick = tick;
  }

  public int getChannel()
  {
    return DEFAULT_CHANNEL;
  }

  public void setChannel(int channel)
  {
  }

  public class WriteException extends RuntimeException
  {
    public WriteException()
    {
      super("tick=" + tick);
    }
  }

  public Midel clone()
  {
    try
    {
      Midel midel = (Midel)super.clone();
      midel.isReadOnly = false;
      return midel;
    }
    catch (CloneNotSupportedException e)
    {
      throw new RuntimeException(e);
    }
  }

  public void addTo(Track channelTrack)
  {
    throw new RuntimeException("Midel.addTo is unimplemented.");
  }

  public String getDescription()
  {
    return getToolTipText();
  }

  public String getToolTipText()
  {
    return toString();
  }

  public String toString()
  {
    return "tick=" + tick + ", isReadOnly=" + isReadOnly;
  }

}
