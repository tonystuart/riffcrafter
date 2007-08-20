// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

public class Channel
{
  public static final int FIRST_CHANNEL_INDEX = 0;
  public static final int LAST_CHANNEL_INDEX = 15;
  
  public static final int FIRST_CHANNEL_NUMBER = 1;
  public static final int LAST_CHANNEL_NUMBER = 16;
  
  public static int getChannelNumber(int channel)
  {
    return channel + 1;
  }
  
  public static int getChannelIndex(int channelNumber)
  {
    return channelNumber - 1;
  }
}
