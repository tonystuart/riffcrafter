// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.undo;

public abstract class UndoableEdit
{
  
  private long currentTick;

  public abstract void undo();

  public abstract void redo();

  public void setCurrentTick(long currentTick)
  {
    this.currentTick = currentTick;
  }
  
  public long getCurrentTick()
  {
    return currentTick;
  }

}
