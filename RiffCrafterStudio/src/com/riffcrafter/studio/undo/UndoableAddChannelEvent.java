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

public class UndoableAddChannelEvent extends UndoableEdit
{
  private Midi midi;
  private ChannelEvent channelEvent;

  public UndoableAddChannelEvent(Midi midi, ChannelEvent channelEvent)
  {
    this.midi = midi;
    this.channelEvent = channelEvent;
  }

  public void redo()
  {
    midi.add(channelEvent);
  }

  public void undo()
  {
    midi.remove(channelEvent);
  }

}
