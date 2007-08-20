// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;


public class StatisticsManager
{
  private Statistics[] channelStatistics = new Statistics[MidiConstants.MAX_CHANNELS];

  public StatisticsManager()
  {
    for (int i = 0; i < channelStatistics.length; i++)
    {
      channelStatistics[i] = new Statistics(i);
    }
  }

  public void add(int channel, Note note)
  {
    channelStatistics[channel].add(note);
  }

  public void remove(int channel, Note note, Midi set)
  {
    channelStatistics[channel].remove(note, set);
  }

  public Statistics getStatistics(int channel)
  {
    return channelStatistics[channel];
  }

  /**
   * If there are:
   * 
   * key count
   * 50 -  5
   * 40 - 10
   * 
   * We want to bias the result toward 40
   */

  public int getAverageKey()
  {
    int totalKeys = 0;
    int totalPitch = 0;
    for (Statistics statistics : channelStatistics)
    {
      int[] keyCounts = statistics.getKeyCounts();
      for (int i = 0; i < keyCounts.length; i++)
      {
        int keyCount = keyCounts[i];
        if (keyCount > 0)
        {
          totalKeys += keyCount;
          totalPitch += keyCount * i;
        }
      }
    }
    int averageKey = totalPitch / totalKeys;
    return averageKey;
  }
  
  public int getAverageKey(int channel)
  {
    int totalKeys = 0;
    int totalPitch = 0;
    int[] keyCounts = channelStatistics[channel].getKeyCounts();
    for (int i = 0; i < keyCounts.length; i++)
    {
      int keyCount = keyCounts[i];
      if (keyCount > 0)
      {
        totalKeys += keyCount;
        totalPitch += keyCount * i;
      }
    }
    int averageKey = totalPitch / totalKeys;
    return averageKey;
  }

  public long getMaxTick()
  {
    // TODO: For performance reasons, may want to maintain this value locally
    long maxTick = 0;

    for (Statistics statistics : channelStatistics)
    {
      maxTick = Math.max(maxTick, statistics.getMaxTick());
    }
    return maxTick;
  }

  public int getActiveChannelCount()
  {
    int activeChannelCount = 0;

    for (Statistics statistics : channelStatistics)
    {
      if (statistics.getTotalKeys() > 0)
      {
        activeChannelCount++;
      }
    }
    return activeChannelCount;
  }

  public int[] getActiveChannels()
  {
    int activeChannelCount = getActiveChannelCount();

    int[] channels = new int[activeChannelCount];

    int channel = 0;

    for (int i = 0; i < channelStatistics.length; i++)
    {
      if (channelStatistics[i].getTotalKeys() > 0)
      {
        channels[channel++] = i;
      }
    }

    return channels;
  }
  
  public void dump()
  {
    for (int i = 0; i < channelStatistics.length; i++)
    {
      if (channelStatistics[i].getTotalKeys() > 0)
      {
        System.out.println("statistics["+i+"]="+channelStatistics[i]);
      }
    }
  }

}
