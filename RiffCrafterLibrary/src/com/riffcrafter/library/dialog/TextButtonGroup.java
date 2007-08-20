// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.dialog;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;

public class TextButtonGroup extends GridBagPanel
{
  private int value;
  private ButtonGroup buttonGroup = new ButtonGroup();

  public void add(String buttonText, int duration, String toolTipText)
  {
    add(buttonText, duration, toolTipText, false);
  }

  public void addDefault(String buttonText, int duration, String toolTipText)
  {
    add(buttonText, duration, toolTipText, true);
  }

  public void add(String buttonText, int value, String toolTipText, boolean isDefault)
  {
    TextButton textButton = new TextButton(buttonText, value, toolTipText);
    textButton.setFocusable(false);
    buttonGroup.add(textButton);
    add(textButton, "fill=n,weightx=0");
    if (isDefault)
    {
      textButton.doClick();
    }
  }

  public int getValue()
  {
    return value;
  }

  void setValue(int value)
  {
    this.value = value;
  }
  
  private class TextButton extends JToggleButton
  {

    private TextButton(String buttonText, final int value, String toolTipText)
    {
      Font font = getFont().deriveFont(Font.BOLD | Font.ITALIC);
      setFont(font);
      setText(buttonText);
      setToolTipText(toolTipText);
      setRequestFocusEnabled(false);
      Dimension size = new Dimension(24, 24);
      setPreferredSize(size);
      setMaximumSize(size);
      addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          setValue(value);
        }
      });
    }
  }

}
