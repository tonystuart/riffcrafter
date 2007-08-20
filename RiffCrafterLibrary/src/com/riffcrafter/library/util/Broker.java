// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.util;

import java.util.ArrayList;

public class Broker
{
  private ArrayList<Listener> listeners = new ArrayList<Listener>();

  public void subscribe(Listener listener)
  {
    listeners.add(listener);
  }

  public void publish(Object value, Object source)
  {
    for (Listener listener : listeners)
    {
      listener.notify(value, source);
    }
  }

  public interface Listener
  {
    public void notify(Object event, Object source);
  }

}
