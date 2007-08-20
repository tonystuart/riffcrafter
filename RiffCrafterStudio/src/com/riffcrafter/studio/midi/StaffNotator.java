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
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.ImageIcon;

import com.riffcrafter.common.midi.ChannelEvent;
import com.riffcrafter.common.midi.Instruments;
import com.riffcrafter.common.midi.MetaEvent;
import com.riffcrafter.common.midi.Midel;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.common.midi.MidiConstants;
import com.riffcrafter.common.midi.Note;
import com.riffcrafter.common.midi.SysexEvent;
import com.riffcrafter.common.midi.TickEventMap;
import com.riffcrafter.common.midi.TickEventMap.TickEvent;
import com.riffcrafter.library.util.Resources;
import com.riffcrafter.studio.app.Editor.SelectionManager;

public class StaffNotator extends Notator
{
  private static final Color STAFF_COLOR = Color.LIGHT_GRAY;

  public static final int TICKS_PER_PIXEL = 5;

  private static final int NOTE_HEIGHT = 7;
  private static final int NOTE_WIDTH = 9;
  private static final int STEM_HEIGHT = 25;
  private static final int MAXIMUM_RISE = 5;

  private static final int TREBLE_LEDGER_LINES = 5;
  private static final int TREBLE_CLEF_LINES = 5;
  private static final int INTERCLEF_LINES = 5;
  private static final int BASS_CLEF_LINES = 5;
  private static final int BASS_LEDGER_LINES = 5;
  private static final int MIDDLE_C_LINES = 1;

  private static final int MINIMUM_KEY = 24;
  private static final int MAXIMUM_KEY = 108;

  private static final int TICK_ROUNDING_FACTOR = 100; // 1000 / TICKS_PER_PIXEL;

  private static final int STAFF_LINES = TREBLE_LEDGER_LINES + TREBLE_CLEF_LINES + INTERCLEF_LINES + BASS_CLEF_LINES + BASS_LEDGER_LINES;
  private static final int STAFF_HEIGHT = STAFF_LINES * NOTE_HEIGHT;

  private static final double OCTAVE_LINES = 3.5;

  private static final BasicStroke NORMAL_STROKE = new BasicStroke(1);
  private static final BasicStroke WIDE_STROKE = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

  private MaxTicks maxTicks = new MaxTicks();

  private long ticksPerMeasure; // TODO: Add support for multiple Time Signatures per midi file

  public StaffNotator(Midi midi, SelectionManager selectionManager)
  {
    super(midi, selectionManager);
    setBackground(Color.WHITE);
    setForeground(Color.BLACK);
  }

  @Override
  public int getStaffHeight(int activeChannelMapIndex)
  {
    // Staff height if fixed, not dependent on channel
    return STAFF_HEIGHT;
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
    midiView = midi.getSet(firstTick - leftMargin, lastTick + leftMargin);
  }

  @Override
  protected int getDefaultTicksPerPixel()
  {
    return TICKS_PER_PIXEL;
  }

  // See http://en.wikipedia.org/wiki/Musical_staff

  protected void paintStaffs(Graphics2D g2d, int ticksPerMeasure, long firstMeasureTick, long lastMeasureTick)
  {
    g2d.setColor(STAFF_COLOR);

    for (int channelPosition = 0; channelPosition < activeChannelMap.length; channelPosition++)
    {
      int y = adjustY(getY(channelPosition, 0));

      y += TREBLE_LEDGER_LINES * NOTE_HEIGHT;

      int trebleTop = y;

      for (int j = 0; j < TREBLE_CLEF_LINES; j++)
      {
        g2d.drawLine(0, y, width, y);
        y += NOTE_HEIGHT;
      }

      y += INTERCLEF_LINES * NOTE_HEIGHT;

      for (int j = 0; j < BASS_CLEF_LINES; j++)
      {
        g2d.drawLine(0, y, width, y);
        y += NOTE_HEIGHT;
      }

      int bassBottom = y - NOTE_HEIGHT; // that last line we just added but never drew

      for (long j = firstMeasureTick; j <= lastMeasureTick; j += ticksPerMeasure)
      {
        int x = getX(j);
        g2d.drawLine(x, trebleTop, x, bassBottom);
      }
    }
  }

  @Override
  protected void paintTitles(Graphics2D g2d)
  {
    for (int i = 0; i < activeChannelMap.length; i++)
    {
      int channel = activeChannelMap[i];
      String title = channelTitles[channel];
      int yTitle = adjustY(getYTitle(channel));
      g2d.drawString(title, 5, yTitle);
    }
  }

  @Override
  protected void paintMidels(Graphics2D g2d, Iterable<Midel> midels, boolean isSelected, ArrayList<MetaEvent> lyrics)
  {
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    long lastMeasure = 0;
    MeasureNotes measureNotes = new MeasureNotes();
    ticksPerMeasure = midi.findTicksPerMeasure(0);
    maxTicks.clear();

    for (Midel midel : midels)
    {
      if (midel instanceof Note)
      {
        Note note = (Note)midel;
        long tick = note.getTick();
        long measure = tick / ticksPerMeasure;
        // TODO: Insert MeasureNotes cache here
        if (measure != lastMeasure)
        {
          long measureEndingTick = measure * ticksPerMeasure;
          measureNotes.paint(g2d, isSelected, measureEndingTick);
          lastMeasure = measure;
          measureNotes.clear();
        }
        if (note.getDuration() > 0)
        {
          measureNotes.add(note);
        }
      }
      else
      {
        paint(g2d, midel, isSelected, lyrics);
      }
    }

    measureNotes.paint(g2d, isSelected, (lastMeasure + 1) * ticksPerMeasure);

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
  }

  private void paint(Graphics2D g2d, Midel midel, boolean isSelected, ArrayList<MetaEvent> lyrics)
  {
    if (midel instanceof ChannelEvent)
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

  //  C      C#    D      D#    E      F      F#    G      G#    A      A#    B

  private boolean isSharp[] = new boolean[] { false, true, false, true, false, false, true, false, true, false, true, false };

  //  c   (c#)
  // -b-         3.0
  //  a   (a#)   2.5 and 2.5
  // -g-  (g#)   2.0 and 2.0
  //  f   (f#)   1.5 and 1.5
  // -e-         1.0
  //  d   (d#)   0.5 and 0.5
  // -c-  (c#)   0.0 and 0.0

  private static double[] keyLineOffset = new double[] { 0.0, 0.0, 0.5, 0.5, 1.0, 1.5, 1.5, 2.0, 2.0, 2.5, 2.5, 3 };

  // Ledger lines above treble clef are indexed bottom-up

  //  g   (g#)
  // -f-  (f#)
  //  e
  // -d-  (d#)
  //  c   (c#)
  // -b-
  //  a   (a#)
  // -g-  (g#)
  //  f   (f#)
  // -e-
  //  d   (d#)
  // -c-  (c#)
  //  b
  // -a-  (a#)  first ledger line above treble clef

  private static int[] upperLedgerLineCounts = new int[] { 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 7, 7 };

  // Ledger lines below base clef are indexed top-down

  // -e-        first ledger line below bass clef
  //  d   (d#)
  // -c-  (c#)
  //  b
  // -a-  (a#)
  //  g   (g#)
  // -f-  (f#)
  //  e
  // -d-  (d#)
  //  c   (c#)
  // -b-
  //  a   (a#)
  // -g-  (g#)
  //  f   (f#)

  private static int[] lowerLedgerLineCounts = new int[] { 1, 1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 7, 7, 7, 7 };

  public Point getStemEndPoint(Note note, long roundedTick, boolean isStemUp)
  {
    int x = getX(roundedTick);
    int y = adjustY(getY(note));

    Point stemEndPoint = new Point();
    if (isStemUp)
    {
      stemEndPoint.x = x + NOTE_WIDTH;
      stemEndPoint.y = y - STEM_HEIGHT;
    }
    else
    {
      stemEndPoint.x = x;
      stemEndPoint.y = y + STEM_HEIGHT;
    }
    return stemEndPoint;
  }

  private void paintNote(Graphics2D g2d, Note note, long roundedTick, long roundedDuration, boolean isStemUp, boolean isSelected, Point firstStemEndPoint, Point lastStemEndPoint, Note firstAccidentals[])
  {
    int key = getRangeBoundedKey(note);
    int channel = note.getChannel();

    int x = getX(roundedTick);
    int y = adjustY(getY(note));

    paintLedgerLines(g2d, key, channel, x);

    Point stemEndPoint = new Point();
    if (isStemUp)
    {
      stemEndPoint.x = x + NOTE_WIDTH;
      stemEndPoint.y = y - STEM_HEIGHT;
    }
    else
    {
      stemEndPoint.x = x;
      stemEndPoint.y = y + STEM_HEIGHT;
    }

    if (firstStemEndPoint != null)
    {
      stemEndPoint.y = findIntersection(stemEndPoint.x, firstStemEndPoint, lastStemEndPoint);
    }

    g2d.setColor(isSelected ? Color.RED : Color.BLACK);

    int noteIndex = key % MidiConstants.SEMITONES_PER_OCTAVE;
    if (firstAccidentals[noteIndex] == note)
    {
      g2d.drawString("#", x - NOTE_WIDTH, y + NOTE_HEIGHT + 1);
    }

    if (roundedDuration < MidiConstants.DURATION_HALF_NOTE)
    {
      g2d.fillOval(x, y, NOTE_WIDTH, NOTE_HEIGHT);
    }
    else
    {
      g2d.drawOval(x, y, NOTE_WIDTH, NOTE_HEIGHT);
      g2d.drawOval(x, y + 1, NOTE_WIDTH, NOTE_HEIGHT - 2);
    }

    if (roundedDuration < MidiConstants.DURATION_WHOLE_NOTE)
    {
      g2d.drawLine(stemEndPoint.x, y + NOTE_HEIGHT / 2, stemEndPoint.x, stemEndPoint.y);
    }

    if (roundedDuration == MidiConstants.DURATION_DOTTED_QUARTER_NOTE || roundedDuration == MidiConstants.DURATION_DOTTED_HALF_NOTE)
    {
      g2d.fillOval(x + NOTE_WIDTH + 3, y + (NOTE_HEIGHT / 2), 2, 2);
    }

    if (note instanceof TiedNote)
    {
      TiedNote tiedNote = (TiedNote)note;
      TiedNote previousNote = tiedNote.getPreviousNote();
      if (previousNote != null)
      {
        int previousNoteX = getX(previousNote.getRoundedTick());
        int previousNoteY = adjustY(getY(previousNote));
        int tieX = previousNoteX + NOTE_WIDTH;
        int tieY = previousNoteY;
        int tieWidth = x - tieX;
        tieY += NOTE_HEIGHT / 2;
        g2d.drawArc(tieX, tieY, tieWidth, NOTE_HEIGHT, 180, 180);
      }
    }
  }

  private int getRangeBoundedKey(Note note)
  {
    int key = note.getKey();

    if (key < MINIMUM_KEY)
    {
      key = MINIMUM_KEY;
    }
    else if (key > MAXIMUM_KEY)
    {
      key = MAXIMUM_KEY;
    }
    return key;
  }

  private void paintLedgerLines(Graphics2D g2d, int key, int channel, int x)
  {
    if (key >= 81)
    {
      g2d.setColor(STAFF_COLOR);
      int ledgerLineCount = upperLedgerLineCounts[(key - 81) % upperLedgerLineCounts.length];
      int ledgerY = adjustY(getY(reverseChannelMap[channel], 0)) + ((TREBLE_LEDGER_LINES - ledgerLineCount) * NOTE_HEIGHT);
      for (int i = 0; i < ledgerLineCount; i++)
      {
        g2d.drawLine(x - 2, ledgerY, x + NOTE_WIDTH + 2, ledgerY);
        ledgerY += NOTE_HEIGHT;
      }
    }
    else if (key <= 40)
    {
      g2d.setColor(STAFF_COLOR);
      int ledgerLineCount = lowerLedgerLineCounts[(40 - key) % lowerLedgerLineCounts.length];
      int ledgerY = adjustY(getY(reverseChannelMap[channel], 0)) + ((TREBLE_LEDGER_LINES + TREBLE_CLEF_LINES + INTERCLEF_LINES + BASS_CLEF_LINES) * NOTE_HEIGHT);
      for (int i = 0; i < ledgerLineCount; i++)
      {
        g2d.drawLine(x - 2, ledgerY, x + NOTE_WIDTH + 2, ledgerY);
        ledgerY += NOTE_HEIGHT;
      }
    }
    else if (key == 60)
    {
      g2d.setColor(STAFF_COLOR);
      int ledgerY = adjustY(getY(reverseChannelMap[channel], 0)) + ((TREBLE_LEDGER_LINES + TREBLE_CLEF_LINES) * NOTE_HEIGHT);
      g2d.drawLine(x - 2, ledgerY, x + NOTE_WIDTH + 2, ledgerY);
    }
  }

  // y = mx + b

  private int findIntersection(int x, Point p1, Point p2)
  {
    if (p1.x == p2.x)
    {
      return p1.y;
    }

    // slope (m) = delta-y / delta-x (rise / run)
    double m = ((double)p1.y - (double)p2.y) / ((double)p1.x - (double)p2.x);

    // using p1, solve for y-intercept (b) = y - mx
    double b = p1.y - m * p1.x;

    // now solve for y = mx + b
    int y = (int)(m * (double)x + b);

    return y;
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
      y = getYChannelKey(channel, key);
    }
    else
    {
      y = super.getY(midel);
    }
    return y;
  }

  private int getYChannelKey(int channel, int key)
  {
    int octave = key / MidiConstants.SEMITONES_PER_OCTAVE;
    int keyIndex = key % MidiConstants.SEMITONES_PER_OCTAVE;

    octave -= 2; // subtract midi octaves not on piano keyboard (see NoteName.java)
    if (octave < 0)
    {
      return -1;
    }

    double octaveOffset = octave * OCTAVE_LINES * NOTE_HEIGHT;
    double noteOffset = keyLineOffset[keyIndex] * NOTE_HEIGHT;

    if (key >= 60)
    {
      octaveOffset += (INTERCLEF_LINES) * NOTE_HEIGHT;
    }
    else
    {
      octaveOffset += NOTE_HEIGHT; // Not sure why this is necessary, perhaps because first key on piano keyboard is midi key 22, not 24.
    }

    int channelPosition = reverseChannelMap[channel];
    int y = getY(channelPosition, 0);
    y += STAFF_HEIGHT - (octaveOffset + noteOffset);
    return y;
  }

  @Override
  protected Color getColor(int channel)
  {
    return Color.BLACK;
  }

  @Override
  protected Color getMetaEventColor()
  {
    return Color.BLACK;
  }

  @Override
  protected Color getSysexEventColor()
  {
    return Color.BLACK;
  }

  @Override
  protected Note getNote(int x, int y)
  {
    int lastOffsetIndex = channelOffsets.length - 1;
    for (int channelPosition = 0; channelPosition < channelOffsets.length; channelPosition++)
    {
      if (y > channelOffsets[channelPosition] && (channelPosition == lastOffsetIndex || y < channelOffsets[channelPosition + 1]))
      {
        int channel = activeChannelMap[channelPosition];
        double deltaY = y - channelOffsets[channelPosition];

        if (deltaY > ((TREBLE_LEDGER_LINES + TREBLE_CLEF_LINES + INTERCLEF_LINES) * NOTE_HEIGHT))
        {
          deltaY -= (INTERCLEF_LINES - MIDDLE_C_LINES) * NOTE_HEIGHT;
        }
        else if (deltaY > ((TREBLE_LEDGER_LINES + TREBLE_CLEF_LINES + MIDDLE_C_LINES) * NOTE_HEIGHT))
        {
          deltaY = (TREBLE_LEDGER_LINES + TREBLE_CLEF_LINES + MIDDLE_C_LINES - .25) * NOTE_HEIGHT;
        }
        double lines = deltaY / getNoteHeight();
        double deltaKey = (lines / OCTAVE_LINES) * MidiConstants.SEMITONES_PER_OCTAVE;
        int key = (int)(MAXIMUM_KEY - MidiConstants.SEMITONES_PER_OCTAVE - deltaKey);
        if (isSharp[key%MidiConstants.SEMITONES_PER_OCTAVE])
        {
          key--;
        }
        int tick = getTicks(x);
        Note note = new Note(channel, key, 64, tick, 250);
        return note;
      }
    }
    return null;
  }

  private int getRoundedDuration(long duration)
  {
    int roundedDuration;

    if (duration == 0)
    {
      roundedDuration = 0;
    }
    else if (duration < 96)
    {
      roundedDuration = MidiConstants.DURATION_SIXTEENTH_NOTE;
    }
    else if (duration < 192)
    {
      roundedDuration = MidiConstants.DURATION_EIGHTH_NOTE;
    }
    else if (duration < 312)
    {
      roundedDuration = MidiConstants.DURATION_QUARTER_NOTE;
    }
    else if (duration < 437)
    {
      roundedDuration = MidiConstants.DURATION_DOTTED_QUARTER_NOTE;
    }
    else if (duration < 625)
    {
      roundedDuration = MidiConstants.DURATION_HALF_NOTE;
    }
    else if (duration < 875)
    {
      roundedDuration = MidiConstants.DURATION_DOTTED_HALF_NOTE;
    }
    else
    {
      roundedDuration = MidiConstants.DURATION_WHOLE_NOTE;
    }

    return roundedDuration;
  }

  private class MeasureNotes
  {
    private ChannelNotes[] channelNotes;

    private MeasureNotes()
    {
      channelNotes = new ChannelNotes[MidiConstants.MAX_CHANNELS];
    }

    public void paint(Graphics2D g2d, boolean isSelected, long measureEndingTick)
    {
      adjustContext(measureEndingTick);
      for (ChannelNotes notes : channelNotes)
      {
        if (notes != null)
        {
          notes.paint(g2d, isSelected);
        }
      }
    }

    public void adjustContext(long measureEndingTick)
    {
      for (ChannelNotes notes : channelNotes)
      {
        if (notes != null)
        {
          notes.adjustContext(measureEndingTick);
        }
      }
    }

    public void add(Note note)
    {
      int channel = note.getChannel();
      if (channelNotes[channel] == null)
      {
        channelNotes[channel] = createChannelNotes(channel);
      }
      channelNotes[channel].add(note);
    }

    private ChannelNotes createChannelNotes(int channel)
    {
      ChannelNotes channelNotes;
      if (channel == Instruments.DRUM_CHANNEL)
      {
        channelNotes = new DrumChannelNotes();
      }
      else
      {
        channelNotes = new InstrumentChannelNotes(channel);
      }
      return channelNotes;
    }

    private void clear()
    {
      for (int i = 0; i < channelNotes.length; i++)
      {
        channelNotes[i] = null;
      }
    }
  }

  interface ChannelNotes
  {

    void add(Note note);

    void adjustContext(long measureEndingTick);

    void paint(Graphics2D g2d, boolean isSelected);

  }

  private class DrumChannelNotes implements ChannelNotes
  {
    private ArrayList<Note> notes = new ArrayList<Note>();

    public void add(Note note)
    {
      notes.add(note);
    }

    public void adjustContext(long measureEndingTick)
    {
    }

    public void paint(Graphics2D g2d, boolean isSelected)
    {
      for (Note note : notes)
      {
        paint(g2d, note, isSelected);
      }
    }

    private void paint(Graphics2D g2d, Note note, boolean isSelected)
    {
      long tick = note.getTick();
      int key = getRangeBoundedKey(note);
      int channel = note.getChannel();

      int x = getX(tick);
      int y = adjustY(getY(note));

      paintLedgerLines(g2d, key, channel, x);

      g2d.setColor(isSelected ? Color.RED : Color.BLACK);

      g2d.drawLine(x, y, x + NOTE_WIDTH, y + NOTE_HEIGHT);
      g2d.drawLine(x + NOTE_WIDTH, y, x, y + NOTE_HEIGHT);
    }
  }

  private class InstrumentChannelNotes implements ChannelNotes
  {
    private ClefNotes bassClef;
    private ClefNotes trebleClef;

    private InstrumentChannelNotes(int channel)
    {
      bassClef = new ClefNotes(channel, false);
      trebleClef = new ClefNotes(channel, true);
    }

    public void add(Note note)
    {
      int key = note.getKey();
      if (key >= 60)
      {
        trebleClef.add(note);
      }
      else
      {
        bassClef.add(note);
      }
    }

    public void adjustContext(long measureEndingTick)
    {
      bassClef.adjustContext(49, 60, false, measureEndingTick);
      trebleClef.adjustContext(59, 71, true, measureEndingTick);
    }

    public void paint(Graphics2D g2d, boolean isSelected)
    {
      bassClef.paint(g2d, isSelected);
      trebleClef.paint(g2d, isSelected);
    }

  }

  private class TickMap extends TreeMap<Long, TickGroup>
  {
    private long tickRoundingFactor;

    private TickMap(long tickRoundingFactor)
    {
      this.tickRoundingFactor = tickRoundingFactor;
    }

    private TickGroup getFuzzy(Long tick)
    {
      TickGroup tickGroup = null;
      long floorDistance = Long.MAX_VALUE;
      Long floorTick = floorKey(tick);
      if (floorTick != null)
      {
        floorDistance = tick - floorTick;
      }
      if (floorDistance == 0)
      {
        tickGroup = get(floorTick);
      }
      else
      {
        long ceilingDistance = Long.MAX_VALUE;
        Long ceilingTick = ceilingKey(tick);
        if (ceilingTick != null)
        {
          ceilingDistance = ceilingTick - tick;
        }
        if (floorDistance < ceilingDistance && floorDistance < tickRoundingFactor)
        {
          tickGroup = get(floorTick);
        }
        else if (ceilingDistance < floorDistance && ceilingDistance < tickRoundingFactor)
        {
          tickGroup = get(ceilingTick);
        }
      }
      return tickGroup;
    }
  }

  private class ClefNotes
  {
    private TickEventMap tickEventMap = new TickEventMap(TICK_ROUNDING_FACTOR);
    private TickMap tickMap = new TickMap(MidiConstants.DURATION_SIXTEENTH_NOTE);//TICK_ROUNDING_FACTOR);
    private ArrayList<DurationGroup> durationGroups = new ArrayList<DurationGroup>();
    private Note firstAccidentals[] = new Note[MidiConstants.SEMITONES_PER_OCTAVE];
    private Rests rests = new Rests();
    private long lastInterMeasureTiedTick;
    private int channel;
    private boolean isTrebleClef;

    public ClefNotes(int channel, boolean isTrebleClef)
    {
      this.channel = channel;
      this.isTrebleClef = isTrebleClef;
      lastInterMeasureTiedTick = maxTicks.getMaxTick(channel, isTrebleClef);
    }

    public void add(Note note)
    {
      tickEventMap.add(note);
    }

    public void addTickMap(Note note)
    {
      addTickMap(note, 0);
    }

    public void addTickMap(Note note, int depth)
    {
      long tick = note.getTick();
      TickGroup tickGroup = tickMap.getFuzzy(tick);
      if (tickGroup == null)
      {
        tickGroup = new TickGroup(tick);
        tickMap.put(tick, tickGroup);
      }
      else
      {
        // If multiple notes start on the same tick, they must have approximately the same duration. If not, split them into tied notes
        long overflow = note.getDuration() - tickGroup.getDuration();
        if (overflow >= MidiConstants.DURATION_SIXTEENTH_NOTE)
        {
          long splitTick = note.getTick() + tickGroup.getDuration();
          TiedNotePair tiedNotePair = splitNote(splitTick, note);
          note = tiedNotePair.getFirst();
          // Limit recursion for channels (e.g. in Hawaii-5-0.mid) where every note overlaps
          if (++depth < 5)
          {
            addTickMap(tiedNotePair.getLast(), depth);
          }
        }
      }
      tickGroup.add(note);
      if (note instanceof TiedNote)
      {
        ((TiedNote)note).setRoundedTick(tickGroup.getTick());
      }
    }

    private boolean isAfterRest(TickGroup tickGroup)
    {
      // TODO: Evaluate performance impact of this approach
      Rest rest = rests.getPrecedingRest(tickGroup);
      return rest != null && getFollowingTickGroup(rest) == tickGroup;
    }

    private TickGroup getFollowingTickGroup(Rest rest)
    {
      TickGroup tickGroup = null;
      Entry<Long, TickGroup> entry = tickMap.higherEntry(rest.tick);
      if (entry != null)
      {
        tickGroup = entry.getValue();
      }
      return tickGroup;
    }

    public void adjustContext(int lowestKey, int highestKey, boolean isStemUp, long measureEndingTick)
    {
      createTiedNotes(measureEndingTick);
      insertRests(measureEndingTick);

      for (Entry<Long, TickGroup> entry : tickMap.entrySet())
      {
        TickGroup tickGroup = entry.getValue();
        tickGroup.adjustContext(lowestKey, highestKey, isStemUp, firstAccidentals);
      }

      DurationGroup durationGroup = null;

      for (Entry<Long, TickGroup> entry : tickMap.entrySet())
      {
        TickGroup tickGroup = entry.getValue();
        long roundedDuration = tickGroup.getRoundedDuration();
        if ((durationGroup == null || durationGroup.getDuration() != roundedDuration) || roundedDuration > MidiConstants.DURATION_EIGHTH_NOTE || isAfterRest(tickGroup))
        {
          boolean isDurationGroupStemUp = tickGroup.isStemUp();
          durationGroup = new DurationGroup(roundedDuration, isDurationGroupStemUp);
          durationGroups.add(durationGroup);
        }
        tickGroup.setStemUp(durationGroup.isStemUp()); // duration stem overrides tick stem
        durationGroup.add(tickGroup);
      }
    }

    private void createTiedNotes(long measureEndingTick)
    {
      ArrayList<Note> activeNotes = new ArrayList<Note>();

      for (Map.Entry<Long, TickEvent> entry : tickEventMap.getTickEvents())
      {
        long thisTick = entry.getKey();
        TickEvent tickEvent = entry.getValue();
        for (Note note : tickEvent.getNoteOffIterable())
        {
          Note correspondingNote = findCorrespondingNote(activeNotes, note);
          activeNotes.remove(correspondingNote);
          if (isTiedToNextMeasure(correspondingNote, measureEndingTick))
          {
            TiedNotePair tiedNotePair = splitNote(measureEndingTick, note);
            addTickMap(tiedNotePair.getFirst());
            addTickMap(tiedNotePair.getLast());
          }
          else
          {
            addTickMap(correspondingNote);
          }
        }

        // At this point, some notes have either just ended and/or some notes
        // are just about to begin, so any notes that are on at this point need to
        // be split into two tied notes.

        int activeNoteCount = activeNotes.size();
        for (int i = 0; i < activeNoteCount; i++)
        {
          Note note = activeNotes.get(i);
          long overflow = note.getTick() + note.getDuration() - thisTick;
          // Only perform the split if the second note is of reasonable duration
          if (overflow >= MidiConstants.DURATION_SIXTEENTH_NOTE)
          {
            TiedNotePair tiedNotePair = splitNote(thisTick, note);
            addTickMap(tiedNotePair.getFirst());
            activeNotes.set(i, tiedNotePair.getLast());
          }
        }

        for (Note note : tickEvent.getNoteOnIterable())
        {
          activeNotes.add(note);
          //          Rest rest = getRest(note);
          //          if (rest != null)
          //          {
          //            rests.add(rest);
          //          }
          maxTicks.setMaxTick(note);
        }
      }
    }

    private void insertRests(long measureEndingTick)
    {
      long measureStartingTick = measureEndingTick - ticksPerMeasure;

      // TODO: Right now, we only handle empty measures if the other clef contains notes. Otherwise,
      // there is nothing to force the filling of the empty measure. We need something to trigger the
      // filling of the empty measure, and we may not have a note (especially for this particular tick view).

      // When an entire measure is devoid of notes, a semibreve (whole) rest is used, regardless of the actual time signature. See: http://en.wikipedia.org/wiki/Musical_rest
      if (tickMap.size() == 0)
      {
        Rest rest = new Rest(channel, measureStartingTick, MidiConstants.DURATION_WHOLE_NOTE, isTrebleClef);
        rests.add(rest);
        return;
      }

      long firstAllocatedTick;
      if (lastInterMeasureTiedTick > measureStartingTick)
      {
        firstAllocatedTick = measureStartingTick + getRoundedDuration(lastInterMeasureTiedTick - measureStartingTick);
      }
      else
      {
        firstAllocatedTick = measureStartingTick;
      }

      // First pass: see how many ticks worth of rests need to be inserted
      long totalNoteDuration = firstAllocatedTick - measureStartingTick;
      for (Entry<Long, TickGroup> entry : tickMap.entrySet())
      {
        TickGroup tickGroup = entry.getValue();
        // Note that notes that are tied into the next measure actually occur in TickGroups in the previous measure
        long tick = tickGroup.getTick();
        if (tick < measureEndingTick)
        {
          long roundedDuration = tickGroup.getRoundedDuration();
          totalNoteDuration += roundedDuration;
        }
      }

      long totalRestDuration = ticksPerMeasure - totalNoteDuration;

      // Second pass: allocate the rests to the gaps using a best fit approach
      long lastTick = firstAllocatedTick;
      for (Entry<Long, TickGroup> entry : tickMap.entrySet())
      {
        TickGroup tickGroup = entry.getValue();
        long tick = tickGroup.getTick();
        if (tick < measureEndingTick)
        {
          long gap = tick - lastTick;
          if (gap >= MidiConstants.DURATION_EIGHTH_NOTE)
          {
            totalRestDuration = insertBestFittingRest(totalRestDuration, lastTick, gap);
          }
          long roundedDuration = tickGroup.getRoundedDuration();
          lastTick = tick + roundedDuration;
        }
      }

      // See if we need a rest at the end of the measure
      totalRestDuration = insertBestFittingRest(totalRestDuration, lastTick, measureEndingTick - lastTick);

      //      if (totalRestDuration >= MidiConstants.DURATION_THIRTYSECOND_NOTE)
      //      {
      //        System.out.println("StaffNotator.insertRests: channel=" + channel + ", isTrebleClef=" + isTrebleClef + ", measure=" + measureStartingTick + ", leftover totalRestDuration=" + totalRestDuration);
      //      }

    }

    private long insertBestFittingRest(long totalRestDuration, long tick, long gap)
    {
      long bestFit = findBestFit(gap);

      if (bestFit != 0)
      {
        Rest rest = new Rest(channel, tick, bestFit, isTrebleClef);
        rests.add(rest);
        totalRestDuration -= bestFit;
        gap -= bestFit;
        return insertBestFittingRest(totalRestDuration, tick + bestFit, gap);
      }

      return totalRestDuration;
    }

    //    private long findBestFit(long gap)
    //    {
    //      for (int i = MidiConstants.REST_DURATIONS.length - 1; i >= 0; i--)
    //      {
    //        if (gap >= MidiConstants.REST_DURATIONS[i])
    //        {
    //          return MidiConstants.REST_DURATIONS[i];
    //        }
    //      }
    //      return 0;
    //    }

    // TODO: Factor out common code with getRoundedDuration (e.g. a table of breaking points)

    private long findBestFit(long duration)
    {
      int roundedDuration = 0;

      if (duration >= 875)
      {
        roundedDuration = MidiConstants.DURATION_WHOLE_NOTE;
      }
      else if (duration >= 437)
      {
        roundedDuration = MidiConstants.DURATION_HALF_NOTE;
      }
      else if (duration >= 192)
      {
        roundedDuration = MidiConstants.DURATION_QUARTER_NOTE;
      }
      else if (duration >= 96)
      {
        roundedDuration = MidiConstants.DURATION_EIGHTH_NOTE;
      }

      return roundedDuration;
    }

    private boolean isTiedToNextMeasure(Note note, long measureEndingTick)
    {
      long tick = note.getTick();
      long duration = note.getDuration();
      long overflow = (tick + duration) - measureEndingTick;
      return overflow > MidiConstants.DURATION_THIRTYSECOND_NOTE;
    }

    private TiedNotePair splitNote(long thisTick, Note note)
    {
      TiedNote firstNote = new TiedNote(note);
      TiedNote lastNote = new TiedNote(note);

      long tick = note.getTick();
      long duration = note.getDuration();

      long firstDuration = thisTick - tick;
      firstNote.setDuration(firstDuration);

      long lastDuration = duration - firstDuration;
      lastNote.setDuration(lastDuration);
      lastNote.setTick(thisTick);

      if (note instanceof TiedNote)
      {
        TiedNote tiedNote = (TiedNote)note;
        TiedNote previousNote = tiedNote.getPreviousNote();
        if (previousNote != null)
        {
          previousNote.setNextNote(firstNote);
          firstNote.setPreviousNote(previousNote);
        }
        TiedNote nextNote = tiedNote.getNextNote();
        if (nextNote != null)
        {
          nextNote.setPreviousNote(lastNote);
          lastNote.setNextNote(nextNote);
        }
      }

      firstNote.setNextNote(lastNote);
      lastNote.setPreviousNote(firstNote);

      TiedNotePair tiedNotePair = new TiedNotePair(firstNote, lastNote);
      return tiedNotePair;
    }

    private Note findCorrespondingNote(ArrayList<Note> activeNotes, Note note)
    {
      for (Note thisNote : activeNotes)
      {
        if (thisNote == note)
        {
          return thisNote;
        }
        else if (thisNote instanceof TiedNote && ((TiedNote)thisNote).getBaseNote() == note)
        {
          return thisNote;
        }
      }
      return note;
    }

    public void paint(Graphics2D g2d, boolean isSelected)
    {
      for (DurationGroup durationGroup : durationGroups)
      {
        durationGroup.paint(g2d, isSelected, firstAccidentals);
      }
      for (Rest rest : rests)
      {
        rest.paint(g2d, isSelected);
      }
    }

  }

  private class TickGroup
  {
    private long tick;
    private boolean isStemUp;
    private long totalDuration;
    private long minimumDuration = Long.MAX_VALUE;
    private ArrayList<Note> notes = new ArrayList<Note>();

    public TickGroup(long tick)
    {
      this.tick = tick;
    }

    public long getRoundedDuration()
    {
      return StaffNotator.this.getRoundedDuration(getDuration());
    }

    public void add(Note note)
    {
      notes.add(note);
      long duration = note.getDuration();
      totalDuration += duration;
      minimumDuration = Math.min(minimumDuration, duration);
    }

    public long getDuration()
    {
      // TODO: Decide which approach is better
      // return totalDuration / notes.size();
      return minimumDuration;
    }

    public long getTick()
    {
      return tick;
    }

    private void adjustContext(int lowestKey, int highestKey, boolean isStemUp, Note[] firstAccidentals)
    {
      normalizeStems(lowestKey, highestKey, isStemUp);
      findAccidentals(firstAccidentals);
    }

    public void normalizeStems(int lowestKey, int highestKey, boolean isStemUp)
    {
      for (Note note : notes)
      {
        int key = note.getKey();
        if (lowestKey < key && key < highestKey)
        {
          // At least one note in this tick group is in special override area so make entire tick group that way
          this.isStemUp = isStemUp;
          return;
        }
      }

      // Use natural direction of first note in tick group
      this.isStemUp = isStemUp(notes.get(0).getKey());
    }

    private boolean isStemUp(int key)
    {
      boolean isStemUp;
      if (key >= 71)
      {
        isStemUp = false;
      }
      else if (key >= 60)
      {
        isStemUp = true;
      }
      else if (key >= 50)
      {
        isStemUp = false;
      }
      else
      {
        isStemUp = true;
      }
      return isStemUp;
    }

    private boolean isStemUp()
    {
      return isStemUp;
    }

    public void setStemUp(boolean isStemUp)
    {
      this.isStemUp = isStemUp;
    }

    public void findAccidentals(Note[] firstAccidentals)
    {
      for (Note note : notes)
      {
        int key = note.getKey();
        int noteIndex = key % MidiConstants.SEMITONES_PER_OCTAVE;
        if (isSharp[noteIndex] && firstAccidentals[noteIndex] == null)
        {
          firstAccidentals[noteIndex] = note;
        }
      }

    }

    public void paint(Graphics2D g2d, long duration, boolean isSelected, Point firstStemEndPoint, Point lastStemEndPoint, Note firstAccidentals[])
    {
      for (Note note : notes)
      {
        paintNote(g2d, note, tick, duration, isStemUp, isSelected, firstStemEndPoint, lastStemEndPoint, firstAccidentals);
      }
    }

    private Point getStemEndPoint()
    {
      Point minimumStemEndPoint = null;
      Point maximumStemEndPoint = null;

      for (Note note : notes)
      {
        Point stemEndPoint = StaffNotator.this.getStemEndPoint(note, tick, isStemUp);
        if (minimumStemEndPoint == null || stemEndPoint.y < minimumStemEndPoint.y)
        {
          minimumStemEndPoint = stemEndPoint;
        }
        if (maximumStemEndPoint == null || stemEndPoint.y > maximumStemEndPoint.y)
        {
          maximumStemEndPoint = stemEndPoint;
        }
      }

      return isStemUp ? minimumStemEndPoint : maximumStemEndPoint;
    }

  }

  private class DurationGroup
  {
    private long roundedDuration;
    private boolean isStemUp;
    private ArrayList<TickGroup> tickGroups = new ArrayList<TickGroup>();

    public DurationGroup(long roundedDuration, boolean isStemUp)
    {
      this.roundedDuration = roundedDuration;
      this.isStemUp = isStemUp;
    }

    public void paint(Graphics2D g2d, boolean isSelected, Note firstAccidentals[])
    {
      if (roundedDuration > MidiConstants.DURATION_EIGHTH_NOTE)
      {
        for (TickGroup tickGroup : tickGroups)
        {
          tickGroup.paint(g2d, roundedDuration, isSelected, null, null, firstAccidentals);
        }
      }
      else
      {
        Point firstStemEndPoint = new Point();
        Point lastStemEndPoint = new Point();
        getLimits(firstStemEndPoint, lastStemEndPoint);
        for (TickGroup tickGroup : tickGroups)
        {
          tickGroup.paint(g2d, roundedDuration, isSelected, firstStemEndPoint, lastStemEndPoint, firstAccidentals);
        }
        if (tickGroups.size() == 1)
        {
          paintFlag(g2d, firstStemEndPoint, isSelected);
        }
        else
        {
          paintConnectedFlags(g2d, firstStemEndPoint, lastStemEndPoint);
        }
      }
    }

    private void getLimits(Point firstStemEndPoint, Point lastStemEndPoint)
    {
      Point minimumStemEndPoint = null;
      Point maximumStemEndPoint = null;

      int lastIndex = tickGroups.size() - 1;

      for (int index = 0; index <= lastIndex; index++)
      {
        TickGroup tickGroup = tickGroups.get(index);
        Point stemEndPoint = tickGroup.getStemEndPoint();
        if (index == 0)
        {
          firstStemEndPoint.setLocation(stemEndPoint);
        }
        if (index == lastIndex)
        {
          lastStemEndPoint.setLocation(stemEndPoint);
        }
        if (minimumStemEndPoint == null || stemEndPoint.y < minimumStemEndPoint.y)
        {
          minimumStemEndPoint = stemEndPoint;
        }
        if (maximumStemEndPoint == null || stemEndPoint.y > maximumStemEndPoint.y)
        {
          maximumStemEndPoint = stemEndPoint;
        }
      }

      int rise = firstStemEndPoint.y - lastStemEndPoint.y; // first-last because y-origin is at top

      // TODO: Factor out common code

      if (isStemUp)
      {
        if (rise < -MAXIMUM_RISE)
        {
          lastStemEndPoint.y = firstStemEndPoint.y + MAXIMUM_RISE;
        }
        else if (rise > MAXIMUM_RISE)
        {
          firstStemEndPoint.y = lastStemEndPoint.y + MAXIMUM_RISE;
        }
        if (minimumStemEndPoint.y < firstStemEndPoint.y && minimumStemEndPoint.y < lastStemEndPoint.y)
        {
          // One of the middle notes is above the line between the first and last
          if (rise > 0)
          {
            firstStemEndPoint.y = minimumStemEndPoint.y + MAXIMUM_RISE;
            lastStemEndPoint.y = minimumStemEndPoint.y - MAXIMUM_RISE;
          }
          else if (rise < 0)
          {
            firstStemEndPoint.y = minimumStemEndPoint.y - MAXIMUM_RISE;
            lastStemEndPoint.y = minimumStemEndPoint.y + MAXIMUM_RISE;
          }
          else
          {
            firstStemEndPoint.y = minimumStemEndPoint.y;
            lastStemEndPoint.y = minimumStemEndPoint.y;
          }
        }
      }
      else
      {
        if (rise < -MAXIMUM_RISE)
        {
          int newY = lastStemEndPoint.y - MAXIMUM_RISE;
          firstStemEndPoint.y = newY;
        }
        else if (rise > MAXIMUM_RISE)
        {
          int newY = firstStemEndPoint.y - MAXIMUM_RISE;
          lastStemEndPoint.y = newY;
        }
        if (maximumStemEndPoint.y > firstStemEndPoint.y && maximumStemEndPoint.y > lastStemEndPoint.y)
        {
          // One of the middle notes is below the line between the first and last
          if (rise > 0)
          {
            firstStemEndPoint.y = maximumStemEndPoint.y + MAXIMUM_RISE;
            lastStemEndPoint.y = maximumStemEndPoint.y - MAXIMUM_RISE;
          }
          else if (rise < 0)
          {
            firstStemEndPoint.y = maximumStemEndPoint.y - MAXIMUM_RISE;
            lastStemEndPoint.y = maximumStemEndPoint.y + MAXIMUM_RISE;
          }
          else
          {
            firstStemEndPoint.y = maximumStemEndPoint.y;
            lastStemEndPoint.y = maximumStemEndPoint.y;
          }
        }
      }
    }

    private void paintFlag(Graphics2D g2d, Point stemEndPoint, boolean isSelected)
    {
      int x;
      int y;
      String direction;

      if (isStemUp)
      {
        x = stemEndPoint.x;
        y = stemEndPoint.y;
        direction = "Up";
      }
      else
      {
        x = stemEndPoint.x;
        y = stemEndPoint.y - 16;
        direction = "Down";
      }

      String selection;
      if (isSelected)
      {
        selection = "Selected";
      }
      else
      {
        selection = "Normal";
      }

      String imageName = "Flag-" + direction + "-" + selection + "-" + roundedDuration + "-6x18.png";
      ImageIcon image = Resources.getIconByFileName(imageName);

      g2d.drawImage(image.getImage(), x, y, null);
    }

    private void paintConnectedFlags(Graphics2D g2d, Point firstStemEndPoint, Point lastStemEndPoint)
    {
      int d = isStemUp ? 1 : -1; // shorthand for direction

      g2d.setStroke(WIDE_STROKE);
      g2d.drawLine(firstStemEndPoint.x, firstStemEndPoint.y, lastStemEndPoint.x, lastStemEndPoint.y);
      if (roundedDuration < MidiConstants.DURATION_EIGHTH_NOTE)
      {
        g2d.drawLine(firstStemEndPoint.x, firstStemEndPoint.y + 5 * d, lastStemEndPoint.x, lastStemEndPoint.y + 5 * d);
      }
      g2d.setStroke(NORMAL_STROKE);
    }

    public void add(TickGroup note)
    {
      tickGroups.add(note);
    }

    public long getDuration()
    {
      return roundedDuration;
    }

    private boolean isStemUp()
    {
      return isStemUp;
    }

  }

  private class TiedNote extends Note
  {
    private Note baseNote;
    private TiedNote previousNote;
    private TiedNote nextNote;
    private long roundedTick;

    public TiedNote(Note note)
    {
      super(note);

      if (note instanceof TiedNote)
      {
        TiedNote tiedNote = (TiedNote)note;
        baseNote = tiedNote.getBaseNote();
      }
      else
      {
        baseNote = note;
      }
      roundedTick = tick;
    }

    public void setRoundedTick(long roundedTick)
    {
      this.roundedTick = roundedTick;
    }

    public long getRoundedTick()
    {
      return roundedTick;
    }

    public void setNextNote(TiedNote nextNote)
    {
      this.nextNote = nextNote;
    }

    public TiedNote getNextNote()
    {
      return nextNote;
    }

    private Note getBaseNote()
    {
      return baseNote;
    }

    public void setPreviousNote(TiedNote previousNote)
    {
      this.previousNote = previousNote;
    }

    public TiedNote getPreviousNote()
    {
      return this.previousNote;
    }
  }

  private class TiedNotePair
  {
    TiedNote first;
    TiedNote last;

    public TiedNotePair(TiedNote first, TiedNote last)
    {
      super();
      this.first = first;
      this.last = last;
    }

    public TiedNote getFirst()
    {
      return first;
    }

    public TiedNote getLast()
    {
      return last;
    }

  }

  private class MaxTicks
  {
    private long maxTicks[] = new long[MidiConstants.MAX_CHANNELS * 2];

    public void setMaxTick(Note note)
    {
      long tick = note.getTick();
      long duration = note.getDuration();
      int index = getIndex(note);
      maxTicks[index] = Math.max(maxTicks[index], tick + duration);
    }

    public void clear()
    {
      for (int i = 0; i < maxTicks.length; i++)
      {
        maxTicks[i] = 0;
      }
    }

    public long getMaxTick(int channel, boolean isTrebleClef)
    {
      int index = getIndex(channel, isTrebleClef);
      return maxTicks[index];
    }

    public long getMaxTick(Note note)
    {
      int index = getIndex(note);
      return maxTicks[index];
    }

    private int getIndex(int channel, boolean isTrebleClef)
    {
      int index = channel;
      if (isTrebleClef)
      {
        index += MidiConstants.MAX_CHANNELS;
      }
      return index;
    }

    private int getIndex(Note note)
    {
      int channel = note.getChannel();
      int key = note.getKey();
      int index = channel;
      if (key >= 60)
      {
        index += MidiConstants.MAX_CHANNELS;
      }
      return index;
    }

  }

  private class Rest
  {
    private int channel;
    private long tick;
    private long roundedDuration;
    private boolean isTrebleClef;

    public Rest(int channel, long tick, long roundedDuration, boolean isTrebleClef)
    {
      this.tick = tick;
      this.channel = channel;
      this.isTrebleClef = isTrebleClef;
      this.roundedDuration = roundedDuration;
    }

    public void paint(Graphics2D g2d, boolean isSelected)
    {
      if (isSelected)
      {
        return;
      }

      int key = isTrebleClef ? 69 : 49;
      int x = getX(tick);
      int y = adjustY(getYChannelKey(channel, key));
      String imageName = null;
      switch ((int)roundedDuration)
      {
        case MidiConstants.DURATION_SIXTEENTH_NOTE:
          imageName = "Rest-16-10x16.png";
          break;
        case MidiConstants.DURATION_EIGHTH_NOTE:
          imageName = "Rest-08-10x16.png";
          break;
        case MidiConstants.DURATION_QUARTER_NOTE:
          imageName = "Rest-04-10x16.png";
          break;
        case MidiConstants.DURATION_HALF_NOTE:
          imageName = "Rest-02-10x16.png";
          break;
        case MidiConstants.DURATION_WHOLE_NOTE:
          imageName = "Rest-01-10x16.png";
          break;
      }
      if (imageName != null)
      {
        ImageIcon image = Resources.getIconByFileName(imageName);
        g2d.drawImage(image.getImage(), x, y - (image.getIconHeight() / 2), null);
      }
    }

  }

  private class Rests implements Iterable<Rest>
  {
    private TreeMap<Long, Rest> restMap = new TreeMap<Long, Rest>();

    public void add(Rest rest)
    {
      restMap.put(rest.tick, rest);
    }

    public Rest getPrecedingRest(TickGroup tickGroup)
    {
      Rest rest = null;
      Entry<Long, Rest> entry = restMap.floorEntry(tickGroup.getTick());
      if (entry != null)
      {
        rest = entry.getValue();
      }
      return rest;
    }

    public Iterator<Rest> iterator()
    {
      return restMap.values().iterator();
    }

  }

}
