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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Rectangle;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import com.riffcrafter.library.util.Resources;


public class HelpDialog extends JDialog
{
  private JEditorPane editorPane;

  public HelpDialog(Component component, String key)
  {
    super(JOptionPane.getFrameForComponent(component));

    String helpTitle = Resources.get(key + ".Title");
    String helpText = Resources.get(key + ".Text");
    setTitle(helpTitle);

    editorPane = new JEditorPane("text/html", helpText);
    editorPane.setEditable(false);
    
    JScrollPane scrollPane = new JScrollPane(editorPane);
    getContentPane().add(scrollPane);
    
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    setSize(new Dimension(700, 500));
    setLocationRelativeTo(component);
    setVisible(true);
    
    EventQueue.invokeLater(new Runnable()
    {
      public void run()
      {
        scrollToHome();
      }
    });
  }

  protected void scrollToHome()
  {
    editorPane.scrollRectToVisible(new Rectangle(0, 0));
  }

}
