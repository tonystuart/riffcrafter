// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

public class DialogBox extends JDialog
{
  private Random random = new Random();
  private JInternalFrame internalFrame;
  private DialogBoxInternalFrameListener dialogBoxInternalFrameListener = new DialogBoxInternalFrameListener();
  private DialogBoxWindowListener dialogBoxWindowListener = new DialogBoxWindowListener();

  public DialogBox(final JInternalFrame internalFrame)
  {
    super(JOptionPane.getFrameForComponent(internalFrame));
    this.internalFrame = internalFrame;
    internalFrame.addInternalFrameListener(dialogBoxInternalFrameListener);
    addWindowListener(dialogBoxWindowListener);
  }

  public void display()
  {
    pack();
    setLocationRelativeTo(internalFrame);
    setVisible(true);
  }

  public void display(Dimension maximumSize)
  {
    pack();
    Dimension size = getSize();
    size.width = Math.min(size.width, maximumSize.width);
    size.height = Math.min(size.height, maximumSize.height);
    setSize(size);
    setLocationRelativeTo(internalFrame);
    setVisible(true);
  }

  @Override
  public void setLocationRelativeTo(Component c)
  {
    super.setLocationRelativeTo(c);
    Point location = getLocation();
    location.x += (1 + random.nextInt(5)) * 5;
    location.y += (1 + random.nextInt(5)) * 5;
    setLocation(location);
  }

  protected class DialogBoxInternalFrameListener implements InternalFrameListener
  {
    public void internalFrameActivated(InternalFrameEvent e)
    {
      if (!internalFrame.isIcon())
      {
        setVisible(true);
      }
    }

    public void internalFrameClosed(InternalFrameEvent e)
    {
      dispose();
    }

    public void internalFrameClosing(InternalFrameEvent e)
    {
    }

    public void internalFrameDeactivated(InternalFrameEvent e)
    {
      setVisible(false);
    }

    public void internalFrameDeiconified(InternalFrameEvent e)
    {
      setVisible(true);
    }

    public void internalFrameIconified(InternalFrameEvent e)
    {
      setVisible(false);
    }

    public void internalFrameOpened(InternalFrameEvent e)
    {
    }
  }

  public class DialogBoxWindowListener extends WindowAdapter
  {
    public void windowClosed(WindowEvent e)
    {
      // Remove reference to DialogBox from InternalFrame
      internalFrame.removeInternalFrameListener(dialogBoxInternalFrameListener);
    }

  }

}
