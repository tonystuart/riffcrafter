// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;
import javax.sound.midi.MidiDevice.Info;

public class Catcher
{
  public static Synthesizer getSynthesizer()
  {
    try
    {
      Synthesizer synthesizer = MidiSystem.getSynthesizer();
      synthesizer.open();
      return synthesizer;
    }
    catch (MidiUnavailableException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static Sequencer getSequencer()
  {
    try
    {
      return MidiSystem.getSequencer();
    }
    catch (MidiUnavailableException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static void openSequencer(Sequencer sequencer)
  {
    try
    {
      sequencer.open();
    }
    catch (MidiUnavailableException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static void setReceiver(Sequencer sequencer, Receiver midiReceiver)
  {
    try
    {
      sequencer.getTransmitter().setReceiver(midiReceiver);
    }
    catch (MidiUnavailableException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static void setSequence(Sequencer sequencer, Sequence sequence)
  {
    try
    {
      sequencer.setSequence(sequence);
    }
    catch (InvalidMidiDataException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static Sequence getSequence(File file)
  {
    try
    {
      return MidiSystem.getSequence(file);
    }
    catch (InvalidMidiDataException e)
    {
      throw new RuntimeException(e);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static String getCanonicalPath(File file)
  {
    try
    {
      return file.getCanonicalPath();
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static Sequence getSequence(InputStream inputStream)
  {
    try
    {
      return MidiSystem.getSequence(inputStream);
    }
    catch (InvalidMidiDataException e)
    {
      throw new RuntimeException(e);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static void setMessage(ShortMessage message, int command, int channel, int data1, int data2)
  {
    try
    {
      message.setMessage(command, channel, data1, data2);
    }
    catch (InvalidMidiDataException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static void setMessage(MetaMessage message, int type, byte[] data, int length)
  {
    try
    {
      message.setMessage(type, data, length);
    }
    catch (InvalidMidiDataException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static void setMessage(SysexMessage message, int type, byte[] data, int length)
  {
    try
    {
      message.setMessage(type, data, length);
    }
    catch (InvalidMidiDataException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static void write(Sequence sequence, int fileType, File file)
  {
    try
    {
      MidiSystem.write(sequence, fileType, file);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static void write(Sequence sequence, int fileType, OutputStream outputStream)
  {
    try
    {
      MidiSystem.write(sequence, fileType, outputStream);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static MidiDevice getMidiDevice(Info deviceDescriptor)
  {
    try
    {
      return MidiSystem.getMidiDevice(deviceDescriptor);
    }
    catch (MidiUnavailableException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static void open(MidiDevice inputDevice)
  {
    try
    {
      inputDevice.open();
    }
    catch (MidiUnavailableException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static Transmitter getTransmitter(MidiDevice inputDevice)
  {
    try
    {
      return inputDevice.getTransmitter();
    }
    catch (MidiUnavailableException e)
    {
      throw new RuntimeException(e);
    }
  }

}
