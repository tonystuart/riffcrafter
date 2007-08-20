// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.launcher.app;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.riffcrafter.studio.app.Studio;

public class StudioLauncher extends JFrame
{
  private static final double JAVA_UPGRADE_VERSION = 1.6;
  private static final String JAVA_UPGRADE_TITLE = "Java Upgrade Required";
  private static final String JAVA_UPGRADE_MESSAGE = "RiffCrafter Studio requires Java SE version 6 or higher.\nPlease download Java Runtime Environment (JRE) 6u1 from:\nhttp://java.sun.com/javase/downloads/index.jsp";

  public static void main(String[] args) throws Exception
  {
    if (!isCompatible(JAVA_UPGRADE_VERSION))
    {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      JOptionPane.showConfirmDialog(null, JAVA_UPGRADE_MESSAGE, JAVA_UPGRADE_TITLE, JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
    Studio.main(args);
  }

  private static boolean isCompatible(double requiredVersion)
  {
    String versionString = System.getProperty("java.specification.version");
    int firstDot = versionString.indexOf('.');
    if (firstDot != -1 && firstDot < versionString.length() - 2)
    {
      int secondDot = versionString.indexOf('.', firstDot + 1);
      if (secondDot != -1)
      {
        versionString = versionString.substring(0, secondDot);
      }
    }

    double version = Double.parseDouble(versionString);
    return version >= requiredVersion;
  }

}
