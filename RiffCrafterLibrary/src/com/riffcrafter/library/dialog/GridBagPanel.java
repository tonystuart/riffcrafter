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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

public class GridBagPanel extends JPanel
{
  private GridBagLayout gridBagLayout;

  public GridBagPanel()
  {
    super(null);
    gridBagLayout = new GridBagLayout();
    setLayout(gridBagLayout);
  }
  
  public void makeEqualWidth()
  {
    int count = getComponentCount();
    for (int i = 0; i < count; i++)
    {
      Component component = getComponent(i);
      Dimension size = component.getPreferredSize();
      size.width = 100;
      component.setPreferredSize(size);
    }
  }

  public static void setEnabled(Component component, boolean isEnabled)
  {
    if (component.isEnabled() != isEnabled)
    {
      component.setEnabled(isEnabled);
      if (component instanceof Container)
      {
        Container container = (Container)component;
        int componentCount = container.getComponentCount();
        for (int i = 0; i < componentCount; i++)
        {
          Component childComponent = container.getComponent(i);
          setEnabled(childComponent, isEnabled);
        }
      }
    }
  }

  public static Color shade(Color c, int percent)
  {
    return new Color(c.getRed() * percent / 100, c.getGreen() * percent / 100, c.getBlue() * percent / 100);
  }

  /**
   *  x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1
   */

  public void add(Component component, String constraintSpecification)
  {
    GridBagConstraints c = new GridBagConstraints();

    String[] constraints = constraintSpecification.split(",");

    for (String constraint : constraints)
    {
      String[] tokens = constraint.split("=");
      String name = tokens[0];
      String value = tokens[1];
      if (name.equals("x"))
      {
        c.gridx = Integer.parseInt(value);
      }
      else if (name.equals("y"))
      {
        c.gridy = Integer.parseInt(value);
      }
      else if (name.equals("top"))
      {
        c.insets.top = Integer.parseInt(value);
      }
      else if (name.equals("left"))
      {
        c.insets.left = Integer.parseInt(value);
      }
      else if (name.equals("bottom"))
      {
        c.insets.bottom = Integer.parseInt(value);
      }
      else if (name.equals("right"))
      {
        c.insets.right = Integer.parseInt(value);
      }
      else if (name.equals("anchor"))
      {
        c.anchor = getAnchor(value);
      }
      else if (name.equals("fill"))
      {
        c.fill = getFill(value);
      }
      else if (name.equals("weightx"))
      {
        c.weightx = Double.parseDouble(value);
      }
      else if (name.equals("weighty"))
      {
        c.weighty = Double.parseDouble(value);
      }
      else if (name.equals("gridwidth"))
      {
        c.gridwidth = getGridExtent(value);
      }
      else if (name.equals("gridheight"))
      {
        c.gridheight = getGridExtent(value);
      }
      else
      {
        throw new RuntimeException("Invalid constraint name=" + name);
      }
    }

    gridBagLayout.setConstraints(component, c);
    add(component);
  }

  private int getAnchor(String value)
  {
    int anchor = 0;

    if (value.equals("center") || value.equals("c"))
    {
      anchor = GridBagConstraints.CENTER;
    }
    else if (value.equals("north") || value.equals("n"))
    {
      anchor = GridBagConstraints.NORTH;
    }
    else if (value.equals("northeast") || value.equals("ne"))
    {
      anchor = GridBagConstraints.NORTHEAST;
    }
    else if (value.equals("east") || value.equals("e"))
    {
      anchor = GridBagConstraints.EAST;
    }
    else if (value.equals("southeast") || value.equals("se"))
    {
      anchor = GridBagConstraints.SOUTHEAST;
    }
    else if (value.equals("south") || value.equals("s"))
    {
      anchor = GridBagConstraints.SOUTH;
    }
    else if (value.equals("southwest") || value.equals("sw"))
    {
      anchor = GridBagConstraints.SOUTHWEST;
    }
    else if (value.equals("west") || value.equals("w"))
    {
      anchor = GridBagConstraints.WEST;
    }
    else if (value.equals("northwest") || value.equals("nw"))
    {
      anchor = GridBagConstraints.NORTHWEST;
    }
    else
    {
      throw new RuntimeException("Invalid anchor value=" + value);
    }

    return anchor;
  }

  private int getFill(String value)
  {
    int fill = 0;

    if (value.equals("none") || value.equals("n"))
    {
      fill = GridBagConstraints.NONE;
    }
    else if (value.equals("horizontal") || value.equals("h"))
    {
      fill = GridBagConstraints.HORIZONTAL;
    }
    else if (value.equals("vertical") || value.equals("v"))
    {
      fill = GridBagConstraints.VERTICAL;
    }
    else if (value.equals("both") || value.equals("b"))
    {
      fill = GridBagConstraints.BOTH;
    }
    else
    {
      throw new RuntimeException("Invalid fill value=" + value);
    }

    return fill;
  }

  private int getGridExtent(String value)
  {
    int gridWidth = 0;

    if (value.equals("remainder") || value.equals("*"))
    {
      gridWidth = GridBagConstraints.REMAINDER;
    }
    else
    {
      gridWidth = Integer.parseInt(value);
    }

    return gridWidth;
  }

  public void setBorder(String borderSpecification)
  {
    ArrayList<Border> borders = new ArrayList<Border>();
    String[] tokens = borderSpecification.split(";");
    for (int i = 0; i < tokens.length; i++)
    {
      Border border = null;
      if (tokens[i].equals("lowered"))
      {
        border = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
      }
      else if (tokens[i].equals("top-etched"))
      {
        border = new TopEtchedBorder(this);
      }
      else if (tokens[i].contains(":"))
      {
        String[] subTokens = tokens[i].split(":");
        String[] parameters = subTokens[1].split(",");
        if (subTokens[0].equals("empty"))
        {
          int top = Integer.parseInt(parameters[0]);
          int left = Integer.parseInt(parameters[1]);
          int bottom = Integer.parseInt(parameters[2]);
          int right = Integer.parseInt(parameters[3]);
          border = BorderFactory.createEmptyBorder(top, left, bottom, right);
        }
        else if (subTokens[0].equals("titled"))
        {
          border = BorderFactory.createTitledBorder(" " + parameters[0] + " ");
        }
      }

      if (border == null)
      {
        throw new RuntimeException("Invalid border specification " + borderSpecification);
      }
      borders.add(border);
    }

    DynamicBorder baseBorder = new DynamicBorder();
    DynamicBorder currentBorder = baseBorder;

    for (Border border : borders)
    {
      if (currentBorder.getOutsideBorder() == null)
      {
        currentBorder.setOutsideBorder(border);
      }
      else if (currentBorder.getInsideBorder() == null)
      {
        currentBorder.setInsideBorder(border);
      }
      else
      {
        DynamicBorder newBorder = new DynamicBorder();
        newBorder.setOutsideBorder(currentBorder.getInsideBorder());
        newBorder.setInsideBorder(border);
        currentBorder.setInsideBorder(newBorder);
        currentBorder = newBorder;
      }
    }

    if (baseBorder.getInsideBorder() == null)
    {
      setBorder(baseBorder.getOutsideBorder());
    }
    else
    {
      setBorder(baseBorder);
    }
  }

  private class DynamicBorder extends CompoundBorder
  {
    private void setOutsideBorder(Border border)
    {
      outsideBorder = border;
    }

    private void setInsideBorder(Border border)
    {
      insideBorder = border;
    }
  }
}
