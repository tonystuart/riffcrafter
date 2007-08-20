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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

import com.riffcrafter.library.util.Resources;


public class ImageButtonGroup extends GridBagPanel
{
  private int buttonWidth;
  private int buttonHeight;

  private int value;
  private ButtonGroup buttonGroup = new ButtonGroup();
  private ButtonActionListener buttonActionListener = new ButtonActionListener();

  public ImageButtonGroup(int buttonWidth, int buttonHeight)
  {
    this.buttonWidth = buttonWidth;
    this.buttonHeight = buttonHeight;
  }

  public void add(String imageName, int duration, String toolTipText)
  {
    add(imageName, duration, toolTipText, false);
  }

  public void addDefault(String imageName, int duration, String toolTipText)
  {
    add(imageName, duration, toolTipText, true);
  }

  public void add(String imageName, int duration, String toolTipText, boolean isDefault)
  {
    ImageButton imageButton = new ImageButton(imageName, duration, toolTipText);
    imageButton.setFocusable(false);
    buttonGroup.add(imageButton);
    add(imageButton, "fill=n,weightx=0");
    if (isDefault)
    {
      imageButton.doClick();
    }
  }

  public void addActionListener(ActionListener actionListener)
  {
    Enumeration<AbstractButton> enumeration = buttonGroup.getElements();
    while (enumeration.hasMoreElements())
    {
      AbstractButton button = enumeration.nextElement();
      button.addActionListener(actionListener);
    }
  }
  
  public int getValue()
  {
    return value;
  }

  public void setValue(int value)
  {
    this.value = value;
  }

  public void clickValue(int value)
  {
    for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements();)
    {
      ImageButton button = (ImageButton)buttons.nextElement();
      if (button.value == value)
      {
        button.doClick();
        return;
      }
    }
  }

  public class ImageButton extends JToggleButton
  {
    private int value;

    private ImageButton(String imageName, final int value, String toolTipText)
    {
      this.value = value;
      ImageIcon image = Resources.getIconByFileName(imageName);
      setIcon(image);
      setToolTipText(toolTipText);
      setRequestFocusEnabled(false);
      Dimension size = new Dimension(buttonWidth, buttonHeight);
      setPreferredSize(size);
      setMaximumSize(size); // very important to set both maxima and minima
      setMinimumSize(size); // so grid bag can make best layout choices
      addActionListener(buttonActionListener);
    }

    public int getValue()
    {
      return value;
    }
  }

  private class ButtonActionListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      ImageButton button = (ImageButton)e.getSource();
      setValue(button.value);
    }
  }
}
