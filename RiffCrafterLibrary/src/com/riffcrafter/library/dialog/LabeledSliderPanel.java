// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.dialog;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class LabeledSliderPanel extends GridBagPanel
{
  private JLabel label;
  private JTextField textField;
  private JSlider slider;

  public LabeledSliderPanel(String title, int minimum, int maximum, int initial)
  {
    label = new JLabel(title);
    add(label, "x=0,y=0,top=0,left=0,bottom=0,right=5,anchor=e,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

    textField = new JTextField();
    textField.setColumns(5);
    textField.addKeyListener(new TextFieldKeyListener());
    add(textField, "x=1,y=0,top=0,left=0,bottom=0,right=0,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

    slider = new JSlider(minimum, maximum, initial);
    slider.addChangeListener(new SliderChangeListener());
    add(slider, "x=0,y=1,top=0,left=0,bottom=0,right=0,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");
  }

  public int getValue()
  {
    String text = textField.getText();
    int value = Integer.parseInt(text);
    return value;
  }

  void setValue(int value)
  {
    slider.setValue(value); // change listener propagates to text field
  }

  public JLabel getLabel()
  {
    return label;
  }

  public void setLabel(JLabel label)
  {
    this.label = label;
  }

  public JSlider getSlider()
  {
    return slider;
  }

  public void setSlider(JSlider slider)
  {
    this.slider = slider;
  }

  public JTextField getTextField()
  {
    return textField;
  }

  public void setTextField(JTextField textField)
  {
    this.textField = textField;
  }

  private class TextFieldKeyListener implements KeyListener
  {
    public void keyPressed(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void keyTyped(KeyEvent e)
    {
      int value = getValue();
      slider.setValue(value);
    }
  }

  public class SliderChangeListener implements ChangeListener
  {
    public void stateChanged(ChangeEvent e)
    {
      int value = (int)slider.getValue();
      String text = Integer.toString(value);
      textField.setText(text);
    }
  }

}
