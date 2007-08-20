// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

import javax.sound.midi.MetaMessage;

public class RiffCrafterEvent extends MetaEvent
{
  public static final String RIFFCRAFTER_SIGNATURE = "RiffCrafter Studio 1.0";
  
  public RiffCrafterEvent(long tick, int track)
  {
    super(tick, new MetaMessage());
    Catcher.setMessage(message, MidiConstants.MM_VENDOR_SPECIFIC, RIFFCRAFTER_SIGNATURE.getBytes(), RIFFCRAFTER_SIGNATURE.length());
  }

}
