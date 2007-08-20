// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.midi;

import java.awt.EventQueue;
import java.awt.event.AdjustmentEvent;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.MidiDevice.Info;

import com.riffcrafter.common.midi.Catcher;
import com.riffcrafter.common.midi.MidiConstants;
import com.riffcrafter.studio.app.Editor;
import com.riffcrafter.studio.app.Studio;
import com.riffcrafter.studio.app.Editor.Bridger;

/**
 * This class does not know about class Midi. Let's keep it that way.
 * 
 * @author Tony
 *
 */
public class Player
{
  private Sequencer sequencer;
  private Synthesizer synthesizer;
  private boolean started;
  private Studio studio;
  private Editor editor;
  private Bridger bridger;

  private MidiReceiver midiReceiver = new MidiReceiver();
  private MidiMetaEventListener midiMetaEventListener = new MidiMetaEventListener();
  private long loopStart;

  public Player(Studio studio)
  {
    try
    {
      this.studio = studio;

      sequencer = MidiSystem.getSequencer(false);
      synthesizer = MidiSystem.getSynthesizer();
      synthesizer.open();

      // Uncomment the following one (1) line to use standard Java Sound synthesizer:
      sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());

      // Uncomment the following three (3) lines to use Microsoft GS Wavetable SW Synth:
      // MidiDevice msSynthesizer = getMidiDevice("Microsoft GS Wavetable SW Synth");
      // msSynthesizer.open();
      // sequencer.getTransmitter().setReceiver(msSynthesizer.getReceiver());

      sequencer.addMetaEventListener(midiMetaEventListener);
      sequencer.open();
      sequencer.getTransmitter().setReceiver(midiReceiver);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  public MidiDevice getMidiDevice(String name)
  {
    Info[] deviceDescriptors = MidiSystem.getMidiDeviceInfo();

    for (Info deviceDescriptor : deviceDescriptors)
    {
      MidiDevice midiDevice = Catcher.getMidiDevice(deviceDescriptor);
      int maxReceivers = midiDevice.getMaxReceivers();
      int maxTransmitters = midiDevice.getMaxTransmitters();
      if (deviceDescriptor.getName().equals(name))
      {
        return midiDevice;
      }
    }
    return null;
  }

  public void play(Sequence sequence, long tickPosition, Editor editor, Bridger bridger)
  {
    this.editor = editor;
    this.bridger = bridger;
    if (bridger != null)
    {
      bridger.start();
    }
    if (tickPosition >= sequence.getTickLength())
    {
      tickPosition = 0;
    }
    
    // Workground bug in the Java RealTimeSequencer. Note MidiUtils.tick2index
    // does a binary search on tick to find the track read position. This doesn't
    // work when there is more than one event at that tick, because it may not
    // find the first one, so any occurring before it will not be processed.
    tickPosition = Math.max(0, tickPosition - 1);

    synchronizeSynthesizer(editor);
    Catcher.setSequence(sequencer, sequence);
    sequencer.setTickPosition(tickPosition);
    sequencer.start();
    this.loopStart = tickPosition; // in case we repeat
    long tickLength = sequence.getTickLength();
    setLoop(studio.isRepeat() && tickLength > 0);
    started = true;
  }

  public void play(Editor editor, int channel, int key, int velocity)
  {
    stop();
    synchronizeSynthesizer(editor);
    MidiChannel[] channels = synthesizer.getChannels();
    channels[channel].noteOn(key, velocity);
  }

  public void stop(int channel, int key)
  {
    synthesizer.getChannels()[channel].noteOff(key);
  }

  public void stop()
  {
    if (bridger != null)
    {
      bridger.stop();
    }
    sequencer.stop();
    sequencer.setLoopCount(0);
    resetSynthesizer();
    started = false;
  }

  public boolean isPlaying(Editor editor)
  {
    return sequencer.isRunning() && this.editor == editor;
  }

  public boolean isPlaying()
  {
    return sequencer.isRunning();
  }

  public Sequencer getSequencer()
  {
    return sequencer;
  }

  public Synthesizer getSynthesizer()
  {
    return synthesizer;
  }

  public void setLoop(boolean isLoop)
  {
    sequencer.setLoopStartPoint(loopStart);
    sequencer.setLoopCount(isLoop ? Sequencer.LOOP_CONTINUOUSLY : 0);
  }

  private void updateCurrentTick()
  {
    if (bridger != null)
    {
      bridger.setCurrentTick(sequencer.getTickPosition());
    }
  }

  private void synchronizeSynthesizer(Editor editor)
  {
    MidiChannel[] channels = synthesizer.getChannels();
    for (int i = 0; i < MidiConstants.MAX_CHANNELS; i++)
    {
      channels[i].setMute(editor.isMute(i));
      channels[i].setSolo(editor.isSolo(i));
      channels[i].programChange(editor.getInstrument(i));
    }
  }

  public void resetSynthesizer()
  {
    MidiChannel[] channels = synthesizer.getChannels();
    for (int i = 0; i < MidiConstants.MAX_CHANNELS; i++)
    {
      channels[i].setMute(false);
      channels[i].setSolo(false);
      channels[i].programChange(i);
    }
  }

  public boolean isMute(int channel)
  {
    return synthesizer.getChannels()[channel].getMute();
  }

  public void setMute(int channel, boolean isMute)
  {
    synthesizer.getChannels()[channel].setMute(isMute);
  }

  public boolean isSolo(int channel)
  {
    return synthesizer.getChannels()[channel].getSolo();
  }

  public void setSolo(int channel, boolean isSolo)
  {
    synthesizer.getChannels()[channel].setSolo(isSolo);
  }

  public void onScrollerChange(AdjustmentEvent e)
  {
    if (sequencer.isRunning() && e.getValueIsAdjusting())
    {
      sequencer.stop();
    }
    else
    {
      long tick = e.getValue();
      sequencer.setTickPosition(tick);
      if (started & !sequencer.isRunning())
      {
        sequencer.start();
      }
    }
  }

  private class MidiReceiver implements Receiver
  {
    public void send(final MidiMessage message, final long timeStamp)
    {
      EventQueue.invokeLater(new Runnable()
      {
        public void run()
        {
          updateCurrentTick();
        }
      });
    }

    public void close()
    {
    }

  }

  private class MidiMetaEventListener implements MetaEventListener
  {
    public void meta(MetaMessage event)
    {
      if (event.getType() == MidiConstants.END_OF_TRACK)
      {
        updateCurrentTick();
        studio.stop();
      }
    }
  }

}
