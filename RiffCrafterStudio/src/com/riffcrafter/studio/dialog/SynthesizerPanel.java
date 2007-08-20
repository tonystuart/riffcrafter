// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.riffcrafter.common.midi.MidiConstants;
import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.studio.app.Editor;


public class SynthesizerPanel extends GridBagPanel
{
  private Editor editor;
  private int channel;
  private JCheckBox muteCheckBox;
  private JCheckBox soloCheckBox;

  public SynthesizerPanel()
  {
    muteCheckBox = new JCheckBox("Mute");
    muteCheckBox.addActionListener(new MuteAction());
    add(muteCheckBox, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=nw,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

    JButton muteAllButton = new JButton("Mute All");
    muteAllButton.addActionListener(new MuteAllAction());
    add(muteAllButton, "x=1,y=0,top=0,left=0,bottom=0,right=0,anchor=nw,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

    JButton unMuteAllButton = new JButton("Unmute All");
    unMuteAllButton.addActionListener(new UnMuteAllAction());
    add(unMuteAllButton, "x=2,y=0,top=0,left=0,bottom=0,right=0,anchor=nw,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

    JPanel spacer = new JPanel();
    add(spacer, "x=3,y=0,top=0,left=0,bottom=0,right=0,anchor=nw,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

    soloCheckBox = new JCheckBox("Solo");
    soloCheckBox.addActionListener(new SoloAction());
    add(soloCheckBox, "x=4,y=0,top=0,left=0,bottom=0,right=0,anchor=nw,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

    JButton soloAllButton = new JButton("Solo All");
    soloAllButton.addActionListener(new SoloAllAction());
    add(soloAllButton, "x=5,y=0,top=0,left=0,bottom=0,right=0,anchor=nw,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

    JButton unSoloAllButton = new JButton("Unsolo All");
    unSoloAllButton.addActionListener(new UnSoloAllAction());
    add(unSoloAllButton, "x=6,y=0,top=0,left=0,bottom=0,right=0,anchor=nw,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

    // Make all buttons the same size (grrr)
    Dimension size = unMuteAllButton.getPreferredSize();
    muteAllButton.setPreferredSize(size);
    soloAllButton.setPreferredSize(size);
    unSoloAllButton.setPreferredSize(size);
  }

  public void selectChannel(Editor editor, int channel)
  {
    this.editor = editor;
    this.channel = channel;
    if (editor != null)
    {
      muteCheckBox.setSelected(editor.isMute(channel));
      soloCheckBox.setSelected(editor.isSolo(channel));
    }
  }

  public class MuteAction implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      boolean isMute = muteCheckBox.isSelected();
      editor.setMute(channel, isMute);
    }
  }

  public class MuteAllAction implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      for (int channel = 0; channel < MidiConstants.MAX_CHANNELS; channel++)
      {
        editor.setMute(channel, true);
      }
      muteCheckBox.setSelected(true);
    }
  }

  public class UnMuteAllAction implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      for (int channel = 0; channel < MidiConstants.MAX_CHANNELS; channel++)
      {
        editor.setMute(channel, false);
      }
      muteCheckBox.setSelected(false);
    }
  }

  public class SoloAction implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      boolean isSolo = soloCheckBox.isSelected();
      editor.setSolo(channel, isSolo);
    }
  }

  public class SoloAllAction implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      for (int channel = 0; channel < MidiConstants.MAX_CHANNELS; channel++)
      {
        editor.setSolo(channel, true);
      }
      soloCheckBox.setSelected(true);
    }
  }

  public class UnSoloAllAction implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      for (int channel = 0; channel < MidiConstants.MAX_CHANNELS; channel++)
      {
        editor.setSolo(channel, false);
      }
      soloCheckBox.setSelected(false);
    }
  }

}
