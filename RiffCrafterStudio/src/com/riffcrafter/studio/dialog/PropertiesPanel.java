// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.dialog;

import com.riffcrafter.library.dialog.GridBagPanel;


public class PropertiesPanel extends GridBagPanel
{
  public PropertiesPanel()
  {
    PropertiesMainPanel commandMainPanel = new PropertiesMainPanel();
    add(commandMainPanel, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=e,fill=b,weightx=1,weighty=1,gridwidth=1,gridheight=1");
  }

  public class PropertiesMainPanel extends GridBagPanel
  {
    private PropertiesMainPanel()
    {
      
    }
  }

}
