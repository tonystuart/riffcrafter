// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.dialog;

import javax.swing.JTabbedPane;

import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.studio.app.Editor;

public class ControlPanel extends GridBagPanel
{
  private ChannelPanel channelPanel;
  private CommandPanel commandPanel;
  private JamPanel jamPanel;

  public ControlPanel()
  {
    channelPanel = new ChannelPanel();
    commandPanel = new CommandPanel();
    jamPanel = new JamPanel();

    JTabbedPane rightTabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    rightTabbedPane.addTab("Channels", channelPanel);
    rightTabbedPane.addTab("Commands", commandPanel);
    rightTabbedPane.addTab("Jam Sessions", jamPanel);

    add(rightTabbedPane, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=e,fill=b,weightx=1,weighty=1,gridwidth=1,gridheight=1");
    // setEnabled(this, false);
  }

  public void selectEditor(Editor editor)
  {
    channelPanel.selectEditor(editor);
    commandPanel.selectEditor(editor);
    jamPanel.selectEditor(editor);
    // setEnabled(this, editor != null);
  }

  public void selectChannel(int channel)
  {
    channelPanel.selectChannel(channel);
  }
  
  public void onNotatorInstrumentChange(int channel, int instrument)
  {
    channelPanel.onNotatorInstrumentChange(channel, instrument);
  }

  public void onMidiChange()
  {
    channelPanel.onMidiChange();
  }

  public void onMidiSelect(Midi selection)
  {
    channelPanel.onMidiSelect(selection);
  }
}
