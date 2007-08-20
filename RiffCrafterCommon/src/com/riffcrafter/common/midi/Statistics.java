// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;


public class Statistics
{
  private int channel;

  private long firstTick;
  private long lastTick;
  private long maxTick;

  private int lowestKey;
  private int highestKey;

  private int setNumber;
  private int lastSetNumber = -1;

  private int totalKeys;
  private long totalKeyDuration;

  private int totalRests;
  private long totalRestDuration;
  
  private int[] keyCounts = new int[MidiConstants.MAX_MIDI_KEYS];

  private boolean firstTickSet = false;
  private boolean lowestKeySet = false;

  public Statistics(int channel)
  {
    this.channel = channel;
  }

  public void add(Note note)
  {
    long tick = note.getTick();
    int key = note.getKey();
    long duration = note.getDuration();
    
    if (!firstTickSet || (tick < firstTick))
    {
      firstTick = tick;
      firstTickSet = true;
    }

    if (tick > lastTick)
    {
      lastTick = tick;
    }

    long maxTick = tick + duration;
    if (maxTick > this.maxTick)
    {
      this.maxTick = maxTick;
    }
    
    if (!lowestKeySet || (key < lowestKey))
    {
      lowestKey = key;
      lowestKeySet = true;
    }

    if (key > highestKey)
    {
      highestKey = key;
    }

    if (setNumber != lastSetNumber)
    {
      this.setNumber++;
      lastSetNumber = setNumber;
    }

    totalKeyDuration += duration;

    keyCounts[key]++;

    totalKeys++;
  }

  public void addRest(long tick, long duration)
  {
    totalRestDuration += duration;
    totalRests++;
  }

  /**
   * Called after note has been removed from noteList.
   */
  public void remove(Note note, Midi set)
  {
    long tick = note.getTick();
    if (tick == firstTick)
    {
      firstTick = set.findFirstTick(channel);
      if (firstTick == -1)
      {
        firstTick = 0;
        firstTickSet = false;
      }
    }

    if (tick == lastTick)
    {
      lastTick = set.findLastTick(channel);
    }

    if (tick + note.getDuration() == maxTick)
    {
      maxTick = set.findMaxTick(channel);
    }
    
    int key = note.getKey();
    if (key == lowestKey)
    {
      lowestKey = set.getLowestKey(channel, note);
      if (lowestKey == -1)
      {
        lowestKey = 0;
        lowestKeySet = false;
      }
    }

    if (key == highestKey)
    {
      highestKey = set.getHighestKey(channel, note);
    }

    totalKeyDuration -= note.getDuration();

    keyCounts[key]--;

    totalKeys--;
  }

  public int getAverageKey()
  {
    int totalKeys = 0;
    int totalPitch = 0;
    for (int i = 0; i < keyCounts.length; i++)
    {
      int keyCount = keyCounts[i];
      totalKeys += keyCount;
      totalPitch += keyCount * i;
    }
    int averageKey = totalPitch / totalKeys;
    return averageKey;
  }

  public long getAverageKeyDuration()
  {
    long averageKeyDuration = 0;
    if (totalKeys > 0)
    {
      averageKeyDuration = totalKeyDuration / totalKeys;
    }
    return averageKeyDuration;
  }

  public long getAverageRestDuration()
  {
    long averageRestDuration = 0;
    if (totalRests > 0)
    {
      averageRestDuration = totalRestDuration / totalRests;
    }
    return averageRestDuration;
  }

  public int getDistinctKeys()
  {
    int distinctKeys = 0;
    for (int noteCount : keyCounts)
    {
      if (noteCount > 0)
      {
        distinctKeys++;
      }
    }
    return distinctKeys;
  }

  public long getFirstTick()
  {
    return firstTick;
  }

  public int getHighestKey()
  {
    return highestKey;
  }

  public long getLastTick()
  {
    return lastTick;
  }

  public long getMaxTick()
  {
    return maxTick;
  }

  public int getLowestKey()
  {
    return lowestKey;
  }

  public int[] getKeyCounts()
  {
    return keyCounts;
  }

  public long getOccupancy()
  {
    long occupancy = 0;
    long activeDuration = lastTick - firstTick;
    if (activeDuration > 0)
    {
      occupancy = totalKeyDuration / activeDuration;
    }
    return occupancy;
  }

  public long getTotalKeyDuration()
  {
    return totalKeyDuration;
  }

  public int getTotalKeys()
  {
    return totalKeys;
  }

  public long getTotalRestDuration()
  {
    return totalRestDuration;
  }

  public int getTotalRests()
  {
    return totalRests;
  }

  public String toString()
  {
    if (totalKeys == 0)
    {
      return "totalKeys=0";
    }

    StringBuffer buffer = new StringBuffer();
    buffer.append("Note Range: ");
    buffer.append(NoteName.getNoteName(getLowestKey()));
    buffer.append(" - ");
    buffer.append(NoteName.getNoteName(getHighestKey()));
    buffer.append(", Tick Range: ");
    buffer.append(getFirstTick());
    buffer.append(" - ");
    buffer.append(getLastTick());
    buffer.append(", Total Notes: ");
    buffer.append(Integer.toString(getTotalKeys()));
    buffer.append(", Distinct Notes: ");
    buffer.append(Integer.toString(getDistinctKeys()));
    buffer.append(", Average Note Duration: ");
    buffer.append(Long.toString(getAverageKeyDuration()));
    buffer.append(", Total Note Duration: ");
    buffer.append(Long.toString(getTotalKeyDuration()));
    buffer.append(", Occupancy: ");
    buffer.append(Long.toString(getOccupancy()));
    buffer.append(", Total Rests: ");
    buffer.append(Integer.toString(getTotalRests()));
    buffer.append(", Average Rest Duration: ");
    buffer.append(Long.toString(getAverageRestDuration()));
    buffer.append(", Total Rest Duration: ");
    buffer.append(Long.toString(getTotalRestDuration()));
    return buffer.toString();
  }

}
