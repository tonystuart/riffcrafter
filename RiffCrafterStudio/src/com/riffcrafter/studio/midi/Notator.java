// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.midi;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NavigableSet;

import javax.swing.JPanel;

import com.riffcrafter.common.midi.Channel;
import com.riffcrafter.common.midi.ChannelEvent;
import com.riffcrafter.common.midi.Instruments;
import com.riffcrafter.common.midi.MetaEvent;
import com.riffcrafter.common.midi.Midel;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.common.midi.MidiConstants;
import com.riffcrafter.common.midi.Note;
import com.riffcrafter.common.midi.NoteName;
import com.riffcrafter.common.midi.Statistics;
import com.riffcrafter.common.midi.SysexEvent;
import com.riffcrafter.common.midi.TempoChange;
import com.riffcrafter.common.midi.TimeSignatureChange;
import com.riffcrafter.library.util.Broker.Listener;
import com.riffcrafter.studio.app.Editor.SelectionManager;

public abstract class Notator extends JPanel
{
  private ArrayList<NotatorListener> listeners = new ArrayList<NotatorListener>();

  private static final int MINIMUM_WIDTH_IN_PIXELS = 300;
  private static final int DETAILS_TOP_MARGIN = 7;
  private static final int DETAILS_BOTTOM_MARGIN = 9;

  private static final int HIGHLIGHT_WIDTH = 3;

  private static final int MH = 4;
  private static final int MHM = (MH / 2);
  private static final int MW = 4;
  private static final int MWM = (MW / 2);
  private static final int GAP = 1;

  public static final Color LIGHT_CORAL = new Color(0xF0, 0x80, 0x80, 0x80);

  public static final Color HIGHLIGHT = LIGHT_CORAL;

  protected static final BasicStroke normalStroke = new BasicStroke(1f);
  protected static final BasicStroke wideStroke = new BasicStroke(2f);
  private static final BasicStroke dottedStroke = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] { 2f }, 0f);

  private static final int CHANNEL_SPECIFIC_SCALE = 0;
  private static final int CHANNEL_SPECIFIC_SCALE_ALL = 1;
  private static final int CHANNEL_COMMON_SCALE = 2;
  private static final int CHANNEL_STYLE_MAX = 3;

  private int channelStyle = CHANNEL_SPECIFIC_SCALE;
  private int overallLowestKey;
  private int overallHighestKey;

  protected Midi midi;
  private SelectionManager selectionManager;

  public int ticksPerPixel = getDefaultTicksPerPixel();
  protected int ticksPerLine = MINIMUM_WIDTH_IN_PIXELS * ticksPerPixel; // Initial value only -- recalculated on first parent component layout

  protected long currentTick;
  protected long firstTick;
  protected long lastTick;

  protected int[] activeChannelMap;
  protected int[] channelNoteCounts;
  private int[] channelInstruments;
  protected String[] channelTitles;
  protected String[] channelRanges;

  private MidiMouseListener midiMouseListener = new MidiMouseListener();
  private NotatorMidiListener notatorMidiListener = new NotatorMidiListener();
  private MidiComponentListener midiComponentListener = new MidiComponentListener();

  private Rectangle selectionRectangleInTicks;

  // NB: The following fields are maintained for performance reasons

  protected int width;
  private int height;
  protected int leftMargin;

  private Font titleFont;
  private int titleFontHeight;

  private Font lyricFont;
  private int lyricFontHeight;
  private int lyricFontWidth;

  protected int[] channelOffsets;
  protected int[] reverseChannelMap = new int[MidiConstants.MAX_CHANNELS];

  protected NavigableSet<Midel> midiView;

  private boolean isUpdateTickMetrics;
  private boolean isUpdateMidiDependencies;

  public Notator(Midi midi, SelectionManager selectionManager)
  {
    super(null);
    this.midi = midi;
    this.selectionManager = selectionManager;
    selectionManager.subscribe(new SelectionListener());

    if (midi.size() == 0)
    {
      channelStyle = CHANNEL_SPECIFIC_SCALE_ALL;
    }

    addMouseListener(midiMouseListener);
    addComponentListener(midiComponentListener);
    setAutoscrolls(true);
    setFocusable(true);
    requestFocusInWindow(true);

    initializeFontMetrics();

    midi.addMidiListener(notatorMidiListener);
    setOpaque(true);
    setToolTipText("");

    channelInstruments = new int[MidiConstants.MAX_CHANNELS];
    channelTitles = new String[MidiConstants.MAX_CHANNELS];
    channelRanges = new String[MidiConstants.MAX_CHANNELS];
    Arrays.fill(channelInstruments, -2); // NB: Midi.getProgram returns -1 for DRUM_CHANNEL

    isUpdateMidiDependencies = true;
    isUpdateTickMetrics = true;

    // Get height for vertical scroll bar
    updateMidiDependencies();
  }

  //      0
  //  y0: top inset
  //  y1: fontHeight              Measure Text
  //  y2: GAP + MHM               SysexEvent Marker Height Midpoint
  //      MHM
  //  y3: GAP + fontHeight        Optional Lyrics Text
  //  y4: 4 * GAP                 Bottom of Nonscrolling Area
  //  y5: GAP + MHM               MetaEvent Marker Height Midpoint
  //      MHM
  //  y6: GAP                     Base of Channels

  private int y0;
  private int y1;
  private int y2;
  private int y3;
  private int y4;
  private int y5;
  private int y6;

  private int verticalOffset;

  private void calculateMetrics()
  {
    y0 = 2;
    y1 = y0 + titleFontHeight;
    y2 = y1 + GAP + MHM;
    y3 = y2 + MHM;
    if (midi.containsLyrics())
    {
      y3 += GAP + lyricFontHeight;
    }
    y4 = y3 + 4 * GAP;
    y5 = y4 + GAP + MHM;
    y6 = y5 + MHM + titleFontHeight;
  }

  private void initializeFontMetrics()
  {
    Font defaultFont = getFont();

    titleFont = defaultFont;
    FontMetrics titleFontMetrics = getFontMetrics(titleFont);
    titleFontHeight = titleFontMetrics.getAscent();

    lyricFont = defaultFont.deriveFont(9f);
    FontMetrics lyricFontMetrics = getFontMetrics(lyricFont);
    lyricFontHeight = lyricFontMetrics.getAscent();
    lyricFontWidth = lyricFontMetrics.charWidth('M');
  }

  public void toggleChannelStyle()
  {
    channelStyle = (channelStyle + 1) % CHANNEL_STYLE_MAX;
    scheduleUpdateMidiDependencies();
  }

  private void updateInstruments()
  {
    for (int i = 0; i < activeChannelMap.length; i++)
    {
      int channel = activeChannelMap[i];
      updateInstrument(channel, currentTick);
    }
  }

  private void updateInstrument(int channel, long tick)
  {
    int instrument = midi.getProgram(channel, tick);
    if (instrument != channelInstruments[channel])
    {
      updateChannelTitle(channel, instrument);
      fireInstrumentChange(channel, instrument);
      channelInstruments[channel] = instrument;
    }
  }

  private void updateChannelTitle(int channel, int instrument)
  {
    String instrumentName;
    if (channel == Instruments.DRUM_CHANNEL)
    {
      // Some MIDI files contain a ProgramChange event for DRUM_CHANNEL
      instrumentName = Instruments.DRUM_SET;
    }
    else
    {
      instrumentName = Instruments.getProgramName(instrument);
    }

    int channelNumber = Channel.getChannelNumber(channel);
    channelTitles[channel] = "Channel " + channelNumber + ":  " + instrumentName;
    repaint();
  }

  public void notifyMouseOutsideWindow(long currentTick)
  {
    if (midiMouseListener.isMove)
    {
      scheduleUpdateTickMetrics(currentTick);
    }
  }

  public void setCurrentTick(long currentTick, boolean isRequired)
  {
    if ((isRequired || Math.abs(currentTick - this.currentTick) > ticksPerPixel) && !midiMouseListener.isMove)
    {
      scheduleUpdateTickMetrics(currentTick);
    }
  }

  private void scheduleUpdateTickMetrics(long currentTick)
  {
    this.currentTick = currentTick;
    isUpdateTickMetrics = true;
    repaint();
  }

  private void updateTickMetrics()
  {
    if (isUpdateTickMetrics)
    {
      getViewSpecificTickMetrics();
      isUpdateTickMetrics = false;
    }
  }

  protected abstract void getViewSpecificTickMetrics();

  private void scheduleUpdateMidiDependencies()
  {
    isUpdateMidiDependencies = true;
    repaint();
  }

  private void updateMidiDependencies()
  {
    if (isUpdateMidiDependencies)
    {
      calculateMetrics();
      leftMargin = midi.findTicksPerMeasure(currentTick) * 2;

      int ticksPerPixel = getDefaultTicksPerPixel();
      if (midi.containsLyrics())
      {
        int ticksPerLetter = midi.getTicksPerLetter(); // e.g. 40 ticks per 'm'
        ticksPerPixel = ticksPerLetter / lyricFontWidth;
      }
      setTicksPerPixel(ticksPerPixel);

      initializeActiveChannelMap();
      initializeReverseChannelMap();

      int width = MINIMUM_WIDTH_IN_PIXELS;
      int height = initializeChannelOffsets();

      setPreferredSize(new Dimension(width, height));
      setMinimumSize(new Dimension(0, height));
      setMaximumSize(new Dimension(Integer.MAX_VALUE, height));

      isUpdateMidiDependencies = false;
    }
  }

  protected abstract int getDefaultTicksPerPixel();

  private void setTicksPerPixel(int ticksPerPixel)
  {
    this.ticksPerPixel = ticksPerPixel;
    setTicksPerLine();
  }

  private void setTicksPerLine()
  {
    int width = getWidth();
    ticksPerLine = width * ticksPerPixel;
  }

  private void initializeActiveChannelMap()
  {
    if (channelStyle == CHANNEL_SPECIFIC_SCALE_ALL)
    {
      activeChannelMap = new int[MidiConstants.MAX_CHANNELS];
      for (int i = 0; i < activeChannelMap.length; i++)
      {
        activeChannelMap[i] = i;
      }
    }
    else
    {
      activeChannelMap = midi.getActiveChannels();
    }

    channelNoteCounts = new int[activeChannelMap.length];

    if (channelStyle == CHANNEL_COMMON_SCALE)
    {
      // Need to make extra pass through to find lowest and highest
      overallLowestKey = 127;
      overallHighestKey = 0;
      for (int i = 0; i < activeChannelMap.length; i++)
      {
        int channel = activeChannelMap[i];
        Statistics statistics = midi.getStatistics(channel);
        int lowestKey = statistics.getLowestKey();
        int highestKey = statistics.getHighestKey();
        overallLowestKey = Math.min(overallLowestKey, lowestKey);
        overallHighestKey = Math.max(overallHighestKey, highestKey);
      }
    }

    for (int i = 0; i < activeChannelMap.length; i++)
    {
      int channel = activeChannelMap[i];
      Statistics statistics = midi.getStatistics(channel);
      int lowestKey = getLowestKey(statistics);
      int highestKey = getHighestKey(statistics);
      channelNoteCounts[i] = (highestKey - lowestKey) + 1; // +1 because they're inclusive
    }

  }

  private void initializeReverseChannelMap()
  {
    for (int i = 0; i < reverseChannelMap.length; i++)
    {
      reverseChannelMap[i] = -1;
    }

    for (int i = 0; i < activeChannelMap.length; i++)
    {
      reverseChannelMap[activeChannelMap[i]] = i;
    }
  }

  private int initializeChannelOffsets()
  {
    channelOffsets = new int[activeChannelMap.length];

    int y = y6;

    for (int i = 0; i < activeChannelMap.length; i++)
    {
      int channel = activeChannelMap[i];
      getChannelRange(channel);

      if (i > 0)
      {
        y += DETAILS_TOP_MARGIN;
      }

      y += titleFontHeight;
      y += DETAILS_BOTTOM_MARGIN;

      channelOffsets[i] = y;

      y += getStaffHeight(i);
    }

    y += DETAILS_BOTTOM_MARGIN;

    return y;
  }

  public abstract int getStaffHeight(int activeChannelMapIndex);

  public void getChannelRange(int channel)
  {
    String text = "";
    Statistics channelStatistics = midi.getStatistics(channel);
    int lowestKey = getLowestKey(channelStatistics);
    int highestKey = getHighestKey(channelStatistics);
    if (channelStatistics.getTotalKeys() > 0)
    {
      String lowestKeyName = NoteName.getNoteName(channel, lowestKey);
      String highestKeyName = NoteName.getNoteName(channel, highestKey);
      text = "  " + lowestKeyName + " (" + lowestKey + ") - " + highestKeyName + " (" + highestKey + ")";
    }
    channelRanges[channel] = text;
  }

  public Rectangle getSelectionRectangleInTicks()
  {
    return selectionRectangleInTicks;
  }

  public void selectNotes(boolean isAppendToSelection)
  {
    Midi selection = selectionManager.getSelection();

    if (!isAppendToSelection || selection == null)
    {
      selection = new Midi();
    }

    selectNotes(selection, selectionRectangleInTicks, isAppendToSelection);
    selectionManager.setSelection(selection, this, true, false);

    updateSelectionHoverText(selection);
  }

  private void updateSelectionHoverText(Midi selection)
  {
    String hoverText = "";
    if (selection != null && selection.size() == 1)
    {
      hoverText = selection.first().getToolTipText();
    }
    fireHover(hoverText);
  }

  public void selectNotes(Midi target, Rectangle rectangleInTicks, boolean isAppendToSelection)
  {
    int left = rectangleInTicks.x;
    int right = left + rectangleInTicks.width;
    int top = rectangleInTicks.y;
    int bottom = top + rectangleInTicks.height;

    for (Midel midel : midi.getSet(left - leftMargin, right))
    {
      long tick = midel.getTick();
      long duration = 0;

      if (midel instanceof Note)
      {
        Note note = (Note)midel;
        duration = note.getDuration();
      }

      if ((left < (tick + duration) && (right > tick)))
      {
        int y = getY(midel);

        if ((top < (y + getNoteHeight())) && (bottom > y))
        {
          if (isAppendToSelection && target.contains(midel))
          {
            target.remove(midel);
          }
          else
          {
            target.add(midel);
          }
        }
      }
    }

  }

  public abstract int getNoteHeight();

  private Rectangle expandPoint(Point point)
  {
    return new Rectangle(point.x - 1, point.y - 1, 2, 2);
  }

  private Rectangle expandRectangle(Point pointInTicks)
  {
    int x = pointInTicks.x - ticksPerPixel * MWM;
    int y = pointInTicks.y - MHM;
    int width = ticksPerPixel * MW;
    int height = MH;

    return new Rectangle(x, y, width, height);
  }

  public Note findNoteAt(Iterable<Midel> iterable, Point pointInTicks)
  {
    Rectangle rectangleInTicks = expandPoint(pointInTicks);

    int left = rectangleInTicks.x;
    int right = rectangleInTicks.x + rectangleInTicks.width;
    int top = rectangleInTicks.y;
    int bottom = rectangleInTicks.y + rectangleInTicks.height;

    for (Midel midel : iterable)
    {
      if (midel instanceof Note)
      {
        Note note = (Note)midel;

        long tick = note.getTick();
        long duration = note.getDuration();

        if ((left < (tick + duration) && (right > tick)))
        {
          int y = getY(note);

          if ((top < (y + getNoteHeight())) && (bottom > y))
          {
            return note;
          }
        }
      }
    }

    return null;
  }

  @Override
  public String getToolTipText(MouseEvent e)
  {
    Point pointInTicks = getTicks(e.getPoint());
    Rectangle selectionRectangleInTicks = expandRectangle(pointInTicks);

    Midi selectionMidi = new Midi();

    selectNotes(selectionMidi, selectionRectangleInTicks, false);

    StringBuffer toolTipText = new StringBuffer();

    for (Midel midel : selectionMidi.getMidels())
    {
      if (toolTipText.length() > 0)
      {
        toolTipText.append(";  ");
      }
      toolTipText.append(midel.getToolTipText());
    }

    fireHover(toolTipText.toString());
    return null;
  }

  public void setVerticalOffset(int verticalOffset)
  {
    this.verticalOffset = verticalOffset;
  }

  private void paintBackground(Graphics g)
  {
    if (isOpaque())
    {
      g.setColor(getBackground());
      g.fillRect(0, 0, width, height);
    }
  }

  @Override
  protected void paintComponent(Graphics g)
  {
    Graphics2D g2d = (Graphics2D)g;

    updateMidiDependencies();
    updateInstruments();
    updateTickMetrics();

    width = getWidth();
    height = getHeight();
    int ticksPerMeasure = midi.findTicksPerMeasure(currentTick);

    paintBackground(g2d);
    paintHighlight(g2d);

    long firstMeasureTick = (firstTick / ticksPerMeasure) * ticksPerMeasure;
    long lastMeasureTick = (lastTick / ticksPerMeasure) * ticksPerMeasure;

    g2d.setFont(titleFont);
    g2d.setColor(getForeground());

    paintStaffs(g2d, ticksPerMeasure, firstMeasureTick, lastMeasureTick);
    paintTitles(g2d);

    ArrayList<MetaEvent> lyrics = new ArrayList<MetaEvent>();

    paintMidels(g2d, midiView, false, lyrics);

    Midi selection = selectionManager.getSelection();
    if (selection != null)
    {
      paintMidels(g2d, selection.getSet(firstTick - leftMargin, lastTick), true, null);
    }

    if (selectionRectangleInTicks != null)
    {
      int x = getX(selectionRectangleInTicks.x);
      int y = adjustY(selectionRectangleInTicks.y);
      int width = selectionRectangleInTicks.width / ticksPerPixel;
      g2d.setColor(Color.GRAY);
      g2d.setStroke(dottedStroke);
      g2d.drawRect(x, y, width, selectionRectangleInTicks.height);
    }

    paintNonScrollingArea(g2d, ticksPerMeasure, lyrics);
  }

  protected abstract void paintStaffs(Graphics2D g2d, int ticksPerMeasure, long firstMeasureTick, long lastMeasureTick);

  protected abstract void paintTitles(Graphics2D g2d);

  protected abstract void paintMidels(Graphics2D g2d, Iterable<Midel> midels, boolean isSelected, ArrayList<MetaEvent> lyrics);

  private void paintHighlight(Graphics2D g2d)
  {
    int currentX = getX(currentTick);
    g2d.setColor(HIGHLIGHT);
    g2d.fillRect(currentX, 0, HIGHLIGHT_WIDTH, getHeight());
  }

  private void paintNonScrollingArea(Graphics2D g2d, int ticksPerMeasure, ArrayList<MetaEvent> lyrics)
  {
    StringBuffer buffer = new StringBuffer(64);

    buffer.append("Measure: ");
    buffer.append(Long.toString(currentTick / ticksPerMeasure));
    buffer.append(", Tick: ");
    buffer.append(Long.toString(currentTick));

    TimeSignatureChange timeSignatureChange = midi.findTimeSignatureChange(currentTick);
    if (timeSignatureChange != null)
    {
      buffer.append(", ");
      buffer.append(timeSignatureChange.getToolTipText());
    }

    TempoChange tempoChange = midi.findTempoChange(currentTick);
    if (tempoChange != null)
    {
      buffer.append(", ");
      buffer.append(tempoChange.getToolTipText());
    }

    String heading = buffer.toString();
    FontMetrics fontMetrics = g2d.getFontMetrics();
    int stringWidth = fontMetrics.stringWidth(heading);
    int x = (width - stringWidth) / 2;

    g2d.setColor(getForeground());
    g2d.fillRect(0, 0, width, y4);

    g2d.setColor(Color.WHITE);
    g2d.drawString(buffer.toString(), x, y1);

    g2d.setFont(lyricFont);
    for (MetaEvent metaEvent : lyrics)
    {
      long tick = metaEvent.getTick();
      x = getX(tick);
      String string = metaEvent.getText();
      g2d.drawString(string, x, y3);
    }
    g2d.setFont(titleFont);
  }

  protected void paint(Graphics2D g2d, ChannelEvent channelEvent, boolean isSelected)
  {
    long tick = channelEvent.getTick();
    int channel = channelEvent.getChannel();
    int x = getX(tick);
    int y = adjustY(getY(channelEvent));

    g2d.setColor(getColor(channel));

    if (isSelected)
    {
      g2d.setStroke(wideStroke);
      g2d.drawLine(x, y - MHM, x, y + MHM);
      g2d.drawLine(x - MWM, y, x + MWM, y);
      g2d.setStroke(normalStroke);
    }
    else
    {
      g2d.drawLine(x, y - MHM, x, y + MHM);
      g2d.drawLine(x - MWM, y, x + MWM, y);
    }
  }

  protected abstract Color getColor(int channel);

  protected abstract Color getMetaEventColor();

  protected abstract Color getSysexEventColor();

  protected void paint(Graphics2D g2d, MetaEvent metaEvent, boolean isSelected, ArrayList<MetaEvent> lyrics)
  {
    long tick = metaEvent.getTick();
    int type = metaEvent.getType();

    int x = getX(tick);

    g2d.setColor(getMetaEventColor());

    if (lyrics != null && isLyrics(type, tick))
    {
      lyrics.add(metaEvent);
    }

    if (isSelected)
    {
      g2d.setStroke(wideStroke);
      drawMetaEventMarker(g2d, x);
      g2d.setStroke(normalStroke);
    }
    else
    {
      drawMetaEventMarker(g2d, x);
    }

  }

  private void drawMetaEventMarker(Graphics2D g2d, int x)
  {
    // See also: getY(Midel midel)
    int top = adjustY(y5 - MWM);
    int middle = adjustY(y5);
    int bottom = adjustY(y5 + MHM);

    g2d.drawLine(x - MWM, middle, x, top);
    g2d.drawLine(x, top, x + MWM, middle);
    g2d.drawLine(x + MWM, middle, x, bottom);
    g2d.drawLine(x, bottom, x - MWM, middle);
  }

  private final boolean isLyrics(int type, long tick)
  {
    return tick > 0 && type == midi.getLyricType();
  }

  protected void paint(Graphics2D g2d, SysexEvent sysexEvent, boolean isSelected)
  {
    long tick = sysexEvent.getTick();

    int x = getX(tick);

    g2d.setColor(getSysexEventColor());

    if (isSelected)
    {
      g2d.setStroke(wideStroke);
      g2d.drawRect(x - MWM, y2 - MHM, getNoteHeight(), getNoteHeight());
      g2d.setStroke(normalStroke);
    }
    else
    {
      g2d.drawRect(x - MWM, y2 - MHM, getNoteHeight(), getNoteHeight());
    }

  }

  public final int getX(long tick)
  {
    int x = ((int)(tick - firstTick)) / ticksPerPixel;
    return x;
  }

  protected abstract Note getNote(int x, int y);

  public final int getY(int channelPosition, int noteIndex)
  {
    return channelOffsets[channelPosition] + (noteIndex * getNoteHeight());
  }

  protected int getY(Midel midel)
  {
    int y;
    if (midel instanceof ChannelEvent)
    {
      ChannelEvent channelEvent = (ChannelEvent)midel;
      int channel = channelEvent.getChannel();
      int channelPosition = reverseChannelMap[channel];
      if (channelPosition == -1)
      {
        // ChannelEvent for a channel that contains no Notes
        return 0;
      }
      y = getY(channelPosition, 0) - getNoteHeight();
    }
    else if (midel instanceof MetaEvent)
    {
      y = y5;
    }
    else if (midel instanceof SysexEvent)
    {
      y = y2;
    }
    else
    {
      throw new RuntimeException("getY is unimplemented for supplied type.");
    }
    return y;
  }

  protected int getYTitle(int channel)
  {
    int y = channelOffsets[reverseChannelMap[channel]] - DETAILS_BOTTOM_MARGIN;
    return y;
  }

  public Rectangle getYChannel(int channel)
  {
    Rectangle r = null;
    int channelPosition = getClosestChannel(channel);
    if (channelPosition != -1)
    {
      r = new Rectangle();
      int topHeader = y6 + titleFontHeight + DETAILS_BOTTOM_MARGIN;
      r.y = channelOffsets[channelPosition] - topHeader;
      r.height = (channelNoteCounts[channelPosition] * getNoteHeight()) + topHeader + DETAILS_TOP_MARGIN; // The "TOP_MARGIN" is below the notes
    }
    return r;
  }

  private int getClosestChannel(int channel)
  {
    int channelPosition = reverseChannelMap[channel];
    if (channelPosition != -1)
    {
      return channelPosition;
    }
    for (int i = channel + 1; i < MidiConstants.MAX_CHANNELS; i++)
    {
      channelPosition = reverseChannelMap[i];
      if (channelPosition != -1)
      {
        return channelPosition;
      }
    }
    for (int i = channel - 1; i >= 0; i--)
    {
      channelPosition = reverseChannelMap[i];
      if (channelPosition != -1)
      {
        return channelPosition;
      }
    }
    return -1;
  }

  protected final int adjustY(int y)
  {
    y -= verticalOffset;
    return y;
  }

  protected int getHighestKey(Statistics statistics)
  {
    if (channelStyle == CHANNEL_COMMON_SCALE)
    {
      return overallHighestKey;
    }
    else
    {
      return statistics.getHighestKey();
    }
  }

  protected int getLowestKey(Statistics statistics)
  {
    if (channelStyle == CHANNEL_COMMON_SCALE)
    {
      return overallLowestKey;
    }
    else
    {
      return statistics.getLowestKey();
    }
  }

  public int getTicksPerPixel()
  {
    return ticksPerPixel;
  }

  public final int getTicks(int x)
  {
    return (int)firstTick + (x * ticksPerPixel);
  }

  public Point getTicks(Point coordinates)
  {
    return new Point(getTicks(coordinates.x), coordinates.y + verticalOffset);
  }

  public void addNotatorListener(NotatorListener listener)
  {
    listeners.add(listener);
  }

  public void removeNotatorListener(NotatorListener listener)
  {
    listeners.remove(listener);
  }

  private void fireMove(int deltaTicks, int deltaKeys)
  {
    for (NotatorListener listener : listeners)
    {
      listener.fireMove(deltaTicks, deltaKeys);
    }
  }

  private void fireInstrumentChange(int channel, int instrument)
  {
    for (NotatorListener listener : listeners)
    {
      listener.fireInstrumentChange(channel, instrument);
    }
  }

  private void fireHover(String text)
  {
    for (NotatorListener listener : listeners)
    {
      listener.fireHover(text);
    }
  }

  private void fireNote(Note note)
  {
    for (NotatorListener listener : listeners)
    {
      listener.fireNote(note);
    }
  }

  public void scrollToVisible(Midi source)
  {
    long sourceFirstTick = source.getFirstTick();
    if (sourceFirstTick > lastTick)
    {
      // Scroll left and position beginning of source in middle of view
      setCurrentTick(sourceFirstTick, true);
    }
    else
    {
      long sourceLastTick = source.getLastTick();
      if (sourceLastTick < firstTick)
      {
        // Scroll right and position end of source in middle of view
        setCurrentTick(sourceLastTick, true);
      }
    }
  }

  public interface NotatorListener
  {
    public void fireMove(int deltaTicks, int deltaKeys);

    public void fireHover(String text);

    public void fireInstrumentChange(int channel, int instrument);

    public void fireNote(Note note);

  }

  public class MidiMouseListener implements MouseListener, MouseMotionListener
  {
    private Point referencePointInTicks;
    private boolean isMove;

    // The interaction between the selection, the mouse position
    // and the scroller is complex during a move operation. The move
    // operation is handled as a number of remove / add combinations.
    // Each one of these sets the current tick. If we allowed these
    // to be handled via scheduleUpdateCurrentTick, then the current
    // position would basically track the note as it moved, meaning
    // the note would not appear to move at all but would actually
    // stay in the center of the screen (and other notes would move
    // around it). In addition, we keep a local copy of the reference
    // point in ticks, which would get out of date unless we updated
    // it. Finally, when the mouse gets to the edge of the window and
    // autoscroll kicks in, the Scroller starts trying to set the
    // current tick as well. This is really the one we care about
    // during a move operation because it is designed to adjust the
    // current tick the minimum amount necessary to keep the mouse
    // in the window. We handle this by ignoring requests in
    // setCurrentTick during a move, and by providing a special method,
    // notifyMouseOutsideWindow, for use by the scroller.

    public void mouseClicked(MouseEvent e)
    {
      if (e.getClickCount() == 2)
      {
        int x = e.getPoint().x;
        int y = e.getPoint().y + verticalOffset;
        Note note = getNote(x, y);
        if (note != null)
        {
          fireNote(note);
        }
      }
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
      requestFocusInWindow();
      addMouseMotionListener(midiMouseListener);
      referencePointInTicks = getTicks(e.getPoint());
      Midi selection = selectionManager.getSelection();
      if (!e.isControlDown() && selection != null && findNoteAt(selection.getMidels(), referencePointInTicks.getLocation()) != null)
      {
        isMove = true;
      }
    }

    public void mouseReleased(MouseEvent e)
    {
      if (isMove)
      {
        isMove = false;
        referencePointInTicks = null;
        return;
      }

      // See if this results in single note selection...
      if (selectionRectangleInTicks == null && referencePointInTicks != null)
      {
        selectionRectangleInTicks = expandPoint(referencePointInTicks);
      }

      if (referencePointInTicks != null)
      {
        removeMouseMotionListener(midiMouseListener);
        referencePointInTicks = null;
      }

      if (selectionRectangleInTicks != null)
      {
        selectNotes(e.isControlDown());
        selectionRectangleInTicks = null;
        repaint();
      }

    }

    public void mouseDragged(MouseEvent e)
    {
      Point currentPointInTicks = getTicks(e.getPoint());
      int x = Math.min(referencePointInTicks.x, currentPointInTicks.x);
      int y = Math.min(referencePointInTicks.y, currentPointInTicks.y);

      if (isMove)
      {
        // Strictly speaking, move does not need to use the reference point at all
        // because each note in the selection constitutes a reference point. Wrong.
        // The mouse is easily dragged off the note, making it impossible to find
        // your reference point.
        int deltaTicks = currentPointInTicks.x - referencePointInTicks.x;
        int deltaKeys = (referencePointInTicks.y - currentPointInTicks.y) / getNoteHeight();
        if (deltaTicks != 0 || deltaKeys != 0)
        {
          fireMove(deltaTicks, deltaKeys);
          updateSelectionHoverText(selectionManager.getSelection());
          if (deltaTicks != 0)
          {
            referencePointInTicks.x = currentPointInTicks.x;
          }
          if (deltaKeys != 0)
          {
            referencePointInTicks.y = currentPointInTicks.y;
          }
        }
        return;
      }

      int width = Math.abs(currentPointInTicks.x - referencePointInTicks.x);
      int height = Math.abs(currentPointInTicks.y - referencePointInTicks.y);

      selectionRectangleInTicks = new Rectangle(x, y, width, height);
      repaint();
    }

    public void mouseMoved(MouseEvent e)
    {
    }

  }

  private class NotatorMidiListener implements Midi.MidiListener
  {

    public void onAddMidel(Midi midi, Midel midel)
    {
      scheduleUpdateMidiDependencies();
      setCurrentTick(currentTick, true);
    }

    public void onRemoveMidel(Midi midi, Midel midel)
    {
      scheduleUpdateMidiDependencies();
      setCurrentTick(currentTick, true);
    }

  }

  public class MidiComponentListener implements ComponentListener
  {

    public void componentHidden(ComponentEvent e)
    {
    }

    public void componentMoved(ComponentEvent e)
    {
    }

    public void componentResized(ComponentEvent e)
    {
      setTicksPerLine();
      setCurrentTick(currentTick, true);
    }

    public void componentShown(ComponentEvent e)
    {
    }

  }

  public class SelectionListener implements Listener
  {

    public void notify(Object event, Object source)
    {
      if (source != Notator.this)
      {
        Midi selection = (Midi)event;
        if (selection != null && selection.size() > 0)
        {
          scrollToVisible(selection);
        }
        repaint();
      }
    }

  }

}
