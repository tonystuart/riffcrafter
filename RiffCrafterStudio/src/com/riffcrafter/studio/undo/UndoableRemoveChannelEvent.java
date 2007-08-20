// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.undo;

import com.riffcrafter.common.midi.ChannelEvent;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.common.midi.Note;

public class UndoableRemoveChannelEvent extends UndoableEdit
{
  private Midi midi;
  private ChannelEvent channelEvent;

  public UndoableRemoveChannelEvent(Midi midi, ChannelEvent channelEvent)
  {
    this.midi = midi;
    this.channelEvent = channelEvent;
  }

  public void redo()
  {
    midi.remove(channelEvent);
  }

  public void undo()
  {
    midi.add(channelEvent);
  }
}
