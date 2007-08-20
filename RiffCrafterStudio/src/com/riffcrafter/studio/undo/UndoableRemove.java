// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.undo;

import com.riffcrafter.common.midi.Midel;
import com.riffcrafter.common.midi.Midi;

public class UndoableRemove extends UndoableEdit
{
  private Midi midi;
  private Midel midel;

  public UndoableRemove(Midi midi, Midel midel)
  {
    this.midi = midi;
    this.midel = midel;
  }

  public void redo()
  {
    midi.remove(midel);
  }

  public void undo()
  {
    midi.add(midel);
  }

}
