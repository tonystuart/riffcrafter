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
import java.io.File;

import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileView;

import com.riffcrafter.library.util.Helper;
import com.riffcrafter.library.util.Resources;
import com.riffcrafter.library.util.Settings;


public class FileDialog extends JFileChooser
{
  public class MidiFileView extends FileView
  {
    @Override
    public Icon getIcon(File f)
    {
      Icon icon = null;
      if (endsWithRecognizedSuffix(f.getName()))
      {
        icon = Resources.getIcon("Application.Desktop.Icon");
      }
      else
      {
        icon = super.getIcon(f);
      }
      return icon;
    }

  }

  private static final String suffix = Resources.get("FileDialog.Suffix");
  private static final String description = Resources.get("FileDialog.Description");
  private static final String defaultSuffix = Resources.get("FileDialog.DefaultSuffix");

  private static final String[] suffixes = suffix.split(";");

  private FileNameExtensionFilter fileDialogFileFilter = new FileNameExtensionFilter(description, suffix.split(";"));

  protected Component parent;

  public FileDialog(Component parent)
  {
    this.parent = parent;
    setFileFilter(fileDialogFileFilter);
    setFileView(new MidiFileView());
    setCurrentDirectory(Settings.getFile(Settings.CURRENT_DIRECTORY_KEY, null));
  }

  public boolean validateFile(File file)
  {
    return true;
  }

  public String showDialog()
  {
    int response = super.showDialog(parent, null);
    if (response != JFileChooser.APPROVE_OPTION)
    {
      return null;
    }

    File file = getSelectedFile();

    if (!file.canRead() && getFileFilter() == fileDialogFileFilter)
    {
      String fileName = Helper.getCanonicalPath(file);
      if (!endsWithRecognizedSuffix(fileName))
      {
        fileName += "." + defaultSuffix;
        file = new File(fileName);
      }
    }

    Settings.put(Settings.CURRENT_DIRECTORY_KEY, file.getParentFile());

    if (!validateFile(file))
    {
      return null;
    }

    String fileName = Helper.getCanonicalPath(file);
    return fileName;
  }

  private boolean endsWithRecognizedSuffix(String fileName)
  {
    for (String suffix : suffixes)
    {
      if (fileName.endsWith("." + suffix))
      {
        return true;
      }
    }
    return false;
  }

  public static class SaveDialog extends FileDialog
  {
    public SaveDialog(Component parent)
    {
      super(parent);
      setDialogType(JFileChooser.SAVE_DIALOG);
    }

    public boolean validateFile(File file)
    {
      if (file.exists())
      {
        return CommonDialog.showYesNo(parent, Resources.format("FileDialog.Overwrite", file));
      }

      return true;
    }
  }

  public static class OpenDialog extends FileDialog
  {
    public OpenDialog(Component parent)
    {
      super(parent);
      setDialogType(JFileChooser.OPEN_DIALOG);
    }

    public boolean validateFile(File file)
    {
      if (!file.canRead())
      {
        CommonDialog.showOkay(parent, Resources.format("FileDialog.Missing", file));
        return false;
      }
      return true;
    }

  }

}
