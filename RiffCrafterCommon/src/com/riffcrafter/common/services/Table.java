// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.services;

import javax.swing.table.DefaultTableModel;

import com.riffcrafter.library.util.Resources;
import com.riffcrafter.library.util.XmlReader;
import com.riffcrafter.library.util.XmlWriter;


public class Table extends DefaultTableModel
{

  private boolean isEditable;

  public Table()
  {
  }

  public Table(XmlReader xmlReader)
  {
    readXml(xmlReader);
  }

  public void writeXml(XmlWriter xmlWriter, int level)
  {
    int nextLevel = level + 1;
    xmlWriter.beginTag(Tags.TABLE, level);
    int rowCount = getRowCount();
    int columnCount = getColumnCount();

    xmlWriter.writeTaggedInt(Tags.ROW_COUNT, rowCount, nextLevel);
    xmlWriter.writeTaggedInt(Tags.COLUMN_COUNT, columnCount, nextLevel);

    for (int column = 0; column < columnCount; column++)
    {
      String columnName = getColumnName(column);
      xmlWriter.writeTaggedString(Tags.HEADING, columnName, nextLevel);
    }

    for (int row = 0; row < rowCount; row++)
    {
      for (int column = 0; column < columnCount; column++)
      {
        Object value = getValueAt(row, column);
        xmlWriter.writeTaggedObject(Tags.VALUE, value, nextLevel);
      }
    }

    xmlWriter.endTag(Tags.TABLE, level);

  }

  private void readXml(XmlReader xmlReader)
  {
    xmlReader.beginTag(Tags.TABLE);
    int rowCount = xmlReader.readTaggedInt(Tags.ROW_COUNT);
    int columnCount = xmlReader.readTaggedInt(Tags.COLUMN_COUNT);

    for (int column = 0; column < columnCount; column++)
    {
      String columnName = xmlReader.readTaggedString(Tags.HEADING);
      columnName = Resources.get("Table.Heading." + columnName, columnName);
      addColumn(columnName);
    }

    setRowCount(rowCount);

    for (int row = 0; row < rowCount; row++)
    {
      for (int column = 0; column < columnCount; column++)
      {
        Object value = xmlReader.readTaggedObject(Tags.VALUE);
        setValueAt(value, row, column);
      }
    }

    xmlReader.endTag(Tags.TABLE);

  }

  public void setEditable(boolean isEditable)
  {
    this.isEditable = isEditable;
  }

  public boolean isCellEditable(int row, int column)
  {
    return isEditable;
  }
}
