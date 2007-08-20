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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

public class TickEventMap
{
  private static final int ANY_CHANNEL = -1;
  private static final TickEventList DEFAULT_EMPTY_LIST = new TickEventList();

  private Cluster ticks;
  private Cluster durations;
  
  private long tickRoundingFactor = -1;

  private Map<Long, TickEvent> tickEventMap = new TreeMap<Long, TickEvent>();

  /**
   * Create a TickEventMap that uses tick rounding (instead of
   * tick clustering) to normalize starting tick.
   */
  public TickEventMap(long tickRoundingFactor)
  {
    this.tickRoundingFactor = tickRoundingFactor;
  }
  
  public TickEventMap(Midi midi)
  {
    this(midi, ANY_CHANNEL);
  }

  public TickEventMap(Midi midi, int targetChannelIndex)
  {
    this(midi, targetChannelIndex, null, null);
  }

  public TickEventMap(Midi midi, int targetChannelIndex, Cluster ticks, Cluster durations)
  {
    this.ticks = ticks;
    this.durations = durations;

    for (Midel midel : midi.getMidels())
    {
      if (midel instanceof Note && (targetChannelIndex == ANY_CHANNEL || midel.getChannel() == targetChannelIndex))
      {
        Note note = (Note)midel;
        add(note);
      }
    }
  }
  
  public int size()
  {
    return tickEventMap.size();
  }

  private long roundTick(Note note)
  {
    long tick = note.getTick();
    long shortestDistance = Integer.MAX_VALUE;
    long closestTick = 0;
    for (Long tickMapTick : getTicks())
    {
      long distance = Math.abs(tick - tickMapTick);
      if (distance < shortestDistance)
      {
        closestTick = tickMapTick;
        shortestDistance = distance;
      }
    }
    if (shortestDistance < tickRoundingFactor)
    {
      tick = closestTick;
    }
    return tick;
  }

  public void add(Note note)
  {
    addOn(note);
    addOff(note);
  }

  private void addOn(Note note)
  {
    long tick = note.getTick();
    if (ticks != null)
    {
      tick = ticks.get(tick);
    }
    else if (tickRoundingFactor != -1)
    {
      tick = roundTick(note);
    }
    TickEvent tickEvent = findTickEvent(tick);
    tickEvent.addOn(note);
  }

  private void addOff(Note note)
  {
    long tick = note.getTick();
    long duration = note.getDuration();
    if (durations != null)
    {
      duration = durations.get(duration);
    }
    tick += duration;
    TickEvent tickEvent = findTickEvent(tick);
    tickEvent.addOff(note);
  }

  public TickEvent findTickEvent(long tick)
  {
    TickEvent tickEvent = tickEventMap.get(tick);
    if (tickEvent == null)
    {
      tickEvent = new TickEvent();
      tickEventMap.put(tick, tickEvent);
    }
    return tickEvent;
  }

  private Set<Long> getTicks()
  {
    return tickEventMap.keySet();
  }

  public Set<Entry<Long, TickEvent>> getTickEvents()
  {
    return tickEventMap.entrySet();
  }

  public class TickEvent
  {
    private TickEventList on = DEFAULT_EMPTY_LIST;
    private TickEventList off = DEFAULT_EMPTY_LIST;

    private void addOn(Note note)
    {
      if (on == DEFAULT_EMPTY_LIST)
      {
        on = new TickEventList();
      }
      on.add(note);
    }

    private void addOff(Note note)
    {
      if (off == DEFAULT_EMPTY_LIST)
      {
        off = new TickEventList();
      }
      off.add(note);
    }

    public Iterable<Note> getNoteOffIterable()
    {
      return off;
    }

    public Iterable<Note> getNoteOnIterable()
    {
      return on;
    }

    public Set<Note> getNoteOnSet()
    {
      return on;
    }

    public int getNoteOnSetSize()
    {
      return on.size();
    }

  }

  // Because we may normalize the tick using clustering, notes that did not originally
  // start on the same tick may end up on the same tick. In order for sequences of these
  // notes to compare deterministically, they must be maintained in order.

  private static class TickEventList extends TreeSet<Note>
  {
    private TickEventList()
    {
      super(new TickEventListComparator());
    }
  }

  private static class TickEventListComparator implements Comparator<Note>
  {
    public int compare(Note o1, Note o2)
    {
      int deltaKey = o1.getKey() - o2.getKey();
      if (deltaKey != 0)
      {
        return deltaKey;
      }
      return o1.hashCode() - o2.hashCode();
    }
  }

}
