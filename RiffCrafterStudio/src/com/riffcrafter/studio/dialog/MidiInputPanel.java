// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;
import javax.sound.midi.MidiDevice.Info;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;

import com.riffcrafter.common.midi.Catcher;
import com.riffcrafter.common.midi.Instruments;
import com.riffcrafter.common.midi.Midel;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.common.midi.Note;
import com.riffcrafter.common.midi.Midi.NoteBuilder;
import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.library.dialog.ImageButtonGroup;
import com.riffcrafter.studio.app.Editor;


public class MidiInputPanel extends GridBagPanel
{
  private static final int DEFAULT_MAXIMUM_GAP = 2000;
  private static final int DEFAULT_MIDI_RESOLUTION = 500000;

  private Editor editor;
  private int channel;

  private MidiDevice inputDevice;
  private JComboBox midiDeviceComboBox;
  private MidiInputControlPanel midiInputControlPanel;
  private MidiInputSettingsPanel midiInputSettingsPanel;

  public MidiInputPanel()
  {
    // Row 1: y = 0

    JLabel midiDeviceLabel = new JLabel("MIDI Input Device:");
    add(midiDeviceLabel, "x=0,y=0,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

    // Row 2: y = 1

    ComboBoxModel midiDeviceComboBoxModel = Instruments.getMidiDeviceComboBoxModel(true, false);
    midiDeviceComboBox = new JComboBox(midiDeviceComboBoxModel);
    add(midiDeviceComboBox, "x=0,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

    // Row 3: y = 2

    midiInputControlPanel = new MidiInputControlPanel();
    midiInputControlPanel.setBorder("titled: Control ");
    add(midiInputControlPanel, "x=0,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

    midiInputSettingsPanel = new MidiInputSettingsPanel();
    add(midiInputSettingsPanel, "x=1,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
  }

  public void record()
  {
    stop();
    Info deviceDescriptor = (Info)midiDeviceComboBox.getSelectedItem();
    inputDevice = Catcher.getMidiDevice(deviceDescriptor);
    Catcher.open(inputDevice);
    Transmitter transmitter = Catcher.getTransmitter(inputDevice);
    //    Midi midi = new Midi();
    //    NoteBuilder noteBuilder = midi.new NoteBuilder(Midi.DEFAULT_RESOLUTION);
    MidiInputReceiver midiInputReceiver = new MidiInputReceiver();
    transmitter.setReceiver(midiInputReceiver);
  }

  public void stop()
  {
    if (inputDevice != null && inputDevice.isOpen())
    {
      inputDevice.close();
    }
    inputDevice = null;
  }

  public void selectChannel(Editor editor, int channel)
  {
    this.editor = editor;
    this.channel = channel;
  }

  public class MidiInputControlPanel extends ImageButtonGroup
  {
    private static final int MONITOR = 1;
    private static final int RECORD = 2;
    private static final int STOP = 3;

    private MidiInputControlPanel()
    {
      super(24, 24);
      add("Recorder-Record-16x16.png", RECORD, "Start recording");
      add("Recorder-Stop-16x16.png", STOP, "Stop recording");
      addActionListener(new ControlPanelListener());
    }

    public class ControlPanelListener implements ActionListener
    {
      public void actionPerformed(ActionEvent e)
      {
        ImageButton button = (ImageButton)e.getSource();
        int value = button.getValue();

        switch (value)
        {
          case MONITOR:
            break;
          case RECORD:
            record();
            break;
          case STOP:
            stop();
            break;
        }
      }
    }

  }

  private class MidiInputSettingsPanel extends GridBagPanel
  {
    private JCheckBox currentChannelCheckBox;
    private JSpinner maximumGapSpinner;
    private JSpinner resolutionSpinner;

    private MidiInputSettingsPanel()
    {
      // Row 1: y = 0 (labels)

      JLabel maximumGapLabel = new JLabel("Limit gaps to (ms):");
      add(maximumGapLabel, "x=0,y=0,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel resolutionLabel = new JLabel("Input Resolution (ppqn):");
      add(resolutionLabel, "x=1,y=0,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      // Row 2: y = 1 (spinners)

      maximumGapSpinner = new JSpinner();
      maximumGapSpinner.setValue(DEFAULT_MAXIMUM_GAP);
      add(maximumGapSpinner, "x=0,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      resolutionSpinner = new JSpinner();
      resolutionSpinner.setValue(DEFAULT_MIDI_RESOLUTION);
      add(resolutionSpinner, "x=1,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      // Row 3: y = 2 (buttons)

      currentChannelCheckBox = new JCheckBox("Record to current channel");
      add(currentChannelCheckBox, "x=0,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JButton refreshDeviceListButton = new JButton("Refresh Device List");
      refreshDeviceListButton.addActionListener(new RefreshActionListener());
      add(refreshDeviceListButton, "x=1,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

    }

    public boolean isRecordToCurrentChannel()
    {
      return currentChannelCheckBox.isSelected();
    }

    public int getMaximumGap()
    {
      return ((Number)maximumGapSpinner.getValue()).intValue();
    }

    public long getResolution()
    {
      return ((Number)resolutionSpinner.getValue()).longValue();
    }

    public class RefreshActionListener implements ActionListener
    {
      public void actionPerformed(ActionEvent e)
      {
        midiDeviceComboBox.setModel(Instruments.getMidiDeviceComboBoxModel(true, false));
      }
    }

  }

  public class MidiInputReceiver implements Receiver
  {
    private Midi midi;
    private NoteBuilder noteBuilder;
    private long timeBase = -1;
    private long lastTime = -1;

    public MidiInputReceiver()
    {
      this.midi = new Midi();
      this.noteBuilder = midi.new NoteBuilder(Midi.DEFAULT_RESOLUTION);
    }

    public void close()
    {
    }

    // The send method manages two timepieces:
    //
    //  1. timeBase, in external device resolution, for external midi events
    //  2. thisTime/lastTime, in system milliseconds, for detecting pauses in input
    //
    // We use two timepieces to eliminate the need to make any assumptions about
    // the relation between the two timepieces.
    //
    // The call to editor.pasteRelative specifies false for the isResetFirstTick
    // argument. This preserves the Midel's tick, which is the delta from the last
    // pasteRelative. We specify true for isAdvanceOnTick which sets currentTick
    // the end of the pasted area.
    //
    // We use timeBase to keep track of the delta (in external device units)
    // between notes, so basically all MIDI time keeping is done using external
    // units which converted to our resolution so that Midels can be stored.
    
    public void send(MidiMessage message, long timeStamp)
    {
      if (editor == null)
      {
        return;
      }

      noteBuilder.processMessage(message, timeStamp);

      boolean isRecordToCurrentChannel = midiInputSettingsPanel.isRecordToCurrentChannel();
      long maximumGap = midiInputSettingsPanel.getMaximumGap();
      long resolution = midiInputSettingsPanel.getResolution();

      long thisTime = System.currentTimeMillis();

      if (lastTime != -1 && (thisTime - lastTime) > maximumGap)
      {
        timeBase = -1;
        long currentTick = editor.getCurrentTick();
        long gapTick = Midi.convertMillisToTicks(maximumGap);
        currentTick += gapTick;
        editor.setCurrentTick(currentTick);
      }

      Midi targetMidi = new Midi();

      for (Iterator<Midel> iterator = midi.getIterator(); iterator.hasNext();)
      {
        Midel midel = iterator.next().clone();

        if (isRecordToCurrentChannel)
        {
          midel.setChannel(channel);
        }

        long tick = midel.getTick();
        
        if (timeBase == -1)
        {
          timeBase = tick;
        }

        long deltaTick = tick - timeBase;
        timeBase = tick;

        long convertedTick = Midi.convertResolution(deltaTick, resolution);
        midel.setTick(convertedTick);

        if (midel instanceof Note)
        {
          Note note = (Note)midel;
          long duration = note.getDuration();
          timeBase += duration;
          duration = Midi.convertResolution(duration, resolution);
          note.setDuration(duration);
        }

        targetMidi.add(midel);
        iterator.remove();
      }

      if (targetMidi.size() > 0)
      {
        editor.pasteRelative(targetMidi, false, true, false, false);
      }
      
      lastTime = thisTime;
    }

  }

}
