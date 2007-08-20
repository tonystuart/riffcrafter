// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.MidiDevice.Info;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public final class Instruments
{
  // http://www.midi.org/about-midi/gm/gm1sound.shtml
  private final static String[] HEADINGS = new String[] {
      //
      "Piano", //
      "Chrom Perc", //
      "Organ", //
      "Guitar", //
      "Bass", //
      "Strings", //
      "Ensemble", //
      "Brass", //
      "Reed", //
      "Pipe", //
      "Synth Lead", //
      "Synth Pad", //
      "Synth F/X", //
      "Ethnic", //
      "Percussive", //
      "Sound F/X", //
  //
  };

  private final static int GM_MIN_DRUM_KEY = 35;

  private final static String[] DRUMS = new String[] {
      "Acoustic Bass Drum", //
      "Bass Drum 1", //
      "Side Stick", //
      "Acoustic Snare",
      "Hand Clap", //
      "Electric Snare", //
      "Low Floor Tom", //
      "Closed Hi Hat",
      "High Floor Tom", //
      "Pedal Hi-Hat", //
      "Low Tom", //
      "Open Hi-Hat",
      "Low-Mid Tom", //
      "Hi-Mid Tom", //
      "Crash Cymbal 1", //
      "High Tom", //
      "Ride Cymbal 1", //
      "Chinese Cymbal", //
      "Ride Bell", //
      "Tambourine", //
      "Splash Cymbal", //
      "Cowbell", //
      "Crash Cymbal 2", //
      "Vibraslap", //
      "Ride Cymbal 2", //
      "Hi Bongo", //
      "Low Bongo", //
      "Mute Hi Conga", //
      "Open Hi Conga", //
      "Low Conga", //
      "High Timbale", //
      "Low Timbale", //
      "High Agogo", //
      "Low Agogo", //
      "Cabasa", //
      "Maracas", //
      "Short Whistle", //
      "Long Whistle", //
      "Short Guiro", //
      "Long Guiro", //
      "Claves", //
      "Hi Wood Block", //
      "Low Wood Block", //
      "Mute Cuica", //
      "Open Cuica", //
      "Mute Triangle", //
      "Open Triangle" //
  };

  private static final String NA = "n/a";
  public static final String DRUM_SET = "Drums";
  public static final int DRUM_CHANNEL = 9;
  private static final Instruments instruments = new Instruments();
  private static ProgramTableModel programTableModel;

  private static final int COLUMN_COUNT = HEADINGS.length;
  private static final int ROW_COUNT = MidiConstants.MAX_PROGRAMS / COLUMN_COUNT;

  private String[] programNames = new String[MidiConstants.MAX_PROGRAMS];
  private int[] sortOrder = new int[MidiConstants.MAX_PROGRAMS];

  private Instruments()
  {
    initializeProgramNames();
    initializeSortOrder();
  }

  private void initializeProgramNames()
  {
    Synthesizer synthesizer = Catcher.getSynthesizer();
    Instrument[] instruments = synthesizer.getAvailableInstruments();

    for (int i = 0; i < instruments.length; i++)
    {
      Instrument instrument = instruments[i];
      Patch patch = instrument.getPatch();
      int bank = patch.getBank();
      if (bank == 0)
      {
        int program = patch.getProgram();
        programNames[program] = instrument.getName();
      }
    }
    synthesizer.close();
  }

  // The programNames are returned in General MIDI order by the
  // Synthesizer and must remain in that order if we are to identify
  // them properly, so in order to provide a sorted list, we just
  // sort the indexes of the program names. Unfortunately, the
  // standard sort methods, like Arrays.sort(int[]) don't work
  // with this approach, and creating a custom Comparator for
  // use with Arrays.sort(int[], Comparator<? super T>()) is cost
  // prohibitive because it uses Integers instead of ints.

  private void initializeSortOrder()
  {
    for (int i = 0; i < sortOrder.length; i++)
    {
      sortOrder[i] = i;
    }

    int limit = programNames.length - 1;
    boolean changes;
    do
    {
      changes = false;
      for (int i = 0, j = 1; i < limit; i++, j++)
      {
        if (programNames[sortOrder[i]].compareTo(programNames[sortOrder[j]]) > 0)
        {
          int t = sortOrder[i];
          sortOrder[i] = sortOrder[j];
          sortOrder[j] = t;
          changes = true;
        }
      }
    }
    while (changes);
  }

  public int getProgram(int sortedIndex)
  {
    return sortOrder[sortedIndex];
  }

  public int getSortedIndex(int program)
  {
    // TODO: Use a reverse map for performance
    for (int i = 0; i < sortOrder.length; i++)
    {
      if (sortOrder[i] == program)
      {
        return i;
      }
    }
    return 0;
  }

  public static String getProgramName(int program)
  {
    String programName = NA;

    if (0 <= program && program < instruments.programNames.length)
    {
      programName = instruments.programNames[program];
    }

    return programName;
  }

  public static String getDrumName(int key)
  {
    String drumName = "n/a";

    key -= GM_MIN_DRUM_KEY;

    if (key >= 0 && key < DRUMS.length)
    {
      drumName = DRUMS[key];
    }

    return drumName;
  }

  public ListModel createListModel()
  {
    return new ProgramListModel();
  }

  // Note: Cannot use a singleton ComboBoxModel because it manages the current selection.

  public ComboBoxModel createComboBoxModel()
  {
    return new ProgramComboBoxModel();
  }

  public static synchronized TableModel getTableModel()
  {
    if (programTableModel == null)
    {
      programTableModel = instruments.createProgramTableModel();
    }
    return programTableModel;
  }

  private ProgramTableModel createProgramTableModel()
  {
    return new ProgramTableModel();
  }

  public static int getInstrument(int row, int column)
  {
    return (column * ROW_COUNT) + row;
  }

  public static int getRowIndex(int program)
  {
    return program % ROW_COUNT;
  }

  public static int getColumnIndex(int program)
  {
    return program / Instruments.ROW_COUNT;
  }

  private class ProgramListModel implements ListModel
  {

    public void addListDataListener(ListDataListener l)
    {
    }

    public Object getElementAt(int index)
    {
      return programNames[sortOrder[index]];
    }

    public int getSize()
    {
      return programNames.length;
    }

    public void removeListDataListener(ListDataListener l)
    {
    }

  }

  private class ProgramComboBoxModel extends ProgramListModel implements ComboBoxModel
  {
    private Object selectedItem;

    public Object getSelectedItem()
    {
      return selectedItem;
    }

    public void setSelectedItem(Object anItem)
    {
      this.selectedItem = anItem;
    }

  }

  public class ProgramTableModel implements TableModel
  {

    public void addTableModelListener(TableModelListener l)
    {
    }

    public Class<?> getColumnClass(int columnIndex)
    {
      return String.class;
    }

    public int getColumnCount()
    {
      return COLUMN_COUNT;
    }

    public String getColumnName(int columnIndex)
    {
      return HEADINGS[columnIndex];
    }

    public int getRowCount()
    {
      return ROW_COUNT;
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
      return programNames[getInstrument(rowIndex, columnIndex)];
    }

    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
      return false;
    }

    public void removeTableModelListener(TableModelListener l)
    {
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
    }

  }

  public static ComboBoxModel getMidiDeviceComboBoxModel(boolean showInput, boolean showOutput)
  {
    DefaultComboBoxModel midiDeviceComboBoxModel = new DefaultComboBoxModel();

    Info[] deviceDescriptors = MidiSystem.getMidiDeviceInfo();

    for (Info deviceDescriptor : deviceDescriptors)
    {
      MidiDevice midiDevice = Catcher.getMidiDevice(deviceDescriptor);
      int maxReceivers = midiDevice.getMaxReceivers();
      int maxTransmitters = midiDevice.getMaxTransmitters();
      if ((showInput && maxTransmitters != 0) || (showOutput && maxReceivers != 0))
      {
        midiDeviceComboBoxModel.addElement(deviceDescriptor);
      }
    }

    return midiDeviceComboBoxModel;
  }
}
