// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.test;

import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

public class VanillaJavaSoundSequencer
{

  public static void main(String[] args)
  {
    try
    {
      File file = new File("midi/Rock/Elton John/Daniel.mid");
      Sequence sequence = MidiSystem.getSequence(file);
      Sequencer sequencer = MidiSystem.getSequencer();
      sequencer.open();
      sequencer.setSequence(sequence);
      sequencer.start();
    }
    catch (InvalidMidiDataException e)
    {
      throw new RuntimeException(e);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
    catch (MidiUnavailableException e)
    {
      throw new RuntimeException(e);
    }
    
  }

}
