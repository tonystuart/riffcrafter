// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.undo;

import java.util.ArrayList;

public class CompoundEdit extends UndoableEdit
{
  protected ArrayList<UndoableEdit> undoableEdits = new ArrayList<UndoableEdit>();

  public void undo()
  {
    int index = undoableEdits.size() - 1;
    while (index >= 0)
    {
      undoableEdits.get(index--).undo();
    }
  }

  public void redo()
  {
    for (UndoableEdit undoableEdit : undoableEdits)
    {
      undoableEdit.redo();
    }
  }

  public void add(UndoableEdit undoableEdit)
  {
    if (undoableEdits.size() == 0)
    {
      setCurrentTick(undoableEdit.getCurrentTick());
    }
    undoableEdits.add(undoableEdit);
  }

}
