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

import javax.swing.JTextField;

//Limit the preferred width so that this component is expanded
//by the containing GridBagPanels, not vice-versa.

//If the preferred width is not set, then the component will set
//it based on its content, which will be accommodated by all of the
//containing GridBagPanels, even if they have a maximum width set.

public class WidthConstrainedTextField extends JTextField
{
  @Override
  public Dimension getPreferredSize()
  {
    Dimension preferredSize = super.getPreferredSize();
    preferredSize.width = 1;
    return preferredSize;
  }
}

