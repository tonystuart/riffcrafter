// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.midi;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import com.riffcrafter.common.midi.ChannelEvent;
import com.riffcrafter.common.midi.Instruments;
import com.riffcrafter.common.midi.MetaEvent;
import com.riffcrafter.common.midi.Midel;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.common.midi.Note;
import com.riffcrafter.common.midi.Statistics;
import com.riffcrafter.common.midi.SysexEvent;
import com.riffcrafter.studio.app.Editor.SelectionManager;

public class GraphicalNotator extends Notator
{
  public static final int TICKS_PER_PIXEL = 10;

  private static final int NOTE_VERTICAL_SPACING = 3;
  private static final int NOTE_HEIGHT = NOTE_VERTICAL_SPACING;
  private static final int ARC_WIDTH = NOTE_VERTICAL_SPACING;
  private static final int ARC_HEIGHT = NOTE_VERTICAL_SPACING;

  public static final Color c00 = new Color(0xff, 0x00, 0x00);
  public static final Color c01 = new Color(0xff, 0xff, 0x00);
  public static final Color c02 = new Color(0xff, 0x00, 0xff);
  public static final Color c03 = new Color(0xff, 0xff, 0xff);

  public static final Color c04 = new Color(0x3f, 0xff, 0x3f);
  public static final Color c05 = new Color(0xff, 0xff, 0x7f);
  public static final Color c06 = new Color(0xff, 0x7f, 0xbf);
  public static final Color c07 = new Color(0xbf, 0xbf, 0xbf);

  public static final Color c08 = new Color(0x7f, 0xbf, 0xbf);
  public static final Color c09 = Color.ORANGE; // new Color(0x7f, 0x7f, 0xbf);
  public static final Color c10 = new Color(0x7f, 0xbf, 0x7f);
  public static final Color c11 = new Color(0x7f, 0x7f, 0x7f);

  public static final Color c12 = new Color(0x3f, 0xff, 0xff);
  public static final Color c13 = new Color(0x3f, 0x3f, 0xff);
  public static final Color c14 = new Color(0xff, 0x7f, 0x7f);
  public static final Color c15 = new Color(0x7f, 0x3f, 0xff);

  public static final Color[] colors = new Color[] {
      c00, c01, c02, c03, c04, c05, c06, c07, c08, c09, c10, c11, c12, c13, c14, c15
  };

  public GraphicalNotator(Midi midi, SelectionManager selectionManager)
  {
    super(midi, selectionManager);
    setBackground(Color.BLACK);
    setForeground(Color.DARK_GRAY);
  }

  @Override
  public int getStaffHeight(int activeChannelMapIndex)
  {
    return channelNoteCounts[activeChannelMapIndex] * NOTE_VERTICAL_SPACING;
  }

  @Override
  public int getNoteHeight()
  {
    return NOTE_HEIGHT;
  }

  @Override
  protected void getViewSpecificTickMetrics()
  {
    firstTick = currentTick - (ticksPerLine / 2);
    lastTick = currentTick + (ticksPerLine / 2);
    midiView = midi.getSet(firstTick - leftMargin, lastTick);
  }

  @Override
  protected int getDefaultTicksPerPixel()
  {
    return TICKS_PER_PIXEL;
  }

  @Override
  protected void paintStaffs(Graphics2D g2d, int ticksPerMeasure, long firstMeasureTick, long lastMeasureTick)
  {
    for (int channelPosition = 0; channelPosition < activeChannelMap.length; channelPosition++)
    {
      int channel = activeChannelMap[channelPosition];
      Statistics statistics = midi.getStatistics(channel);
      int firstLine = getLowestKey(statistics);
      int lastLine = getHighestKey(statistics) + 1;
      for (int j = firstLine; j <= lastLine; j++)
      {
        int y = adjustY(getY(channelPosition, j - firstLine));
        g2d.drawLine(0, y, width, y);
      }
      for (long j = firstMeasureTick; j <= lastMeasureTick; j += ticksPerMeasure)
      {
        int x = getX(j);
        int y1 = adjustY(getY(channelPosition, 0));
        int y2 = adjustY(getY(channelPosition, lastLine - firstLine));
        g2d.drawLine(x, y1, x, y2);
      }
    }
  }

  @Override
  protected void paintTitles(Graphics2D g2d)
  {
    for (int i = 0; i < activeChannelMap.length; i++)
    {
      int channel = activeChannelMap[i];
      g2d.setColor(colors[channel]);
      String title = channelTitles[channel] + channelRanges[channel];
      int yTitle = adjustY(getYTitle(channel));
      g2d.drawString(title, 5, yTitle);
    }
  }

  @Override
  protected void paintMidels(Graphics2D g2d, Iterable<Midel> midels, boolean isSelected, ArrayList<MetaEvent> lyrics)
  {
    for (Midel midel : midels)
    {
      paint(g2d, midel, isSelected, lyrics);
    }
  }

  private void paint(Graphics2D g2d, Midel midel, boolean isSelected, ArrayList<MetaEvent> lyrics)
  {
    if (midel instanceof Note)
    {
      paint(g2d, (Note)midel, isSelected);
    }
    else if (midel instanceof ChannelEvent)
    {
      paint(g2d, (ChannelEvent)midel, isSelected);
    }
    else if (midel instanceof MetaEvent)
    {
      paint(g2d, (MetaEvent)midel, isSelected, lyrics);
    }
    else if (midel instanceof SysexEvent)
    {
      paint(g2d, (SysexEvent)midel, isSelected);
    }

  }

  protected void paint(Graphics2D g2d, Note note, boolean isSelected)
  {
    int channel = note.getChannel();
    long tick = note.getTick();
    long duration = note.getDuration();

    int x = getX(tick);
    int y = adjustY(getY(note));

    int width = getX(tick + duration) - x;

    g2d.setColor(colors[channel]);
    g2d.setStroke(normalStroke);

    if (isSelected)
    {
      if (channel == Instruments.DRUM_CHANNEL)
      {
        g2d.setStroke(wideStroke);
        g2d.drawLine(x, y, x + width, y + 5);
        g2d.drawLine(x, y + 5, x + width, y);
        g2d.setStroke(normalStroke);
      }
      else
      {
        g2d.fillRoundRect(x, y, width, NOTE_HEIGHT + 1, ARC_WIDTH, ARC_HEIGHT + 1);
      }
    }
    else
    {
      if (channel == Instruments.DRUM_CHANNEL)
      {
        g2d.drawLine(x, y, x + width, y + 5);
        g2d.drawLine(x, y + 5, x + width, y);
      }
      else
      {
        g2d.drawRoundRect(x, y, width, NOTE_HEIGHT, ARC_WIDTH, ARC_HEIGHT);
      }
    }
  }

  @Override
  protected int getY(Midel midel)
  {
    int y;
    if (midel instanceof Note)
    {
      Note note = (Note)midel;
      int channel = note.getChannel();
      int key = note.getKey();
      int highestKey = getHighestKey(midi.getStatistics(channel));
      int channelPosition = reverseChannelMap[channel];
      y = getY(channelPosition, highestKey - key);
    }
    else
    {
      y = super.getY(midel);
    }
    return y;
  }

  @Override
  protected Color getColor(int channel)
  {
    return colors[channel];
  }

  @Override
  protected Color getMetaEventColor()
  {
    return Color.WHITE;
  }

  @Override
  protected Color getSysexEventColor()
  {
    return Color.GREEN;
  }
  
  @Override
  protected Note getNote(int x, int y)
  {
    int lastOffsetIndex = channelOffsets.length - 1;
    for (int channelPosition = 0; channelPosition < channelOffsets.length; channelPosition++)
    {
      if (y > channelOffsets[channelPosition] && (channelPosition == lastOffsetIndex || y < channelOffsets[channelPosition+1]))
      {
        int channel = activeChannelMap[channelPosition];
        int deltaY = y - channelOffsets[channelPosition];
        int deltaKey = deltaY / getNoteHeight();
        int highestKey = getHighestKey(midi.getStatistics(channel));
        int key = highestKey - deltaKey;
        int tick = getTicks(x);
        Note note = new Note(channel, key, 64, tick, 250);
        return note;    
      }
    }
    return null;
  }

}
