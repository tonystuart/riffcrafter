// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.dialog;

import java.awt.Component;

import javax.swing.JOptionPane;

import com.riffcrafter.library.util.Resources;
import com.riffcrafter.studio.dialog.JamPanel.MainJamPanel;


public class CommonDialog
{
  private static String title = Resources.get("Application.Title.Default");
  
  public static boolean showYesNo(Component component, String message)
  {
    return JOptionPane.showConfirmDialog(component, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
  }

  public static int showYesNoCancel(Component component, String message)
  {
    return JOptionPane.showConfirmDialog(component, message, title, JOptionPane.YES_NO_CANCEL_OPTION);
  }

  public static void showOkay(Component component, String message)
  {
    JOptionPane.showMessageDialog(component, message, title, JOptionPane.OK_OPTION);
  }

  public static String getString(Component component, String message)
  {
    String value = JOptionPane.showInputDialog(component, message);
    return value;
  }

}
