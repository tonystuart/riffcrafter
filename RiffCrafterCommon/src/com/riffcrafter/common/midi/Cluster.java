// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

import java.util.TreeMap;

public class Cluster
{
  private TreeMap<Long, Integer> set = new TreeMap<Long, Integer>();

  public Cluster()
  {
    set.put(-1000000000L, 1); // NB: See caveat about Math.abs and MIN_VALUE
    set.put(+1000000000L, 1);
  }

  public long train(long value)
  {
    long closest = get(value);
    int sampleSize = set.get(closest);

    int maximumDistance;

    if (value < 50)
    {
      maximumDistance = 10;
    }
    else if (value < 100)
    {
      maximumDistance = 20;
    }
    else if (value < 300)
    {
      maximumDistance = 40;
    }
    else
    {
      maximumDistance = 50;
    }

    long distance = Math.abs(closest - value);

    if (closest == value)
    {
      set.put(closest, sampleSize + 1);
    }
    else if (distance > maximumDistance)
    {
      set.put(value, 1);
    }
    else
    {
      long weightedAverage = ((closest * sampleSize) + value) / ++sampleSize;
      if (weightedAverage != closest)
      {
        set.remove(closest);
      }
      set.put(weightedAverage, sampleSize);
    }
    return value;
  }

  public long get(long value)
  {
    long closest;
    Long floor = set.floorKey(value);
    long floorValue = floor.longValue();
    if (floorValue == value)
    {
      closest = floorValue;
    }
    else
    {
      Long ceiling = set.ceilingKey(value);
      long ceilingValue = ceiling.longValue();
      if (ceilingValue == value)
      {
        closest = ceilingValue;
      }
      else
      {
        if (Math.abs(floorValue - value) < Math.abs(ceilingValue - value))
        {
          closest = floorValue;
        }
        else
        {
          closest = ceilingValue;
        }
      }
    }
    return closest;
  }
}

