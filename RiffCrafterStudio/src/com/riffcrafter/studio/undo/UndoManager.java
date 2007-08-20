// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.undo;

import java.util.Stack;

import com.riffcrafter.studio.app.Editor;


public class UndoManager
{
  private int index = -1;
  private boolean inUndoRedo;
  private Stack<UndoableEdit> stack = new Stack<UndoableEdit>();
  private long lastTime = System.currentTimeMillis();
  private Editor editor;
  private long currentTick;

  public UndoManager(Editor editor)
  {
    this.editor = editor;
  }
  
  public void addEdit(UndoableEdit undoableEdit)
  {
    if (!inUndoRedo)
    {
      undoableEdit.setCurrentTick(editor.getCurrentTick());
      while ((stack.size() - 1) > index)
      {
        stack.pop();
      }
      long time = System.currentTimeMillis();
      if (index >= 0 && time < (lastTime + 500))
      {
        UndoableEdit previousEdit = stack.get(index);
        if (!(previousEdit instanceof CompoundEdit))
        {
          CompoundEdit compoundEdit = new CompoundEdit();
          compoundEdit.add(previousEdit);
          stack.set(index, compoundEdit);
          previousEdit = compoundEdit;
        }
        ((CompoundEdit)previousEdit).add(undoableEdit);
      }
      else
      {
        stack.push(undoableEdit);
        index++;
      }
      lastTime = time;
    }
  }

  public boolean redo()
  {
    if (index == (stack.size() - 1))
    {
      return false;
    }

    index++;
    UndoableEdit undoableEdit = stack.get(index);
    currentTick = undoableEdit.getCurrentTick();
    inUndoRedo = true;
    undoableEdit.redo();
    inUndoRedo = false;
    return true;
  }

  public boolean undo()
  {
    if (index < 0)
    {
      return false;
    }

    UndoableEdit undoableEdit = stack.get(index);
    currentTick = undoableEdit.getCurrentTick();
    inUndoRedo = true;
    undoableEdit.undo();
    inUndoRedo = false;
    index--;
    return true;
  }

  public int getUndoIndex()
  {
    return index;
  }

  public long getCurrentTick()
  {
    return currentTick;
  }

}
