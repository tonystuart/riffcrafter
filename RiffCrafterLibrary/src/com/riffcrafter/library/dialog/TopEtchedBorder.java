// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.Border;

public class TopEtchedBorder implements Border
{
  private Insets insets;
  private Color shadowColor;
  private Color highlightColor;

  public TopEtchedBorder(Component component)
  {
    insets = new Insets(2, 0, 2, 0);
    shadowColor = component.getBackground().darker();
    highlightColor = component.getBackground().brighter();
  }

  public Insets getBorderInsets(Component c)
  {
    return insets;
  }

  public boolean isBorderOpaque()
  {
    return true;
  }

  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
  {
    Color currentColor = g.getColor();

    g.setColor(shadowColor);
    g.drawLine(0, 0, width, 0); // top
    // g.drawLine(0, height-2, width, height-2); // bottom

    g.setColor(highlightColor);
    g.drawLine(0, 1, width, 1); // top 
    // g.drawLine(0, height-1, width, height-1); // bottom

    g.setColor(currentColor);
  }

}

