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

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import com.riffcrafter.common.midi.Instruments;
import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.studio.app.Editor;


public class InstrumentPanel extends GridBagPanel
{
  private Editor editor;
  private int channel;
  private InstrumentTable instrumentTable;

  private ButtonGroup buttonGroup;
  private JRadioButton updateRadioButton;
  private JRadioButton addRadioButton;

  private boolean inSetInstrument;

  private InstrumentListSelectionListener instrumentListSelectionListener = new InstrumentListSelectionListener();

  public InstrumentPanel()
  {
    TableModel tableModel = Instruments.getTableModel();
    instrumentTable = new InstrumentTable(tableModel);

    JScrollPane scrollPane = new JScrollPane(instrumentTable);
    scrollPane.getViewport().setBackground(instrumentTable.getBackground());
    add(scrollPane, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=nw,fill=b,weightx=1,weighty=1,gridwidth=*,gridheight=1");

    updateRadioButton = new JRadioButton("Change current instrument for channel");
    updateRadioButton.setHorizontalAlignment(SwingConstants.LEFT);
    add(updateRadioButton, "x=0,y=1,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

    addRadioButton = new JRadioButton("Add new instrument for channel");
    addRadioButton.setHorizontalAlignment(SwingConstants.RIGHT);
    add(addRadioButton, "x=1,y=1,top=5,left=5,bottom=0,right=5,anchor=e,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

    buttonGroup = new ButtonGroup();
    buttonGroup.add(updateRadioButton);
    buttonGroup.add(addRadioButton);
    updateRadioButton.setSelected(true);
  }

  public void selectChannel(Editor editor, int channel)
  {
    this.editor = editor;
    this.channel = channel;
    setEnabled(this, channel != Instruments.DRUM_CHANNEL);
    if (editor != null)
    {
      int instrument = editor.getInstrument(channel);
      setInstrument(instrument);
    }
  }

  private void updateInstrument()
  {
    int instrument = getInstrument();
    boolean isUpdate = updateRadioButton.isSelected();
    editor.setInstrument(channel, instrument, isUpdate);
  }

  public int getInstrument()
  {
    int row = instrumentTable.getSelectedRow();
    int column = instrumentTable.getSelectedColumn();
    int instrument = Instruments.getInstrument(row, column);
    return instrument;
  }

  public void setInstrument(int instrument)
  {
    instrumentTable.setInstrument(instrument);
  }

  public void onNotatorInstrumentChange(int instrument)
  {
    setInstrument(instrument);
  }

  private class InstrumentTable extends JTable
  {
    private InstrumentTable(TableModel tableModel)
    {
      super(tableModel);
      setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      setCellSelectionEnabled(true);
      setShowGrid(false);
      // Listen for selections across rows
      getSelectionModel().addListSelectionListener(instrumentListSelectionListener);
      // Listen for selections across columns (yes, we do get dups if both row/col change!)
      getColumnModel().getSelectionModel().addListSelectionListener(instrumentListSelectionListener);
    }

    public void setInstrument(int instrument)
    {
      inSetInstrument = true;
      if (instrument == -1)
      {
        clearSelection();
      }
      else
      {
        int rowIndex = Instruments.getRowIndex(instrument);
        int columnIndex = Instruments.getColumnIndex(instrument);
        changeSelection(rowIndex, columnIndex, false, false);
      }
      inSetInstrument = false;
    }

    public Component prepareRenderer(final TableCellRenderer renderer, int row, int column)
    {
      Component component = super.prepareRenderer(renderer, row, column);
      component.setEnabled(isEnabled());
      return component;
    }
  }

  private class InstrumentListSelectionListener implements ListSelectionListener
  {
    public void valueChanged(ListSelectionEvent e)
    {
      if (!inSetInstrument && !e.getValueIsAdjusting())
      {
        updateInstrument();
      }
    }
  }

}
