// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.dialog;

import javax.swing.BorderFactory;

import com.riffcrafter.common.midi.KeyboardPanel;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.common.midi.MidiConstants;
import com.riffcrafter.common.midi.Note;
import com.riffcrafter.common.midi.KeyboardPanel.NoteOffEvent;
import com.riffcrafter.common.midi.KeyboardPanel.NoteOnEvent;
import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.library.dialog.ImageButtonGroup;
import com.riffcrafter.library.util.Broker.Listener;
import com.riffcrafter.studio.app.Editor;



public class KeyboardInputPanel extends GridBagPanel
{
  public static final int DEFAULT_VELOCITY = 63;
  public static final int DEFAULT_DURATION = MidiConstants.DURATION_QUARTER_NOTE;
  public static final int DEFAULT_ARTICULATION = ArticulationPanel.ARTICULATION_NORMAL;

  private static int AS_PLAYED = -1;

  private Editor editor;
  private int channel;

  private KeyboardPanel keyboardPanel;
  private VelocityPanel velocityPanel;
  private DurationPanel durationPanel;
  private ArticulationPanel articulationPanel;

  public KeyboardInputPanel()
  {
    keyboardPanel = new KeyboardPanel();
    keyboardPanel.subscribe(new MidiKeyboardListener());
    add(keyboardPanel, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

    velocityPanel = new VelocityPanel();
    velocityPanel.setBorder(BorderFactory.createTitledBorder(" Volume "));
    add(velocityPanel, "x=0,y=1,top=0,left=0,bottom=0,right=0,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

    durationPanel = new DurationPanel();
    durationPanel.setBorder(BorderFactory.createTitledBorder(" Duration "));
    add(durationPanel, "x=2,y=1,top=0,left=0,bottom=0,right=0,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

    articulationPanel = new ArticulationPanel();
    articulationPanel.setBorder(BorderFactory.createTitledBorder(" Articulation "));
    add(articulationPanel, "x=3,y=1,top=0,left=0,bottom=0,right=0,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
  }

  public void selectChannel(Editor editor, int channel)
  {
    this.editor = editor;
    this.channel = channel;
    velocityPanel.selectChannel();
    durationPanel.selectChannel();
    articulationPanel.selectChannel();
  }

  public static final long articulate(long duration, int articulation)
  {
    duration = (articulation * duration) / 100;
    return duration;
  }

  private class MidiKeyboardListener implements Listener
  {
    private Midi midi = new Midi();
    private long maxDuration;
    private long lastTime;

    @SuppressWarnings("unchecked")
    public void notify(Object value, Object source)
    {
      if (editor != null)
      {
        if (value instanceof NoteOnEvent)
        {
          NoteOnEvent noteOnEvent = (NoteOnEvent)value;
          processNoteOn(noteOnEvent);
        }
        else if (value instanceof NoteOffEvent)
        {
          NoteOffEvent noteOffEvent = (NoteOffEvent)value;
          processNoteOff(noteOffEvent);
        }
      }
    }

    private void processNoteOn(NoteOnEvent noteOnEvent)
    {
      int key = noteOnEvent.getKey();
      int velocity = velocityPanel.getValue();
      long duration = durationPanel.getValue();
      if (noteOnEvent.isChord() || duration == AS_PLAYED)
      {
        addArticulationAsPlayed();
        editor.play(channel, key, velocity);
      }
      else
      {
        long adjustedDuration = articulationPanel.articulate(duration);
        long gap = duration - adjustedDuration;
        Note note = new Note(channel, key, velocity, 0, adjustedDuration);
        editor.addNoteRelative(note, gap, true);
        lastTime = 0;
      }
    }

    private void addArticulationAsPlayed()
    {
      if (lastTime != 0)
      {
        long thisTime = System.currentTimeMillis();
        long gap = Math.min(2000, thisTime - lastTime);
        long ticks = Midi.convertMillisToTicks(gap);
        long currentTick = editor.getCurrentTick();
        currentTick += ticks;
        editor.setCurrentTick(currentTick);
      }
    }

    private void processNoteOff(NoteOffEvent noteOffEvent)
    {
      int key = noteOffEvent.getKey();
      int velocity = velocityPanel.getValue();
      long duration = durationPanel.getValue();
      if (noteOffEvent.isChord() || duration == AS_PLAYED)
      {
        if (duration == AS_PLAYED)
        {
          duration = noteOffEvent.getDuration();
          duration = Midi.convertMillisToTicks(duration);
          lastTime = System.currentTimeMillis();
        }
        duration = articulationPanel.articulate(duration);
        maxDuration = Math.max(maxDuration, duration);
        long tick = editor.getCurrentTick();
        Note note = new Note(channel, key, velocity, tick, duration);
        midi.add(note);
        editor.stop(channel, key);
        boolean isFinalKey = noteOffEvent.isFinalKey();
        if (isFinalKey)
        {
          editor.pasteAbsolute(midi, false);
          tick += maxDuration;
          editor.setCurrentTick(tick);
          midi = new Midi();
          maxDuration = 0;
        }
      }
    }

  }

  private class VelocityPanel extends ImageButtonGroup
  {
    private VelocityPanel()
    {
      super(24, 24);
      add("Velocity-PP-16x16.png", 2, "Pianissimo (very quiet)");
      add("Velocity-P-16x16.png", 25, "Piano (quiet)");
      add("Velocity-MP-16x16.png", 50, "Mezzo piano (medium quiet)");
      add("Velocity-M-16x16.png", DEFAULT_VELOCITY, "Mezzo (medium)");
      add("Velocity-MF-16x16.png", 75, "Mezzo forte (medium loud)");
      add("Velocity-F-16x16.png", 100, "Forte (loud)");
      add("Velocity-FF-16x16.png", 125, "Fortissimo (very loud)");
    }

    @Override
    public void setValue(int velocity)
    {
      if (editor != null)
      {
        editor.setVelocity(channel, velocity);
      }
      super.setValue(velocity);
    }

    public void selectChannel()
    {
      if (editor != null)
      {
        int velocity = editor.getVelocity(channel);
        clickValue(velocity);
      }
    }

  }

  private class DurationPanel extends ImageButtonGroup
  {
    private DurationPanel()
    {
      super(24, 24);
      add("Note-16n-16x16.png", MidiConstants.DURATION_SIXTEENTH_NOTE, "16th note, 62 ticks");
      add("Note-08n-16x16.png", MidiConstants.DURATION_EIGHTH_NOTE, "8th note, 125 ticks");
      add("Note-04n-16x16.png", DEFAULT_DURATION, "Quarter note, 250 ticks");
      add("Note-02n-16x16.png", MidiConstants.DURATION_HALF_NOTE, "Half note, 500 ticks");
      add("Note-01n-16x16.png", MidiConstants.DURATION_WHOLE_NOTE, "Whole note, 1000 ticks");
      add("Note-00n-16x16.png", AS_PLAYED, "As played on keyboard");
    }

    @Override
    public void setValue(int duration)
    {
      if (editor != null)
      {
        editor.setDuration(channel, duration);
      }
      super.setValue(duration);
    }

    public void selectChannel()
    {
      if (editor != null)
      {
        int duration = editor.getDuration(channel);
        clickValue(duration);
      }
    }
  }

  // See http://en.wikipedia.org/wiki/Modern_musical_symbols

  private class ArticulationPanel extends ImageButtonGroup
  {
    private static final int ARTICULATION_STACCATO = 40;
    private static final int ARTICULATION_NORMAL = 80;
    private static final int ARTICULATION_TENUTO = 100;

    private ArticulationPanel()
    {
      super(24, 24);
      add("Articulation-Staccato-16x16.png", ARTICULATION_STACCATO, "Staccato (40%)");
      add("Articulation-Normal-16x16.png", ARTICULATION_NORMAL, "Normal (80%)");
      add("Articulation-Tenuto-16x16.png", ARTICULATION_TENUTO, "Tenuto (100%)");
    }

    public long articulate(long duration)
    {
      int articulation = getValue();
      duration = KeyboardInputPanel.articulate(duration, articulation);
      return duration;
    }

    @Override
    public void setValue(int articulation)
    {
      if (editor != null)
      {
        editor.setArticulation(channel, articulation);
      }
      super.setValue(articulation);
    }

    public void selectChannel()
    {
      if (editor != null)
      {
        int articulation = editor.getArticulation(channel);
        clickValue(articulation);
      }
    }

  }

}
