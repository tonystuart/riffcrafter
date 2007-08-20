// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import com.riffcrafter.common.midi.Channel;
import com.riffcrafter.common.midi.Cluster;
import com.riffcrafter.common.midi.Instruments;
import com.riffcrafter.common.midi.Midel;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.common.midi.MidiConstants;
import com.riffcrafter.common.midi.Note;
import com.riffcrafter.common.midi.NoteName;
import com.riffcrafter.common.midi.TempoChange;
import com.riffcrafter.common.midi.TickEventMap;
import com.riffcrafter.common.midi.TickEventMap.TickEvent;

public class Analyzer
{
  private static final int ARRAY_SIZE_INCREMENT = 20;

  private IdentityManager patternManager = new IdentityManager();
  private IdentityManager chordManager = new IdentityManager();
  private IdentityManager groupManager = new IdentityManager();

  private Cluster ticks = new Cluster();
  private Cluster durations = new Cluster();

  private int lastGroupIndex;
  private boolean isDebug = false;

  private Midi midi;

  public AnalyzerNode buildGrammar(Midi midi, int targetChannelIndex, boolean isGroupByMeasure, boolean isGroupByGap, int minimumGroupSize, int percentAverageGap)
  {
    AnalyzerNode root = createInitialTree(midi, targetChannelIndex, isGroupByMeasure, isGroupByGap, minimumGroupSize, percentAverageGap);
    buildGrammar(root);
    return root;
  }

  public AnalyzerNode createInitialTree(Midi midi, int targetChannelIndex, boolean isGroupByMeasure, boolean isGroupByGap, int minimumGroupSize, int percentAverageGap)
  {
    this.midi = midi;

    RootNode root = new RootNode(targetChannelIndex);

    trainClusters(midi);

    TickEventMap tickEventMap = new TickEventMap(midi, targetChannelIndex, ticks, durations);

    long lastTick = 0;
    long lastMeasure = 0;
    long minimumGapSize = 0;
    lastGroupIndex = 0;

    if (isGroupByGap)
    {
      long averageNoteGap = getAverageNoteGap(tickEventMap);
      minimumGapSize = (averageNoteGap * percentAverageGap) / 100;
    }

    ArrayList<Note> activeNotes = new ArrayList<Note>();
    ChordNode chordNode = new ChordNode();

    for (Map.Entry<Long, TickEvent> entry : tickEventMap.getTickEvents())
    {
      TickEvent tickEvent = entry.getValue();
      for (Note note : tickEvent.getNoteOffIterable())
      {
        activeNotes.remove(note);
        if (activeNotes.size() == 0)
        {
          lastTick = note.getTick() + note.getDuration();
        }
      }

      if (activeNotes.size() > 0)
      {
        Set<Note> noteOnSet = tickEvent.getNoteOnSet();
        if (noteOnSet.size() > 0)
        {
          if (isMinimalOverlap(activeNotes, noteOnSet))
          {
            activeNotes.clear();
            if (isGroupByGap)
            {
              lastTick = 0; // set to zero to eliminate gap on this iteration
            }
          }
        }
      }

      if (activeNotes.size() == 0)
      {
        chordNode = addLastNoteSet(root, chordNode);
      }

      for (Note note : tickEvent.getNoteOnIterable())
      {
        if (isGroupByMeasure)
        {
          long tick = note.getTick();
          long duration = note.getDuration();
          int ticksPerMeasure = midi.findTicksPerMeasure(tick);
          // TODO: Check fuzzy logic to see where this note is intended to fall
          if (duration > ticksPerMeasure)
          {
            // Keep long notes that straddle measure from pulling all accompanying notes into the next measure
            duration = ticksPerMeasure;
          }
          tick += (duration / 2);
          long thisMeasure = tick / ticksPerMeasure;
          if (thisMeasure > lastMeasure) // note gt so that fuzzy logic on small notes that overlap long ones doesn't pull thisMeasure back to previous measure.
          {
            chordNode = addLastNoteSet(root, chordNode);
            MeasureNode measureNode = new MeasureNode(lastMeasure);
            formGroup(root, measureNode, 1);
            lastMeasure = thisMeasure;
          }
        }
        else if (isGroupByGap && activeNotes.size() == 0)
        {
          long tick = note.getTick();
          if (lastTick == 0)
          {
            lastTick = tick; // Skip intro *and* minimal overlap notes
          }
          long deltaTime = tick - lastTick;
          if (deltaTime > minimumGapSize)
          {
            GroupNode groupNode = new GroupNode();
            formGroup(root, groupNode, minimumGroupSize);
          }
        }
        activeNotes.add(note);
        chordNode.add(note);
      }

    }

    if (isGroupByMeasure)
    {
      chordNode = addLastNoteSet(root, chordNode);
      MeasureNode measureNode = new MeasureNode(lastMeasure);
      formGroup(root, measureNode, minimumGroupSize);
    }
    else if (isGroupByGap)
    {
      if (countGroups(root) > 0)
      {
        GroupNode groupNode = new GroupNode();
        formGroup(root, groupNode, minimumGroupSize);
      }
    }

    root.complete();

    return root;
  }

  /**
   * Add notes to root and return chordNode suitable for subsequent use
   */
  
  private ChordNode addLastNoteSet(RootNode root, ChordNode chordNode)
  {
    int noteSetSize = chordNode.getChildCount();
    if (noteSetSize == 0)
    {
      // nothing to do
    }
    else if (noteSetSize == 1)
    {
      NoteNode noteNode = chordNode.getNoteItemAt(0);
      root.add(noteNode);
      chordNode = new ChordNode();
    }
    else if (chordNode.getNoteItemAt(0).getNote().getChannel() == Instruments.DRUM_CHANNEL)
    {
      // Convert chord into group for drum channel
      GroupNode drumNode = new GroupNode();
      for (int i = 0; i < noteSetSize; i++)
      {
        // Get first item because adding to drumNode removes from chordNode
        NoteNode drum = chordNode.getNoteItemAt(0);
        drumNode.add(drum);
      }
      drumNode.complete();
      root.add(drumNode);
      // Even though chordNode is empty, it is not suitable for reuse
      chordNode = new ChordNode();
    }
    else
    {
      chordNode.complete();
      root.add(chordNode);
      chordNode = new ChordNode();
    }
    return chordNode;
  }

  private int countGroups(RootNode root)
  {
    int groupCount = 0;
    int childCount = root.getChildCount();
    for (int i = 0; i < childCount; i++)
    {
      AnalyzerNode analyzerNode = (AnalyzerNode)root.getChildAt(i);
      if (analyzerNode instanceof GroupNode)
      {
        groupCount++;
      }
    }
    return groupCount;
  }

  private long getAverageNoteGap(TickEventMap tickEventMap)
  {
    long totalTicks = 0;
    int totalGaps = 0;

    long lastTick = 0;
    int activeNoteCount = 0;

    for (Map.Entry<Long, TickEvent> entry : tickEventMap.getTickEvents())
    {
      TickEvent tickEvent = entry.getValue();
      for (Note note : tickEvent.getNoteOffIterable())
      {
        activeNoteCount--;
        if (activeNoteCount == 0)
        {
          lastTick = note.getTick() + note.getDuration();
        }
      }
      for (Note note : tickEvent.getNoteOnIterable())
      {
        if (activeNoteCount == 0)
        {
          long thisTick = note.getTick();
          if (lastTick == 0)
          {
            // Don't let intro scew results
            lastTick = thisTick;
          }
          long deltaTick = thisTick - lastTick;
          totalTicks += deltaTick;
          totalGaps++;
        }
        activeNoteCount++;
      }
    }

    long averageNoteGap = totalGaps > 0 ? totalTicks / totalGaps : 0;
    return averageNoteGap;
  }

  private void formGroup(AnalyzerNode root, MultiNoteNode multiNoteNode, int minimumGroupSize)
  {
    // Count notes from lastGroupIndex, including notes in chords
    int noteCount = countNotes(root);

    if (noteCount >= minimumGroupSize)
    {
      int groupChildNodeCount = root.getChildCount() - lastGroupIndex;

      for (int i = 0; i < groupChildNodeCount; i++)
      {
        // This gets the next node because the one at this index on the previous iteration has been removed
        AnalyzerNode child = (AnalyzerNode)root.getChildAt(lastGroupIndex);

        // Note that adding a child automatically removes it from its parent
        multiNoteNode.add(child);
      }

      multiNoteNode.complete();
      root.add(multiNoteNode);
      lastGroupIndex++;
    }
    else
    {
      lastGroupIndex = root.getChildCount();
    }

  }

  private int countNotes(AnalyzerNode root)
  {
    int noteCount = 0;
    int childCount = root.getChildCount();
    for (int i = lastGroupIndex; i < childCount; i++)
    {
      AnalyzerNode child = (AnalyzerNode)root.getChildAt(i);
      noteCount += child.getNoteCount();
    }
    return noteCount;
  }

  private boolean isMinimalOverlap(ArrayList<Note> oldNotes, Set<Note> newNotes)
  {
    for (Note oldNote : oldNotes)
    {
      long oldTick = oldNote.getTick();
      long oldDuration = oldNote.getDuration();
      long oldNoteEnd = oldTick + oldDuration;
      for (Note newNote : newNotes)
      {
        long newTick = newNote.getTick();
        long newDuration = newNote.getDuration();
        long overlap = oldNoteEnd - newTick;
        long shortestDuration = Math.min(newDuration, oldDuration);
        if (shortestDuration == 0)
        {
          shortestDuration = 1;
        }
        long percent = (overlap * 100) / shortestDuration;
        if (percent > 25) // make this a variable?
        {
          return false;
        }
      }
    }
    return true;
  }

  private void trainClusters(Midi midi)
  {
    for (Midel midel : midi.getMidels())
    {
      if (midel instanceof Note)
      {
        Note note = (Note)midel;
        ticks.train(note.getTick());
        durations.train(note.getDuration());
      }
    }
  }

  // Example of loop limits:
  //
  // Needle = xxx (length = 3)
  //
  // 01234567890 (length = 11)
  // AxxxBBBxxxA
  //      p
  //         q
  //
  // p = maxNeedleIndex (11 - 2 * 3) -> 5
  // p = maxHaystackIndex (11 - 3) -> 8

  private static final int INITIAL_WINDOW_SIZE = -1;
  private static final int MINIMUM_WINDOW_SIZE = 2;

  public void buildGrammar(AnalyzerNode parent)
  {
    int windowSize = INITIAL_WINDOW_SIZE;
    int oldChildCount = parent.getChildCount();

    while ((windowSize = getWindowSize(parent, windowSize)) > MINIMUM_WINDOW_SIZE)
    {
      scan(parent, windowSize);
    }

    int newChildCount = parent.getChildCount();
    if (oldChildCount > newChildCount)
    {
      // Only replace remainder if repeating group patterns occurred at this level
      replaceRemainder(parent);
    }
  }

  private void scan(AnalyzerNode parent, int windowSize)
  {
    ArrayList<Integer> repeatingSequences = findRepeatingSequences(parent, windowSize);
    if (repeatingSequences.size() > 0)
    {
      replaceRepeatingSequences(parent, windowSize, repeatingSequences);
    }
  }

  private ArrayList<Integer> findRepeatingSequences(AnalyzerNode parent, int windowSize)
  {
    ArrayList<Integer> toReplace = new ArrayList<Integer>();

    int maxNeedleIndex = parent.getChildCount() - (2 * windowSize);
    for (int needleIndex = 0; needleIndex <= maxNeedleIndex && toReplace.size() == 0; needleIndex++)
    {
      int maxHaystackIndex = parent.getChildCount() - windowSize;
      for (int haystackIndex = needleIndex + windowSize; haystackIndex <= maxHaystackIndex; haystackIndex++)
      {
        if (isRepeatingSequence(parent, needleIndex, haystackIndex, windowSize))
        {
          if (toReplace.size() == 0)
          {
            toReplace.add(needleIndex);
          }
          toReplace.add(haystackIndex);
          // You can't just increment j in the for statement, or you could match overlapping segments!
          haystackIndex += windowSize - 1; // -1 for increment in for statement
        }
      }
    }
    return toReplace;
  }

  private void replaceRepeatingSequences(AnalyzerNode parent, int windowSize, ArrayList<Integer> toReplace)
  {
    for (int i = toReplace.size() - 1; i >= 0; i--)
    {
      int matchIndex = toReplace.get(i);
      AnalyzerNode patternNode = reduce(parent, matchIndex, windowSize);
      buildGrammar(patternNode);
    }
  }

  private void replaceRemainder(AnalyzerNode parent)
  {
    int lastNote = -1;
    for (int i = parent.getChildCount() - 1; i >= 0; i--)
    {
      AnalyzerNode analyzerNode = (AnalyzerNode)parent.getChildAt(i);
      if (analyzerNode instanceof PatternNode)
      {
        if (lastNote != -1)
        {
          int firstNote = i + 1;
          int size = (lastNote - firstNote) + 1; // +1 because lastNote is inclusive
          if (size > 1)
          {
            reduce(parent, firstNote, size);
          }
          lastNote = -1;
        }
      }
      else
      {
        if (lastNote == -1)
        {
          lastNote = i;
        }
      }
    }

    // Check for leftover pattern (of at least two notes) at beginning of string
    if (lastNote > 0)
    {
      reduce(parent, 0, lastNote + 1); // +1 because lastNote is inclusive
    }
  }

  private int getWindowSize(AnalyzerNode parent, int oldWindowSize)
  {
    int childCount = parent.getChildCount();
    int maximumWindowSize = childCount / 2;
    int newWindowSize = maximumWindowSize;
    if (oldWindowSize != INITIAL_WINDOW_SIZE)
    {
      newWindowSize = Math.min(--oldWindowSize, maximumWindowSize);
    }
    return newWindowSize;
  }

  private boolean isRepeatingSequence(AnalyzerNode parent, int i, int j, int windowSize)
  {
    for (int k = 0; k < windowSize; k++)
    {
      if (!parent.fuzzyEqualsChildren(i + k, j + k))
      {
        return false;
      }
    }
    return true;
  }

  private AnalyzerNode reduce(AnalyzerNode parent, int offset, int windowSize)
  {
    PatternNode patternNode = new PatternNode();

    int limit = offset + windowSize;
    for (int i = offset; i < limit; i++)
    {
      // Note that adding a child automatically removes it from its parent
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)parent.getChildAt(offset);
      patternNode.add(child);
    }

    patternNode.complete();

    parent.insert(patternNode, offset);
    return patternNode;
  }

  private class IdentityManager
  {
    private int count;
    private HashMap<Long, Integer> map = new HashMap<Long, Integer>();
    private IntegerArray frequency = new IntegerArray();

    private int find(AnalyzerNode parentNode, long hashKey)
    {
      int index;
      Integer value = map.get(hashKey);
      if (value == null)
      {
        index = ++count; // All IdentityManagers start at one
        map.put(hashKey, index);
        frequency.set(index, 1);
      }
      else
      {
        index = value.intValue();
        int occurrences = frequency.get(index);
        occurrences++;
        frequency.set(index, occurrences);
      }
      return index;
    }

    public int getFrequency(int index)
    {
      return frequency.get(index);
    }

  }

  private static class ChordName
  {
    private String intervals;
    private String name;

    private ChordName(String intervals, String name)
    {
      this.intervals = intervals;
      this.name = name;
    }

    public String getIntervals()
    {
      return intervals;
    }

    public String getName()
    {
      return name;
    }
  }

  // SEMITONES_PER_MINOR_SECOND = 1;
  // SEMITONES_PER_MAJOR_SECOND = 2;
  // SEMITONES_PER_MINOR_THIRD = 3;
  // SEMITONES_PER_MAJOR_THIRD = 4;
  // SEMITONES_PER_PERFECT_FOURTH = 5;
  // SEMITONES_PER_DIMINISHED_FIFTH = 6;
  // SEMITONES_PER_PERFECT_FIFTH = 7;
  // SEMITONES_PER_AUGMENTED_FIFTH = 8;
  // SEMITONES_PER_MAJOR_SIXTH = 9;
  // SEMITONES_PER_MINOR_SEVENTH = 10;
  // SEMITONES_PER_MAJOR_SEVENTH = 11;
  // SEMITONES_PER_OCTAVE = 12;

  private final static String intervalNames[] = new String[] //
  { //
      "oct", // 0
      "min2", // 1
      "maj2", // 2
      "min3", // 3
      "maj3", // 4
      "perf4", // 5
      "dim5", // 6
      "perf5", // 7
      "aug5", // 8
      "maj6", // 9
      "min7", // 10
      "maj7", // 11
  };

  private final static ChordName chordNames[] = new ChordName[] //
  { //             012345678901
      new ChordName("100100010000", "min"), //
      new ChordName("100100100000", "dim"), //
      new ChordName("100010010000", "Maj"), //
      new ChordName("100010001000", "aug"), //
      new ChordName("100001010000", "sus4"), //
      new ChordName("100010010010", "7"), //
      new ChordName("100010010001", "Maj7"), //
      new ChordName("100100010010", "min7"), //
      new ChordName("100010010100", "Maj6"), //
      new ChordName("100100010100", "m6"), //
      new ChordName("100000010000", "5"), //
  };

  public static String getChordName(ArrayList<Note> notes)
  {
    int root = MidiConstants.MAX_MIDI_KEYS;
    for (Note note : notes)
    {
      int key = note.getKey();
      if (key < root)
      {
        root = key;
      }
    }

    char intervals[] = new char[MidiConstants.SEMITONES_PER_OCTAVE];
    Arrays.fill(intervals, '0');

    for (Note note : notes)
    {
      int key = note.getKey();
      int interval = (key - root) % intervals.length;
      intervals[interval] = '1';
    }

    String text = formatIntervals(root, intervals);
    String chordName = formatChordName(root, intervals);
    if (chordName != null)
    {
      text += "=" + chordName;
    }
    return text;
  }

  private static String formatChordName(int root, char[] intervals)
  {
    String intervalString = new String(intervals);
    for (ChordName chordName : chordNames)
    {
      if (chordName.getIntervals().equals(intervalString))
      {
        return NoteName.getNoteNameWithoutOctave(root) + " " + chordName.getName();
      }
    }
    return null;
  }

  private static String formatIntervals(int root, char[] intervals)
  {
    int intervalCount = 0;
    StringBuffer chordSuffix = new StringBuffer();
    chordSuffix.append(NoteName.getNoteNameWithoutOctave(root));
    for (int i = 1; i < intervals.length; i++)
    {
      if (intervals[i] == '1')
      {
        chordSuffix.append("+");
        chordSuffix.append(intervalNames[i]);
        intervalCount++;
      }
    }
    if (intervalCount == 0)
    {
      chordSuffix.append("+");
      chordSuffix.append(intervalNames[0]); // otherwise we wouldn't be here
    }
    return chordSuffix.toString();
  }

  private class IntegerArray
  {
    int[] values = new int[ARRAY_SIZE_INCREMENT];

    public void set(int index, int value)
    {
      if (index >= values.length)
      {
        int newLength = ((values.length + ARRAY_SIZE_INCREMENT) / ARRAY_SIZE_INCREMENT) * ARRAY_SIZE_INCREMENT;
        values = Arrays.copyOf(values, newLength);
      }
      values[index] = value;
    }

    public int get(int index)
    {
      return values[index];
    }

  }

  public class AnalyzerNode extends DefaultMutableTreeNode
  {
    private long hashKey;

    private AnalyzerNode()
    {
    }

    protected AnalyzerNode(Object userObject)
    {
      super(userObject);
    }

    public boolean fuzzyEquals(AnalyzerNode object)
    {
      return getHashKey() == object.getHashKey();
    }

    public long getHashKey()
    {
      if (hashKey == 0)
      {
        hashKey = calculateHashKey();
      }
      return hashKey;
    }

    public void setHashKey(long hashKey)
    {
      this.hashKey = hashKey;
    }

    public long calculateHashKey()
    {
      int sequenceNumber = 1;
      long calculatedHashKey = 1;
      Enumeration enumeration = depthFirstEnumeration();
      while (enumeration.hasMoreElements())
      {
        AnalyzerNode analyzerNode = (AnalyzerNode)enumeration.nextElement();
        if (analyzerNode instanceof NoteNode)
        {
          NoteNode noteNode = (NoteNode)analyzerNode;
          long childHashKey = noteNode.getHashKey();
          // C4,E4,G4 hashes different from G4,E4,C4
          childHashKey *= sequenceNumber++;
          calculatedHashKey += childHashKey;
        }
      }
      return calculatedHashKey;
    }

    public boolean fuzzyEqualsChildren(int leftIndex, int rightIndex)
    {
      AnalyzerNode leftNode = (AnalyzerNode)getChildAt(leftIndex);
      AnalyzerNode rightNode = (AnalyzerNode)getChildAt(rightIndex);
      return leftNode.fuzzyEquals(rightNode);
    }

    public ArrayList<Note> getNotes()
    {
      ArrayList<Note> notes = new ArrayList<Note>();
      Enumeration enumeration = depthFirstEnumeration();
      while (enumeration.hasMoreElements())
      {
        AnalyzerNode analyzerNode = (AnalyzerNode)enumeration.nextElement();
        if (analyzerNode instanceof NoteNode)
        {
          Note note = ((NoteNode)analyzerNode).getNote();
          notes.add(note);
        }
      }
      return notes;
    }

    public Midi getMidi()
    {
      Midi newMidi = new Midi();
      Enumeration enumeration = depthFirstEnumeration();
      while (enumeration.hasMoreElements())
      {
        AnalyzerNode analyzerNode = (AnalyzerNode)enumeration.nextElement();
        if (analyzerNode instanceof NoteNode)
        {
          Note note = ((NoteNode)analyzerNode).getNote();
          // TODO: I had thought that when Player synchronized with Editor
          // to pick up the current settings for instrument (program) and
          // channel overrides, that it also initialized the tempo if the
          // tempo was not set. This is not the case. Although one might
          // think that Player could get the tempo from Midi via Editor,
          // the problem is that Player is passed a Sequence, which makes
          // it difficult to determine if a tempo has been supplied, or to
          // add one if not. I added this code here, assuming that selections
          // play at the right tempo, due to some magic in Player that was
          // not available for the Analyzer. However, this is the only code
          // that does it. Eventually we may want to factor this out and add
          // it to the code that sets the selection.
          if (newMidi.size() == 0)
          {
            long tick = note.getTick();
            TempoChange tempoChange = midi.findTempoChange(tick);
            if (tempoChange != null)
            {
              tempoChange = (TempoChange)tempoChange.clone();
              tempoChange.setTick(tick);
              tempoChange.setSerialNumber(0);
              // Some MIDI files contain multiple tempo change messages. Make
              // sure that ours always occurs in the lowest numberered channel.
              tempoChange.setChannel(-100);
              newMidi.add(tempoChange);
            }
          }
          newMidi.add(note);
        }
      }
      return newMidi;
    }

    protected int getNoteCount()
    {
      int noteCount = 0;
      Enumeration enumeration = depthFirstEnumeration();
      while (enumeration.hasMoreElements())
      {
        AnalyzerNode analyzerNode = (AnalyzerNode)enumeration.nextElement();
        if (analyzerNode instanceof NoteNode)
        {
          noteCount++;
        }
      }
      return noteCount;
    }

  }

  class NoteNode extends AnalyzerNode
  {

    public NoteNode(Note note)
    {
      super(note);

      int key = note.getKey();
      long duration = durations.get(note.getDuration());

      //      long hashKey = Math.max(1, key) * 1000;
      //      hashKey *= Math.max(1, duration) * 100;
      long hashKey = (Math.max(1, duration) * 1000) + key;

      setHashKey(hashKey);
    }

    @Override
    public long calculateHashKey()
    {
      //System.out.println("How can we end up not having a hash key?");
      return 1;
    }

    public String toString()
    {
      String string = getNote().getToolTipText();
      if (isDebug)
      {
        string += ", Key: " + getHashKey();
      }
      return string;
    }

    public Note getNote()
    {
      return (Note)getUserObject();
    }

  }

  private class ChordNode extends AnalyzerNode
  {
    private int chordNumber;
    private String chordName;
    private String stringRepresentation;

    private void add(Note note)
    {
      NoteNode noteNode = new NoteNode(note);
      add(noteNode);
    }

    public void complete()
    {
      chordNumber = chordManager.find(this, getHashKey());
      chordName = getChordName(getNotes());
    }

    public NoteNode getNoteItemAt(int index)
    {
      return (NoteNode)getChildAt(index);
    }

    public String toString()
    {
      if (stringRepresentation == null)
      {
        int childCount = getChildCount();
        int occurrences = chordManager.getFrequency(chordNumber);
        String chordText = chordName == null ? "" : " (" + chordName + ")";
        stringRepresentation = "Chord: " + chordNumber + chordText + ", Notes: " + childCount + ", Occurrences: " + occurrences;
        if (isDebug)
        {
          stringRepresentation += ", Key: " + getHashKey();
        }
      }
      return stringRepresentation;
    }
  }

  public abstract class MultiNoteNode extends AnalyzerNode
  {
    public abstract void complete();
  }

  private class RootNode extends MultiNoteNode
  {
    private int channel;
    private int noteCount;

    private RootNode(int channel)
    {
      this.channel = channel;
    }

    public String toString()
    {
      String string = "Channel: " + Channel.getChannelNumber(channel) + ", Notes: " + noteCount;
      return string;
    }

    @Override
    public void complete()
    {
      noteCount = getNoteCount();
    }
  }

  public class PatternNode extends MultiNoteNode
  {
    private int patternNumber;
    private int noteCount;

    public void complete()
    {
      patternNumber = patternManager.find(this, getHashKey());
      noteCount = getNoteCount();
    }

    public String toString()
    {
      int occurrences = patternManager.getFrequency(patternNumber);
      String string = "Pattern: " + patternNumber + ", Notes: " + noteCount + ", Occurrences: " + occurrences;
      if (isDebug)
      {
        string += ", Key: " + getHashKey();
      }
      return string;
    }

    @Override
    public boolean fuzzyEquals(AnalyzerNode rightNode)
    {
      return false;
    }

  }

  public class MeasureNode extends MultiNoteNode
  {
    private long measureNumber;
    private int noteCount;

    public MeasureNode(long measureNumber)
    {
      this.measureNumber = measureNumber;
    }

    public void complete()
    {
      noteCount = getNoteCount();
    }

    public String toString()
    {
      String string = "Measure: " + measureNumber + ", Notes: " + noteCount;
      if (isDebug)
      {
        string += ", Key: " + getHashKey();
      }
      return string;
    }
  }

  public class GroupNode extends MultiNoteNode
  {
    private int groupNumber;
    private int noteCount;

    public void complete()
    {
      groupNumber = groupManager.find(this, getHashKey());
      noteCount = getNoteCount();
    }

    public String toString()
    {
      int occurrences = groupManager.getFrequency(groupNumber);
      String string = "Group: " + groupNumber + ", Notes: " + noteCount + ", Occurrences: " + occurrences;
      if (isDebug)
      {
        string += ", Key: " + getHashKey();
      }
      return string;
    }
  }
}
