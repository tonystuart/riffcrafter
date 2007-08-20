// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.swing.tree.DefaultTreeModel;

import com.riffcrafter.common.midi.ProgramChange.ProgramChangeComparator;
import com.riffcrafter.common.thirdparty.Base64;
import com.riffcrafter.common.thirdparty.Base64.OutputStream;
import com.riffcrafter.library.util.Navigator;

public class Midi
{
  public static final int DEFAULT_TEMPO_IN_BPM = 120; // quarter notes per minute
  public static final int DEFAULT_RESOLUTION = 250; // ticks per quarter note
  private static final int DEFAULT_BEATS_PER_MEASURE = 4;

  private int lyricType;
  private int ticksPerLetter;
  private boolean isCurrentVersion;

  private TreeSet<Midel> midels = new TreeSet<Midel>();
  private TreeSet<ProgramChange> programChanges = new TreeSet<ProgramChange>(new ProgramChangeComparator());
  private TreeSet<TempoChange> tempoChanges = new TreeSet<TempoChange>();
  private TreeSet<TimeSignatureChange> timeSignatureChanges = new TreeSet<TimeSignatureChange>();
  private StatisticsManager statisticsManager = new StatisticsManager();
  private ArrayList<MidiListener> midiListeners = new ArrayList<MidiListener>();

  public Midi()
  {
  }

  public Midi(String fileName)
  {
    File file = new File(fileName);
    Sequence sequence = Catcher.getSequence(file);
    add(sequence);
  }

  public Midi(Sequence sequence)
  {
    add(sequence);
  }

  public Midi(InputStream inputStream)
  {
    Sequence sequence = Catcher.getSequence(inputStream);
    add(sequence);
  }

  public Midi copy()
  {
    Midi midi = new Midi();
    for (Midel midel : midels)
    {
      midi.add(midel);
    }
    return midi;
  }

  private Midi copyChannel(int channel)
  {
    Midi midi = new Midi();
    for (Midel midel : midels)
    {
      if (midel.getChannel() == channel)
      {
        midi.add(midel);
      }
    }
    return midi;
  }

  public void add(Midi midi)
  {
    add(0, midi);
  }

  public void add(long tick, Midi midi)
  {
    add(midi, 0, tick);
  }

  public void add(Midi sourceMidi, long minTick, long currentTick)
  {
    add(sourceMidi, minTick, currentTick, null);
  }

  // TODO: For performance reasons, this should generate a single undo/redo event

  public void add(Midi sourceMidi, long minTick, long currentTick, Midi targetMidi)
  {
    for (Midel midel : sourceMidi.midels)
    {
      Midel newMidel = midel.clone();
      newMidel.setTick(midel.getTick() - minTick + currentTick);
      add(newMidel);
      if (targetMidi != null)
      {
        targetMidi.add(newMidel);
      }
    }
    this.ticksPerLetter = Math.max(this.ticksPerLetter, sourceMidi.ticksPerLetter);
  }

  public void add(Sequence sequence)
  {
    int resolution = sequence.getResolution();
    if (resolution > DEFAULT_RESOLUTION)
    {
      System.out.println("MIDI file resolution higher than default resolution");
    }

    int minAssignedChannel = Midel.DEFAULT_CHANNEL;
    Track[] tracks = sequence.getTracks();

    // We process the tracks backward so the meta midels at the beginning
    // of the sequence are assigned the lowest channel numbers.

    for (int i = tracks.length - 1; i >= 0; i--)
    {
      Track track = tracks[i];
      NoteBuilder noteBuilder = new NoteBuilder(resolution);

      int eventCount = track.size();
      for (int j = 0; j < eventCount; j++)
      {
        MidiEvent event = track.get(j);
        noteBuilder.processMessage(event.getMessage(), event.getTick());
      }

      int ticksPerLetter = noteBuilder.getTicksPerLetter();
      if (ticksPerLetter > 0)
      {
        this.ticksPerLetter = Math.max(this.ticksPerLetter, ticksPerLetter);
      }

      ArrayList<Midel> metaMidels = noteBuilder.getMetaMidels();
      if (metaMidels != null && (metaMidels.size() > 0))
      {
        int channel = noteBuilder.getChannel();
        if (channel == Midel.DEFAULT_CHANNEL)
        {
          channel = --minAssignedChannel;
        }
        for (Midel midel : metaMidels)
        {
          midel.setChannel(channel);
          add(midel);
        }
      }
    }

  }

  public void add(ArrayList<Midel> midels)
  {
    for (Midel midel : midels)
    {
      add(midel);
    }
  }

  public boolean add(Midel midel)
  {
    midel.setReadOnly();
    boolean isAdd = midels.add(midel);
    if (isAdd)
    {
      recordAddOperation(midel);
    }
    return isAdd;
  }

  private void recordAddOperation(Midel midel)
  {
    if (midel instanceof Note)
    {
      int channel = midel.getChannel();
      statisticsManager.add(channel, (Note)midel);
    }
    else if (midel instanceof ProgramChange)
    {
      ProgramChange programChange = (ProgramChange)midel;
      programChanges.add(programChange);
    }
    else if (midel instanceof TempoChange)
    {
      TempoChange tempoChange = (TempoChange)midel;
      tempoChanges.add(tempoChange);
    }
    else if (midel instanceof TimeSignatureChange)
    {
      TimeSignatureChange timeSignatureChange = (TimeSignatureChange)midel;
      timeSignatureChanges.add(timeSignatureChange);
    }

    fireAdd(midel);
  }

  public boolean remove(Midel midel)
  {
    boolean isRemove = midels.remove(midel);
    if (isRemove)
    {
      recordRemoveOperation(midel);
    }
    return isRemove;
  }

  protected void recordRemoveOperation(Midel midel)
  {
    if (midel instanceof Note)
    {
      int channel = midel.getChannel();
      statisticsManager.remove(channel, (Note)midel, this);
    }
    else if (midel instanceof ProgramChange)
    {
      ProgramChange programChange = (ProgramChange)midel;
      programChanges.remove(programChange);
    }
    else if (midel instanceof TempoChange)
    {
      TempoChange tempoChange = (TempoChange)midel;
      tempoChanges.remove(tempoChange);
    }
    else if (midel instanceof TimeSignatureChange)
    {
      TimeSignatureChange timeSignatureChange = (TimeSignatureChange)midel;
      timeSignatureChanges.remove(timeSignatureChange);
    }
    fireRemove(midel);
  }

  public void remove(Midi thatMidi)
  {
    for (Midel midel : thatMidi.midels)
    {
      remove(midel);
    }
  }

  public Midi move(int deltaTicks, int deltaKeys)
  {
    Midi newMidi = new Midi();

    for (Midel midel : midels)
    {
      long tick = midel.getTick();
      tick += deltaTicks;
      if (tick < 0)
      {
        return null;
      }

      Midel newMidel = midel.clone();
      newMidel.setTick(tick);

      if (newMidel instanceof Note)
      {
        Note newNote = (Note)newMidel;
        int key = newNote.getKey();
        key += deltaKeys;
        if (key < 0 || key >= MidiConstants.MAX_MIDI_KEYS)
        {
          return null;
        }
        newNote.setKey(key);
      }

      newMidi.add(newMidel);
    }

    return newMidi;
  }

  public Midi normalizeFirstTick(long tick)
  {
    long minTick = getFirstTick();
    long deltaTicks = tick - minTick;
    if (deltaTicks == 0)
    {
      return this;
    }
    Midi newMidi = modifyTicks(deltaTicks);
    return newMidi;
  }

  public void modifyTicks(long startingTick, long deltaTicks)
  {
    Iterator<Midel> pendingMidels = getIterator(startingTick);
    ArrayList<Midel> movedMidels = new ArrayList<Midel>();

    long firstTickToMove = startingTick;

    if (deltaTicks < 0)
    {
      firstTickToMove -= deltaTicks; // minus a minus is a plus
    }

    while (pendingMidels.hasNext())
    {
      Midel midel = pendingMidels.next();
      long midelTick = midel.getTick();
      if (midelTick >= firstTickToMove)
      {
        midel = midel.clone();
        midelTick += deltaTicks;
        midel.setTick(midelTick);
        movedMidels.add(midel);
      }
      pendingMidels.remove();
    }

    add(movedMidels);
  }

  public Midi modifyTicks(long deltaTicks)
  {
    Midi newMidi = new Midi();

    for (Midel midel : midels)
    {
      long tick = midel.getTick();
      tick += deltaTicks;
      if (tick < 0)
      {
        return null;
      }
      Midel newMidel = midel.clone();
      newMidel.setTick(tick);
      newMidi.add(newMidel);
    }

    return newMidi;
  }

  public Midi modifyDuration(long deltaDuration)
  {
    Midi newMidi = new Midi();

    for (Midel midel : midels)
    {
      Midel newMidel = midel.clone();
      if (midel instanceof Note)
      {
        Note newNote = (Note)newMidel;
        long duration = newNote.getDuration();
        duration += deltaDuration;
        if (duration < 0)
        {
          return null;
        }
        newNote.setDuration(duration);
      }
      newMidi.add(newMidel);
    }

    return newMidi;
  }

  public Midi modifyChannel(long deltaChannel)
  {
    Midi newMidi = new Midi();

    for (Midel midel : midels)
    {
      int originalChannel = midel.getChannel();
      int channel = originalChannel + (int)deltaChannel;
      // If the original channel was in range (i.e. not a special control channel, like -100), make sure the adjusted channel is also in range
      if ((originalChannel >= 0 && originalChannel < MidiConstants.MAX_CHANNELS) && (channel < 0 || channel >= MidiConstants.MAX_CHANNELS))
      {
        return null;
      }
      Midel newMidel = midel.clone();
      newMidel.setChannel(channel);
      newMidi.add(newMidel);
    }

    return newMidi;
  }

  public Midi transpose(int deltaKeys)
  {
    Midi newMidi = new Midi();

    for (Midel midel : midels)
    {
      Midel newMidel = midel.clone();
      if (newMidel instanceof Note)
      {
        Note newNote = (Note)newMidel;
        int key = newNote.getKey();
        key += deltaKeys;
        if (key < 0 || key >= MidiConstants.MAX_MIDI_KEYS)
        {
          return null;
        }
        newNote.setKey(key);
      }
      newMidi.add(newMidel);
    }

    return newMidi;
  }

  public Midi reverseTimeByChannel()
  {
    Midi newMidi = new Midi();

    for (int i = 0; i < 16; i++)
    {
      Midi channelNotes = copyChannel(i);
      if (channelNotes.size() > 1)
      {
        channelNotes.reverseTime(newMidi);
      }
    }

    return newMidi;
  }

  private void reverseTime(Midi newMidi)
  {
    long minTick = getFirstTick();
    long maxTick = getMaxTick();

    for (Midel midel : midels)
    {
      Midel newMidel = midel.clone();
      long endingTick = midel.getTick();
      if (newMidel instanceof Note)
      {
        Note newNote = (Note)newMidel;
        endingTick += newNote.getDuration();
      }
      long tick = minTick + (maxTick - endingTick);
      newMidel.setTick(tick);
      newMidi.add(newMidel);
    }
  }

  public Midi reversePitchByChannel()
  {
    Midi newMidi = new Midi();

    for (int i = 0; i < 16; i++)
    {
      Midi channelNotes = copyChannel(i);
      if (channelNotes.size() > 1)
      {
        channelNotes.reversePitch(newMidi);
      }
    }

    return newMidi;
  }

  private void reversePitch(Midi newMidi)
  {
    ArrayList<Note> notes = new ArrayList<Note>();

    for (Midel midel : midels)
    {
      if (midel instanceof Note)
      {
        Note note = (Note)midel;
        notes.add(note);
      }
    }

    int size = notes.size();

    for (int i = 0, j = size - 1; i <= j; i++, j--)
    {
      Note newFirstNote = notes.get(i).clone();
      Note newLastNote = notes.get(j).clone();

      int temp = newFirstNote.getKey();
      newFirstNote.setKey(newLastNote.getKey());
      newLastNote.setKey(temp);

      newMidi.add(newFirstNote);
      newMidi.add(newLastNote);
    }
  }

  public Midi roundToMeasure(long left, long right)
  {
    int ticksPerMeasure = findTicksPerMeasure(left);
    long measureBegin = (left / ticksPerMeasure) * ticksPerMeasure;
    long measureEnd = ((right + ticksPerMeasure - 1) / ticksPerMeasure) * ticksPerMeasure;

    Midi newMidi = new Midi();

    for (Midel midel : midels)
    {
      Midel newMidel = midel.clone();
      if (newMidel instanceof Note)
      {
        Note newNote = (Note)newMidel;
        long tick = newNote.getTick();
        long duration = newNote.getDuration();

        // Precedes current measure
        if (tick < measureBegin)
        {
          if ((tick + duration) > measureBegin)
          {
            duration -= (measureBegin - tick);
            tick = measureBegin;
            newNote.setTick(tick);
            newNote.setDuration(duration);
          }
        }
        // Exceeds current measure
        if ((tick + duration) > measureEnd)
        {
          duration = measureEnd - tick;
          newNote.setDuration(duration);
        }
      }

      newMidi.add(newMidel);
    }

    return newMidi;
  }

  // To convert milliseconds to ticks requires consideration
  // of both the tempo (in beats per minute) and the resolution
  // (in ticks per beat).

  // For this discussion, one quarter note equals one beat.

  // Here are the fundamental relationships:

  // BPM beats     1 sec   RES ticks   BPM * RES   ticks
  // --------- * ------- * --------- = --------- = -----
  //    60 sec   1000 ms        beat   60 * 1000      ms

  // To check  this out, substitute the following:

  // BPM = beats per minute = Midi.DEFAULT_TEMPO_IN_BPM (e.g. 120)
  // RES = resolution in ticks per beat = Midi.DEFAULT_RESOLUTION (e.g. 250)

  // 120 * 250   30000   1 tick
  // --------- = ----- = ------  (sometimes it may be helpful to
  // 60 * 1000   60000     2 ms   think of this as 2 ms / tick)

  // To convert ms to ticks, just multiply the entire equation by ms:

  // ticks   ms
  // ----- * -- = ticks
  //    ms    1

  // Because the tempo can (and does) change throughout a midi file,
  // doing the conversion correctly requires adjusting the "time" in
  // ms for every tempo change up to the controlling tempo change.
  // We do not currently handle this.

  public static long convertMillisToTicks(long millis)
  {
    long ticks = millis * (DEFAULT_TEMPO_IN_BPM * DEFAULT_RESOLUTION) / 60000;
    return ticks;
  }

  public static void convertMillisToTicks(Note note)
  {
    note.setTick(convertMillisToTicks(note.getTick()));
    note.setDuration(convertMillisToTicks(note.getDuration()));
  }

  public static long convertResolution(long tick, long resolution)
  {
    // It may make more sense to think of this as dividing by the ratio
    // of the resolutions:
    //
    //     tick / (resolution / DEFAULT_RESOLUTION);
    //
    // So, if the resolution is the same, the denominator is one,
    // and the result is the same. If the source resolution is twice
    // the default resolution, the denominator is one and the result
    // is half the input.
    //
    // For example, for microsecond resolution:
    //
    //     tick / (500,000 / 250)
    //     tick / 2000

    return tick / (resolution / DEFAULT_RESOLUTION);
  }

  public void setCurrentVersion(boolean isCurrentVersion)
  {
    this.isCurrentVersion = isCurrentVersion;
  }

  public boolean isCurrentVersion()
  {
    return isCurrentVersion;
  }

  public Sequence toSequence()
  {
    try
    {
      Sequence sequence = new Sequence(Sequence.PPQ, DEFAULT_RESOLUTION);
      toTrack(sequence);
      return sequence;
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * Multitrack sequences must be written as file type 1.
   * 
   * The Java Sound Sequencer appears to only handle tempo events
   * if they are in the first track in the sequence.
   * 
   * We write our signature event, the first event in the file,
   * to the lowest numbered channel, ensuring that the lowest
   * numbered channel is in the first track.
   * 
   * Therefore, for tempo events to have an effect, they
   * should be placed in the lowest numbered channel.
   */
  public void toTrack(Sequence sequence) throws Exception
  {
    HashMap<Integer, Track> tracks = new HashMap<Integer, Track>();

    signSequence(sequence, tracks);

    for (Midel midel : midels)
    {
      int channel = midel.getChannel();
      Track track = getTrack(sequence, tracks, channel);
      midel.addTo(track);
    }
  }

  private void signSequence(Sequence sequence, HashMap<Integer, Track> tracks)
  {
    if (midels.size() > 0)
    {
      int firstChannel = getLowestChannel();
      Track defaultTrack = getTrack(sequence, tracks, firstChannel);
      new RiffCrafterEvent(0, firstChannel).addTo(defaultTrack);
    }
  }

  private Track getTrack(Sequence sequence, HashMap<Integer, Track> tracks, int trackKey)
  {
    Track track = tracks.get(trackKey);
    if (track == null)
    {
      track = sequence.createTrack();
      tracks.put(trackKey, track);
    }
    return track;
  }

  public long getBaseTick()
  {
    long minTick = getFirstTick();
    int ticksPerMeasure = findTicksPerMeasure(minTick);
    return (minTick / ticksPerMeasure) * ticksPerMeasure;
  }

  public int findTicksPerMeasure(long tick)
  {
    int beatsPerMeasure = DEFAULT_BEATS_PER_MEASURE;
    TimeSignatureChange lastTimeSignatureChange = findTimeSignatureChange(tick);
    if (lastTimeSignatureChange != null)
    {
      beatsPerMeasure = lastTimeSignatureChange.getBeatsPerMeasure();
    }
    int ticksPerMeasure = beatsPerMeasure * DEFAULT_RESOLUTION;
    return ticksPerMeasure;
  }

  public void changeProgram(int channel, long tick, int program, boolean isUpdate)
  {
    if (isUpdate)
    {
      ProgramChange oldProgramChange = findProgramChange(channel, tick);
      if (oldProgramChange == null)
      {
        tick = 0;
      }
      else
      {
        tick = oldProgramChange.getTick();
        remove(oldProgramChange);
      }
    }
    ProgramChange programChange = ProgramChange.create(tick, channel, program);
    add(programChange);
  }

  // NB: This gets call frequently. It must be very fast.
  // NB: programChanges is ordered by channel, then tick.

  public ProgramChange findProgramChange(int channel, long tick)
  {
    ProgramChange targetProgramChange = new ProgramChange(tick, channel);
    ProgramChange controllingProgramChange = programChanges.floor(targetProgramChange);
    if (controllingProgramChange == null || controllingProgramChange.getChannel() != channel)
    {
      return null;
    }

    return controllingProgramChange;
  }

  public TempoChange findTempoChange(long tick)
  {
    TempoChange controllingTempoChange = null;
    for (TempoChange tempoChange : tempoChanges)
    {
      if (tempoChange.getTick() > tick)
      {
        return controllingTempoChange;
      }
      else
      {
        controllingTempoChange = tempoChange;
      }
    }
    return controllingTempoChange;
  }

  public TimeSignatureChange findTimeSignatureChange(long tick)
  {
    TimeSignatureChange controllingTimeSignatureChange = null;
    for (TimeSignatureChange timeSignatureChange : timeSignatureChanges)
    {
      if (timeSignatureChange.getTick() > tick)
      {
        return controllingTimeSignatureChange;
      }
      else
      {
        controllingTimeSignatureChange = timeSignatureChange;
      }
    }
    return controllingTimeSignatureChange;
  }

  public int getProgram(int channel, long tick)
  {
    int program = channel == Instruments.DRUM_CHANNEL ? -1 : channel;
    ProgramChange programChange = findProgramChange(channel, tick);
    if (programChange != null)
    {
      program = programChange.getProgram();
    }
    return program;
  }

  public void applyProgramsFrom(Midi sourceMidi, long sourceTick, long tick)
  {
    int[] activeChannels = getActiveChannels();
    for (int i = 0; i < activeChannels.length; i++)
    {
      int channel = activeChannels[i];
      if (channel != Instruments.DRUM_CHANNEL)
      {
        int program = getProgram(channel, tick);
        int sourceProgram = sourceMidi.getProgram(channel, sourceTick);
        if (program != sourceProgram)
        {
          changeProgram(channel, tick, sourceProgram, true);
        }
      }
    }
  }

  public void applyTempoFrom(Midi sourceMidi, long sourceTick, long tick)
  {
    TempoChange tempoChange = findTempoChange(tick);
    TempoChange sourceTempoChange = sourceMidi.findTempoChange(sourceTick);
    if (sourceTempoChange != null && !sourceTempoChange.equalsTempo(tempoChange))
    {
      TempoChange newTempoChange = (TempoChange)sourceTempoChange.clone();
      newTempoChange.setTick(tick);
      add(newTempoChange);
    }
  }

  public Statistics getStatistics(int channel)
  {
    return statisticsManager.getStatistics(channel);
  }

  public int getAverageKey()
  {
    return statisticsManager.getAverageKey();
  }

  public int getAverageKey(int channel)
  {
    return statisticsManager.getAverageKey(channel);
  }

  public boolean containsLyrics()
  {
    return ticksPerLetter > 0;
  }

  public int getTicksPerLetter()
  {
    return ticksPerLetter;
  }

  public int getLyricType()
  {
    return lyricType;
  }

  public int getFirstChannel()
  {
    for (Midel midel : midels)
    {
      if (midel instanceof Note)
      {
        Note note = (Note)midel;
        int channel = note.getChannel();
        return channel;
      }
    }

    return -1;
  }

  public Note getFirstNote()
  {
    for (Midel midel : midels)
    {
      if (midel instanceof Note)
      {
        Note note = (Note)midel;
        return note;
      }
    }
    return null;
  }

  private int getLowestChannel()
  {
    // TODO: Monitor the cost of this operation and cache the lowest channel, if necessary
    int lowestChannel = Integer.MAX_VALUE;
    for (Midel midel : midels)
    {
      int channel = midel.getChannel();
      if (channel < lowestChannel)
      {
        lowestChannel = channel;
      }
    }
    return lowestChannel;
  }

  public long getMaxTick()
  {
    return statisticsManager.getMaxTick(); // includes duration
  }

  public long getFirstTick()
  {
    long minTick = 0;
    Midel midel = first();
    if (midel != null)
    {
      minTick = midel.getTick();
    }

    return minTick;
  }

  public long getLastTick()
  {
    long lastTick = 0;
    Midel midel = last();
    if (midel != null)
    {
      lastTick = midel.getTick();
    }

    return lastTick;
  }

  public long findFirstTick(int channel)
  {
    for (Midel midel : midels)
    {
      if (midel.getChannel() == channel)
      {
        return midel.getTick();
      }
    }
    return -1;
  }

  public long findLastTick(int channel)
  {
    for (Midel midel : midels.descendingSet())
    {
      if (midel.getChannel() == channel)
      {
        return midel.getTick();
      }
    }
    return 0;
  }

  public long findMaxTick(int channel)
  {
    long maxTick = 0;

    for (Midel midel : midels)
    {
      if (midel.getChannel() == channel)
      {
        long tick = midel.getTick();
        if (midel instanceof Note)
        {
          Note note = (Note)midel;
          tick += note.getDuration();
        }
        maxTick = Math.max(maxTick, tick);
      }
    }

    // System.out.println("Midi.findMaxTick: maxTick=" + maxTick + " (expensive operation)");

    return maxTick;
  }

  public int getLowestKey(int channel)
  {
    return getLowestKey(channel, null);
  }

  public int getLowestKey(int channel, Note exceptNote)
  {
    int key = Integer.MAX_VALUE;
    for (Midel midel : midels)
    {
      if (midel.getChannel() == channel && midel != exceptNote && midel instanceof Note)
      {
        Note note = (Note)midel;
        key = Math.min(note.getKey(), key);
      }
    }
    return key == Integer.MAX_VALUE ? -1 : key;
  }

  public int getHighestKey(int channel)
  {
    return getHighestKey(channel, null);
  }

  public int getHighestKey(int channel, Note exceptNote)
  {
    int key = 0;
    for (Midel midel : midels)
    {
      if (midel.getChannel() == channel && midel != exceptNote && midel instanceof Note)
      {
        Note note = (Note)midel;
        key = Math.max(note.getKey(), key);
      }
    }
    return key;
  }

  public int[] getActiveChannels()
  {
    return statisticsManager.getActiveChannels();
  }

  public NavigableSet<Midel> getSet(long firstTick, long lastTick)
  {
    NavigableSet<Midel> set;
    Midel first = getCeiling(firstTick);
    Midel last = getFloor(lastTick);

    if (first == null || last == null || first.compareTo(last) > 0)
    {
      set = new TreeSet<Midel>();
    }
    else
    {
      set = midels.subSet(first, true, last, true);
    }

    return set;
  }

  public Midel getFloor(long tick)
  {
    Midel midel = new Midel(tick, Midel.LOWEST_IN_SEQUENCE);
    Midel floor = floor(midel);
    return floor;
  }

  public Midel getCeiling(long tick)
  {
    Midel midel = new Midel(tick, Midel.HIGHEST_IN_SEQUENCE);
    Midel ceiling = ceiling(midel);
    return ceiling;
  }

  public Midel getPrevious(Midel lastMidel)
  {
    Midel previousMidel = midels.lower(lastMidel);
    return previousMidel;
  }

  public Midel getNext(Midel lastMidel)
  {
    Midel nextMidel = midels.higher(lastMidel);
    return nextMidel;
  }

  public Midel get(int index)
  {
    for (Midel midel : midels)
    {
      if (index-- == 0)
      {
        return midel;
      }
    }
    return null;
  }

  public int getIndex(long tick)
  {
    int row = 0;

    for (Midel midel : midels)
    {
      if (midel.getTick() >= tick)
      {
        return row;
      }
      row++;
    }

    return row - 1;
  }

  public int getIndex(Midel searchMidel)
  {
    int row = 0;

    for (Midel midel : midels)
    {
      if (midel.equals(searchMidel))
      {
        return row;
      }
      row++;
    }

    return -1;
  }

  //  C# D#  F# G# A# 
  // C  D  EF  G  A  B  
  //  Db Eb  Gb Ab Bb

  private static final KeySignature[] keySignatures = new KeySignature[] {
  //                    C D EF G A B
      new KeySignature("101011010101", 0, 9, true, 0), //   C  maj/A  min
      new KeySignature("101011010101", 9, 0, false, 0), //   C  maj/A  min
      new KeySignature("101010110101", 7, 4, true, 1), //   G  maj/E  min (F#)
      new KeySignature("101010110101", 4, 7, false, 1), //   G  maj/E  min (F#)
      new KeySignature("101011010110", 5, 2, true, -1), //  F  maj/D  min (Bb)
      new KeySignature("101011010110", 2, 5, false, -1), //  F  maj/D  min (Bb)
      new KeySignature("011010110101", 2, 11, true, 2), //  D  maj/B  min (C#, F#)
      new KeySignature("011010110101", 11, 2, false, 2), //  D  maj/B  min (C#, F#)
      new KeySignature("101101010110", 10, 7, true, -2), // Bb maj/G  min (Bb, Eb)
      new KeySignature("101101010110", 7, 10, false, -2), // Bb maj/G  min (Bb, Eb)
      new KeySignature("011010101101", 9, 6, true, 3), //   A  maj/F# min (C#, F#, G#)
      new KeySignature("011010101101", 6, 9, false, 3), //   A  maj/F# min (C#, F#, G#)
      new KeySignature("101101011010", 3, 0, true, -3), //  Eb maj/C  min (Bb, Eb, Ab)
      new KeySignature("101101011010", 0, 3, false, -3), //  Eb maj/C  min (Bb, Eb, Ab)
      new KeySignature("010110101101", 4, 1, true, 4), //   E  maj/C# min (C#, D#, F#, G#)
      new KeySignature("010110101101", 1, 4, false, 4), //   E  maj/C# min (C#, D#, F#, G#)
      new KeySignature("110101011010", 8, 5, true, -4), //  Ab maj/F  min (Bb, Db, Eb, Ab)
      new KeySignature("110101011010", 5, 8, false, -4), //  Ab maj/F  min (Bb, Db, Eb, Ab)
      new KeySignature("010110101011", 11, 8, true, 5), //  B  maj/G# min (C#, D#, F#, G#, A#)
      new KeySignature("010110101011", 8, 11, false, 5), //  B  maj/G# min (C#, D#, F#, G#, A#)
      new KeySignature("110101101010", 1, 10, true, -5), // Db maj/Bb min (Bb, Db, Eb, Gb, Ab)
      new KeySignature("110101101010", 10, 1, false, -5), // Db maj/Bb min (Bb, Db, Eb, Gb, Ab)
      new KeySignature("010101101011", 6, 3, true, 6), //   F# maj/D# min (C#, D#, E#, F#, G#, A#)     Note: E# is enharmonic to F
      new KeySignature("010101101011", 3, 6, false, 6), //   F# maj/D# min (C#, D#, E#, F#, G#, A#)     Note: E# is enharmonic to F
  //      new KeySignature("010101101011", 6, 3, true, -6), //  Gb maj/Eb min (Bb, Cb, Db, Eb, Gb, Ab)     Note: Cb is enharmonic to B
  //      new KeySignature("010101101011", 3, 6, false, -6), //  Gb maj/Eb min (Bb, Cb, Db, Eb, Gb, Ab)     Note: Cb is enharmonic to B
  //      new KeySignature("110101101010", 1, 10, true, 7), //  C# maj/A# min (C#, D#, E#, F#, G#, A#, B#) Note: E# is enharmonic to F, B# is enharmonic to C
  //      new KeySignature("110101101010", 10, 1, false, 7), //  C# maj/A# min (C#, D#, E#, F#, G#, A#, B#) Note: E# is enharmonic to F, B# is enharmonic to C
  //      new KeySignature("010110101011", 11, 8, true, -7), // Cb maj/Ab min (Bb, Cb, Db, Eb, Fb, Gb, Ab) Note: Cb is enharmonic to B, Fb is enharmonic to E
  //      new KeySignature("010110101011", 8, 11, false, -7), // Cb maj/Ab min (Bb, Cb, Db, Eb, Fb, Gb, Ab) Note: Cb is enharmonic to B, Fb is enharmonic to E
  };

  public KeyScore[] getKeyScores()
  {
    int[] noteCounts = new int[MidiConstants.SEMITONES_PER_OCTAVE];
    Arrays.fill(noteCounts, 0);

    for (int i = 0; i < MidiConstants.MAX_CHANNELS; i++)
    {
      Statistics statistics = statisticsManager.getStatistics(i);
      if (statistics.getTotalKeys() > 0)
      {
        int[] keyCounts = statistics.getKeyCounts();
        for (int j = 0; j < MidiConstants.MAX_MIDI_KEYS; j++)
        {
          noteCounts[j % MidiConstants.SEMITONES_PER_OCTAVE] += keyCounts[j];
        }
      }
    }

    KeyScore[] keyScores = new KeyScore[keySignatures.length];

    for (int i = 0; i < keySignatures.length; i++)
    {
      KeySignature keySignature = keySignatures[i];
      keyScores[i] = keySignature.getKeyScore(noteCounts);
    }

    Arrays.sort(keyScores, new KeyScoreComparator());

    for (int i = 0; i < keySignatures.length; i++)
    {
      if ((i > 0 && keyScores[i].isTieScore(keyScores[i - 1])))
      {
        keyScores[i].setRank(keyScores[i - 1].getRank());
      }
      else
      {
        keyScores[i].setRank(i + 1);
      }
    }
    return keyScores;
  }

  public static int min(Integer... numbers)
  {
    int min = Integer.MAX_VALUE;
    for (int number : numbers)
    {
      if (number < min)
      {
        min = number;
      }
    }
    return min;
  }

  public ArrayList<KeyScore> getKeyScores(int minimumConfidence)
  {
    KeyScore[] keyScores = getKeyScores();
    ArrayList<KeyScore> qualifyingKeyScores = new ArrayList<KeyScore>();

    for (int i = 0; i < keyScores.length && keyScores[i].getConfidence() > minimumConfidence; i++)
    {
      qualifyingKeyScores.add(keyScores[i]);
    }

    return qualifyingKeyScores;
  }

  public void addMidiListener(MidiListener midiListener)
  {
    midiListeners.add(midiListener);
  }

  private void fireAdd(Midel midel)
  {
    for (MidiListener midiListener : midiListeners)
    {
      midiListener.onAddMidel(this, midel);
    }
  }

  private void fireRemove(Midel midel)
  {
    for (MidiListener midiListener : midiListeners)
    {
      midiListener.onRemoveMidel(this, midel);
    }
  }

  public interface MidiListener
  {
    public void onAddMidel(Midi midi, Midel addMidel);

    public void onRemoveMidel(Midi midi, Midel removeMidel);
  }

  public Iterable<Midel> getMidels()
  {
    return new Iterable<Midel>()
    {
      public Iterator<Midel> iterator()
      {
        return getIterator();
      }
    };
  }

  public Iterator<Midel> getIterator()
  {
    return new MidiIterator(midels.iterator());
  }

  private Iterator<Midel> getIterator(long fromTick)
  {
    Midel tickMidel = new Midel(fromTick, Midel.LOWEST_IN_SEQUENCE);
    Midel startMidel = ceiling(tickMidel);
    Iterator<Midel> iterator;
    if (startMidel == null)
    {
      iterator = new EmptyIterator<Midel>();
    }
    else
    {
      NavigableSet<Midel> tailSet = midels.tailSet(startMidel, true);
      iterator = tailSet.iterator();
    }
    Iterator<Midel> wrappedIterator = new MidiIterator(iterator);
    return wrappedIterator;
  }

  private class EmptyIterator<E> implements Iterator<E>
  {
    public boolean hasNext()
    {
      return false;
    }

    public E next()
    {
      throw new NoSuchElementException();
    }

    public void remove()
    {
      throw new IllegalStateException();
    }
  }

  public MidiNavigator navigator()
  {
    return new MidiNavigator(midels);
  }

  public int size()
  {
    return midels.size();
  }

  public boolean contains(Midel midel)
  {
    return midels.contains(midel);
  }

  public Midel first()
  {
    return midels.size() == 0 ? null : midels.first();
  }

  public Midel last()
  {
    return midels.size() == 0 ? null : midels.last();
  }

  public Midel floor(Midel midel)
  {
    Midel floorMidel = midels.floor(midel);
    return floorMidel;
  }

  public Midel ceiling(Midel midel)
  {
    Midel ceilingMidel = midels.ceiling(midel);
    return ceilingMidel;
  }

  public String toBase64()
  {
    try
    {
      Sequence sequence = toSequence();
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      OutputStream base64OutputStream = new Base64.OutputStream(byteArrayOutputStream);
      Catcher.write(sequence, MidiConstants.MULTIPLE_TRACK, base64OutputStream);
      base64OutputStream.close();
      byteArrayOutputStream.close();
      String base64 = byteArrayOutputStream.toString();
      return base64;
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static Midi fromBase64(String base64)
  {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(base64.getBytes());
    InputStream base64InputStream = new Base64.InputStream(byteArrayInputStream);
    Midi midi = new Midi(base64InputStream);
    return midi;
  }

  public String toString()
  {
    return "[size=" + size() + ", notes=" + midels.toString() + "]";
  }

  private class MidiIterator implements Iterator<Midel>
  {
    private Midel midel;
    private Iterator<Midel> iterator;

    public MidiIterator(Iterator<Midel> iterator)
    {
      this.iterator = iterator;
    }

    public boolean hasNext()
    {
      return iterator.hasNext();
    }

    public Midel next()
    {
      midel = iterator.next();
      return midel;
    }

    public void remove()
    {
      iterator.remove();
      recordRemoveOperation(midel);
    }
  }

  public class MidiNavigator extends Navigator<Midel>
  {
    public MidiNavigator(TreeSet<Midel> set)
    {
      super(set);
    }

    @Override
    public Midel next()
    {
      return super.next();
    }

    @Override
    public Midel previous()
    {
      return super.previous();
    }
  }

  public class NoteBuilder
  {
    private int setNumber;
    private long startTime;
    private KeyManager keyManager = new KeyManager();

    private long lastTick;
    private int letterCount;
    private long totalTicks;
    private int totalLetters;

    private double resolutionScale;

    private int channel = Midel.DEFAULT_CHANNEL;
    private ArrayList<Midel> metaMidels;

    public NoteBuilder(int resolution)
    {
      resolutionScale = (double)DEFAULT_RESOLUTION / (double)resolution;
    }

    public int getChannel()
    {
      return channel;
    }

    public ArrayList<Midel> getMetaMidels()
    {
      return metaMidels;
    }

    public long normalize(long inputValue)
    {
      long outputValue = (long)(resolutionScale * inputValue);
      return outputValue;
    }

    public void processMessage(MidiMessage midiMessage, long tick)
    {
      tick = normalize(tick);

      if (midiMessage instanceof ShortMessage)
      {
        processShortMessage((ShortMessage)midiMessage, tick);
      }
      else if (midiMessage instanceof MetaMessage)
      {
        processMetaMessage((MetaMessage)midiMessage, tick);
      }
      else if (midiMessage instanceof SysexMessage)
      {
        processSysexMessage((SysexMessage)midiMessage, tick);
      }
      else
      {
        throw new RuntimeException("Unrecognized MidiMessage type " + midiMessage.getClass().getCanonicalName());
      }
    }

    public void processShortMessage(ShortMessage message, long tick)
    {
      int command = message.getCommand();

      if (command == ShortMessage.NOTE_OFF)
      {
        processKeyOff(message, tick);
      }
      else if (command == ShortMessage.NOTE_ON)
      {
        processKeyOn(message, tick);
      }
      else if (command == ShortMessage.PROGRAM_CHANGE)
      {
        ProgramChange programChange = new ProgramChange(tick, message);
        addChannelMidel(programChange);
      }
      else
      {
        ChannelEvent channelEvent = new ChannelEvent(tick, message);
        addChannelMidel(channelEvent);
      }
    }

    private void processKeyOn(ShortMessage message, long tick)
    {
      int key = message.getData1();
      int velocity = message.getData2();
      int channel = message.getChannel();
      if (velocity == 0)
      {
        processKeyOff(message, tick);
        return;
      }

      if (keyManager.allKeysAreOff())
      {
        if (tick > startTime)
        {
          long duration = tick - startTime;
          getStatistics(channel).addRest(startTime, duration);
          startTime = tick;
        }
      }
      else if (keyManager.keyIsOn(key))
      {
        processKeyOff(message, tick);
      }
      keyManager.save(key, channel, velocity, tick);
    }

    private void processKeyOff(ShortMessage message, long tick)
    {
      int key = message.getData1();

      if (!keyManager.keyIsOn(key))
      {
        return;
      }

      int channel = keyManager.getChannel(key);
      int velocity = keyManager.getVelocity(key);
      long keyStartTime = keyManager.getTick(key);
      long duration = tick - keyStartTime;

      Note note = new Note(channel, key, velocity, keyStartTime, duration);
      addChannelMidel(note);
      keyManager.free(key);

      if (keyManager.allKeysAreOff())
      {
        setNumber++;
        startTime = tick;
      }

    }

    public void addChannelMidel(Midel midel)
    {
      channel = midel.getChannel();
      add(midel);
    }

    public void addMetaMidel(Midel midel)
    {
      if (metaMidels == null)
      {
        metaMidels = new ArrayList<Midel>();
      }
      metaMidels.add(midel);
      // this will happen later, once the channel has been assigned
      // because the channel is part of the sort order and cannot be
      // changed once the midel has been made read-only.
      //add(midel);
    }

    public void processMetaMessage(MetaMessage message, long tick)
    {
      int type = message.getType();

      if (type == MidiConstants.MM_TEMPO)
      {
        TempoChange tempoChange = new TempoChange(tick, message);
        addMetaMidel(tempoChange);
      }
      else if (type == MidiConstants.MM_TIME_SIGNATURE)
      {
        TimeSignatureChange timeSignatureChange = new TimeSignatureChange(tick, message);
        addMetaMidel(timeSignatureChange);
      }
      else if (type == MidiConstants.MM_VENDOR_SPECIFIC)
      {
        MetaEvent metaEvent = new MetaEvent(tick, message);
        String text = metaEvent.getText();
        if (text.equals(RiffCrafterEvent.RIFFCRAFTER_SIGNATURE))
        {
          isCurrentVersion = true;
        }
        else
        {
          addMetaMidel(metaEvent);
        }
      }
      else if (type == MidiConstants.MM_END_OF_TRACK)
      {
        // Discard this, it is maintained by MIDI library
      }
      else
      {
        MetaEvent metaEvent = new MetaEvent(tick, message);
        addMetaMidel(metaEvent);
        if (type == MidiConstants.MM_LYRIC && lyricType != MidiConstants.MM_TEXT)
        {
          lyricType = type;
          countTicksPerLetter(message, tick);
        }
        if (tick > 0 && type == MidiConstants.MM_TEXT && lyricType != MidiConstants.MM_LYRIC)
        {
          lyricType = type;
          countTicksPerLetter(message, tick);
        }
      }
    }

    private void countTicksPerLetter(MetaMessage message, long tick)
    {
      long interval = tick - lastTick;
      if (interval < findTicksPerMeasure(tick))
      {
        totalTicks += (tick - lastTick);
        totalLetters += letterCount;
      }
      lastTick = tick;
      letterCount = message.getLength() - 2;
    }

    public void processSysexMessage(SysexMessage message, long tick)
    {
      SysexEvent sysexEvent = new SysexEvent(tick, message);
      addMetaMidel(sysexEvent);
    }

    public int getTicksPerLetter()
    {
      return (int)(totalLetters == 0 ? 0 : totalTicks / totalLetters);
    }

  }

  public class KeyManager
  {
    private class KeyData
    {
      private int channel;
      private int velocity;
      private long tick;

      KeyData(int channel, int velocity, long tick)
      {
        this.channel = channel;
        this.velocity = velocity;
        this.tick = tick;
      }
    }

    private KeyData[] keyData = new KeyData[MidiConstants.MAX_MIDI_KEYS];

    public void save(int key, int channel, int velocity, long tick)
    {
      keyData[key] = new KeyData(channel, velocity, tick);
    }

    public void free(int key)
    {
      keyData[key] = null;
    }

    public boolean allKeysAreOff()
    {
      for (int i = 0; i < keyData.length; i++)
      {
        if (keyData[i] != null)
          return false;
      }
      return true;
    }

    public boolean keyIsOn(int key)
    {
      return keyData[key] != null;
    }

    public int getChannel(int key)
    {
      return keyData[key].channel;
    }

    public int getVelocity(int key)
    {
      return keyData[key].velocity;
    }

    public long getTick(int key)
    {
      return keyData[key].tick;
    }

  }

  public static final class KeySignature
  {
    private String notes;
    private int tonic;
    private int relativeTonic;
    private boolean isMajor;
    private int halfNotes;

    public KeySignature(String notes, int tonic, int relativeTonic, boolean isMajor, int halfNotes)
    {
      this.notes = notes;
      this.tonic = tonic;
      this.relativeTonic = relativeTonic;
      this.isMajor = isMajor;
      this.halfNotes = halfNotes;
    }

    // See http://en.wikipedia.org/wiki/Chord_%28music%29

    public KeyScore getKeyScore(int[] noteCounts)
    {
      int inKeyCount = 0;
      int accidentalCount = 0;

      for (int i = 0; i < noteCounts.length; i++)
      {
        boolean isInKey = notes.substring(i, i + 1).equals("1");
        if (isInKey)
        {
          inKeyCount += noteCounts[i];
        }
        else
        {
          accidentalCount += noteCounts[i];
        }
      }

      int third = (tonic + (isMajor ? MidiConstants.SEMITONES_PER_MAJOR_THIRD : MidiConstants.SEMITONES_PER_MINOR_THIRD)) % MidiConstants.SEMITONES_PER_OCTAVE;
      int perfectFifth = (tonic + MidiConstants.SEMITONES_PER_PERFECT_FIFTH) % MidiConstants.SEMITONES_PER_OCTAVE;

      int tonicThirdCount = min(noteCounts[tonic], noteCounts[third]);
      int tonicTriadCount = min(noteCounts[tonic], noteCounts[third], noteCounts[perfectFifth]);

      KeyScore keyScore = new KeyScore(inKeyCount, accidentalCount, tonicThirdCount, tonicTriadCount, tonic, relativeTonic, isMajor, halfNotes);
      return keyScore;
    }
  }

  public static final class KeyScore
  {
    private int inKeyCount;
    private int accidentalCount;
    private int tonicThirdCount;
    private int tonicTriadCount;
    private int tonic;
    private int relativeTonic;
    private boolean isMajor;
    private int halfNotes;
    private int rank;

    public KeyScore(int inKeyCount, int accidentalCount, int tonicThirdCount, int tonicTriadCount, int tonic, int relativeTonic, boolean isMajor, int halfNotes)
    {
      this.inKeyCount = inKeyCount;
      this.accidentalCount = accidentalCount;
      this.tonicThirdCount = tonicThirdCount;
      this.tonicTriadCount = tonicTriadCount;
      this.tonic = tonic;
      this.relativeTonic = relativeTonic;
      this.isMajor = isMajor;
      this.halfNotes = halfNotes;
    }

    public void setRank(int rank)
    {
      this.rank = rank;
    }

    public int getRank()
    {
      return rank;
    }

    public boolean isTieScore(KeyScore that)
    {
      return this.accidentalCount == that.accidentalCount && this.tonicTriadCount == that.tonicTriadCount;
    }

    public int getConfidence()
    {
      int confidence = 0;
      int totalCount = inKeyCount + accidentalCount;
      if (totalCount != 0)
      {
        confidence = (inKeyCount * 100) / totalCount;
      }
      return confidence;
    }

    public String toString()
    {
      return "[inKeyCount=" + inKeyCount + ", accidentalCount=" + accidentalCount + ", tonicTriadCount=" + tonicTriadCount + ", tonic=" + tonic + ", isMajor=" + isMajor + ", halfNotes=" + halfNotes + "]";
    }

    public String getKey()
    {
      return NoteName.getKeyName(tonic, isMajor, halfNotes);
    }

    public String getSynopsis()
    {
      return NoteName.getSynopsis(halfNotes);
    }

    public int getSharps()
    {
      return halfNotes < 0 ? 0 : halfNotes;
    }

    public int getFlats()
    {
      return halfNotes < 0 ? -halfNotes : 0;
    }

    public String getRelativeKey()
    {
      return NoteName.getKeyName(relativeTonic, !isMajor, halfNotes);
    }

    public int getAccidentals()
    {
      return accidentalCount;
    }

    public int getTriads()
    {
      return tonicTriadCount;
    }

    public int getThirds()
    {
      return tonicThirdCount;
    }

  }

  private class KeyScoreComparator implements Comparator<KeyScore>
  {
    // NB: This compare is intended for use with Array.sort and its
    // documented behavior to preserve the natural ordering when elements
    // compare equal. It will give unexpected results when used with ordered
    // lists (e.g. TreeMap) because multiple elements will compare equal.

    public int compare(KeyScore o1, KeyScore o2)
    {
      // Lower accidentalCounts sort first
      int deltaAccidental = o1.accidentalCount - o2.accidentalCount;
      if (deltaAccidental != 0)
      {
        return deltaAccidental;
      }

      // Higher tonicTriadCounts sort first
      int deltaTonicTriadCount = o2.tonicTriadCount - o1.tonicTriadCount;
      if (deltaTonicTriadCount != 0)
      {
        return deltaTonicTriadCount;
      }

      // Higher tonicThirdCounts sort first
      int deltaTonicThirdCount = o2.tonicThirdCount - o1.tonicThirdCount;
      return deltaTonicThirdCount;
    }
  }

}
