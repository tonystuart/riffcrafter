// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.util;

import java.util.NavigableSet;

public class Navigator<E>
{
  private NavigableSet<E> map;
  private E next;

  public Navigator(NavigableSet<E> map)
  {
    this.map = map;
    this.next = null;
  }

  public boolean hasNext()
  {
    return next == null ? map.size() > 0 : map.higher(next) != null;
  }

  public boolean hasPrevious()
  {
    return next == null ? false : map.lower(next) != null;
  }

  public E next()
  {
    if (next == null)
    {
      next = map.first();
    }
    else
    {
      next = map.higher(next);
    }
    return next;
  }

  public E previous()
  {
    next = map.lower(next);
    return next;
  }
}
