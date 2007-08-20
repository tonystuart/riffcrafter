// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.midi;

import java.awt.Adjustable;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.riffcrafter.common.midi.Catcher;
import com.riffcrafter.common.midi.Channel;
import com.riffcrafter.common.midi.ChannelEvent;
import com.riffcrafter.common.midi.MetaEvent;
import com.riffcrafter.common.midi.Midel;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.common.midi.MidiConstants;
import com.riffcrafter.common.midi.Note;
import com.riffcrafter.common.midi.NoteName;
import com.riffcrafter.common.midi.ProgramChange;
import com.riffcrafter.common.midi.SysexEvent;
import com.riffcrafter.common.midi.TempoChange;
import com.riffcrafter.common.midi.TimeSignatureChange;
import com.riffcrafter.common.midi.Midi.MidiListener;
import com.riffcrafter.common.midi.MidiConstants.MMD;
import com.riffcrafter.common.midi.MidiConstants.SMD;
import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.library.util.Broker;
import com.riffcrafter.library.util.Hex;
import com.riffcrafter.library.util.PrintfFormatter;
import com.riffcrafter.library.util.Broker.Listener;
import com.riffcrafter.studio.app.Editor.Bridger;
import com.riffcrafter.studio.app.Editor.SelectionManager;
import com.riffcrafter.studio.dialog.CommonDialog;

public class Tabulator extends GridBagPanel
{
  private static final int DEFAULT_MIDI_TICK = 0;
  private static final int DEFAULT_MIDI_CHANNEL = 0;
  private static final int DEFAULT_MIDI_KEY = 60;
  private static final int DEFAULT_MIDI_VELOCITY = 64;
  private static final int DEFAULT_MIDI_DURATION = 250;

  private Midi midi;
  private SelectionManager selectionManager;

  private JTable midiTable;
  private MidiTableModel midiTableModel;
  private JScrollPane scrollPane;

  private DownwardListener downwardListener = new DownwardListener();
  private Broker upwardBroker = new Broker();

  private PrintfFormatter formatter = new PrintfFormatter();

  private long currentTick;

  private boolean isReceiveSelection;
  private boolean isTableModelChange;
  private boolean isSetCurrentTick;

  private boolean isInReceiveSelection;
  private boolean isInTableModelChange;
  private boolean isInSetCurrentTick;

  private MessageBuilderPanel messageBuilderPanel;

  public Tabulator(Midi midi, SelectionManager selectionManager, Bridger bridger)
  {
    this.midi = midi;
    this.selectionManager = selectionManager;

    midiTableModel = new MidiTableModel(midi);
    midiTable = new JTable(midiTableModel);
    midiTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    scrollPane = new JScrollPane(midiTable);
    scrollPane.getViewport().setBackground(midiTable.getBackground());
    scrollPane.getViewport().addComponentListener(new MidiTableComponentListener());
    add(scrollPane, "x=0,y=0,top=5,left=5,bottom=5,right=5,anchor=nw,fill=b,weightx=1,weighty=1,gridwidth=*,gridheight=1");

    messageBuilderPanel = new MessageBuilderPanel();
    add(messageBuilderPanel, "x=0,y=1,top=5,left=5,bottom=5,right=5,anchor=nw,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

    // Be careful of the order and dependencies amongst the listeners

    selectionManager.subscribe(new SelectionListener());
    midiTable.getSelectionModel().addListSelectionListener(new TabulatorSelectionListener());
    midi.addMidiListener(new TabulatorMidiListener());
    bridger.subscribe(downwardListener, upwardBroker);
    scrollPane.getVerticalScrollBar().addAdjustmentListener(new TabulatorAdjustmentListener());
  }

  @Override
  public void paint(Graphics g)
  {
    if (isTableModelChange)
    {
      // Be sure to do this before adding new selection or selection will go away
      notifyTableModelChange();
    }

    if (isReceiveSelection)
    {
      isInReceiveSelection = true;
      receiveSelection();
      isInReceiveSelection = false;
      isReceiveSelection = false;
    }

    if (isSetCurrentTick)
    {
      isInSetCurrentTick = true;
      int row = midi.getIndex(currentTick);
      scrollTo(row);
      isInSetCurrentTick = false;
      isSetCurrentTick = false;
    }

    super.paint(g);
  }

  private void notifyTableModelChange()
  {
    isInTableModelChange = true;
    midiTableModel.fireTableModelListener(new TableModelEvent(midiTableModel));
    isInTableModelChange = false;
    isTableModelChange = false;
  }

  private void receiveSelection()
  {
    midiTable.clearSelection();
    Midi selection = selectionManager.getSelection();
    if (selection == null || selection.size() == 0)
    {
      return;
    }
    int row = 0;
    boolean isFirstRow = true;
    Midi toSelect = selection.copy();
    long firstTick = selection.getFirstTick();
    for (Midel baseMidel : midi.getMidels())
    {
      if (baseMidel.getTick() >= firstTick)
      {
        for (Iterator<Midel> iterator = toSelect.getIterator(); iterator.hasNext();)
        {
          Midel selectionMidel = iterator.next();
          if (selectionMidel.equals(baseMidel))
          {
            midiTable.addRowSelectionInterval(row, row);
            if (isFirstRow)
            {
              scrollTo(row);
              isFirstRow = false;
            }
            iterator.remove();
          }
        }
        if (toSelect.size() == 0)
        {
          return;
        }
      }
      row++;
    }
  }

  private void sendSelection()
  {
    Midi selection = new Midi();

    int[] rows = midiTable.getSelectedRows();

    for (int row : rows)
    {
      Midel midel = midi.get(row);
      selection.add(midel);
    }

    selectionManager.setSelection(selection, this, true, false);
  }

  private void scrollTo(int row)
  {
    Rectangle cellRect = midiTable.getCellRect(row, 0, true);
    int height = scrollPane.getViewport().getHeight();
    cellRect.y -= height / 2;
    cellRect.height = height;
    midiTable.scrollRectToVisible(cellRect);
  }

  private void setCurrentTick(long nextCurrentTick)
  {
    if (nextCurrentTick != currentTick)
    {
      currentTick = nextCurrentTick;
      isSetCurrentTick = true;
      repaint(); // deferred until the tab is visible
    }
  }

  // Returns the current tick as established by the last call to setCurrentTick (e.g. from the Editor, when pressing keys on the virtual keyboard)

  public long getCurrentTick()
  {
    return currentTick;
  }

  // Returns the tick of the row currently in the middle of the screen (not necessarily selected)

  private long getMiddleTick()
  {
    int y = scrollPane.getVerticalScrollBar().getValue();
    Point point = new Point(0, y);
    int topRow = midiTable.rowAtPoint(point);
    int height = scrollPane.getViewport().getHeight();
    int visibleRows = height / midiTable.getRowHeight();
    int middleRow = topRow + visibleRows / 2;
    int rowCount = midiTable.getRowCount();
    int currentRow;
    if (middleRow < rowCount)
    {
      currentRow = middleRow;
    }
    else
    {
      currentRow = rowCount - 1;
    }
    long tick;
    if (currentRow >= 0)
    {
      Midel midel = midi.get(currentRow);
      tick = midel.getTick();
    }
    else
    {
      tick = 0;
    }
    return tick;
  }

  private void selectRow(int row)
  {
    if (isTableModelChange)
    {
      // Be sure to do this before adding new selection or selection will go away
      notifyTableModelChange();
    }

    midiTable.addRowSelectionInterval(row, row);
    scrollTo(row);
  }

  private String getType(Midel midel)
  {
    String type;
    if (midel instanceof Note)
    {
      type = "Note";
    }
    else if (midel instanceof ChannelEvent)
    {
      type = "Channel";
    }
    else if (midel instanceof MetaEvent)
    {
      type = "Meta";
    }
    else if (midel instanceof SysexEvent)
    {
      type = "Sysex";
    }
    else
    {
      throw new RuntimeException("Invalid midel type");
    }
    return type;
  }

  public Object coalesce(int left, int right, String replacement)
  {
    Object value;
    if (left == right)
    {
      value = replacement;
    }
    else
    {
      value = left;
    }
    return value;
  }

  private class MidiTableModel implements TableModel
  {
    private Midi midi;
    private Midel lastMidel;
    private int lastRowIndex = Integer.MIN_VALUE;
    private ArrayList<TableModelListener> listeners = new ArrayList<TableModelListener>();

    private MidiTableModel(Midi midi)
    {
      this.midi = midi;
    }

    public void clearRowCache()
    {
      lastMidel = null;
      lastRowIndex = Integer.MIN_VALUE;
    }

    private void fireTableModelListener(TableModelEvent e)
    {
      clearRowCache();
      for (TableModelListener listener : listeners)
      {
        listener.tableChanged(e);
      }
    }

    public void addTableModelListener(TableModelListener l)
    {
      listeners.add(l);
    }

    public Class<?> getColumnClass(int columnIndex)
    {
      return String.class;
    }

    public int getColumnCount()
    {
      return 4;
    }

    public String getColumnName(int columnIndex)
    {
      String name;
      switch (columnIndex)
      {
        case 0:
          name = "Tick";
          break;
        case 1:
          name = "Channel";
          break;
        case 2:
          name = "Type";
          break;
        case 3:
          name = "Description";
          break;
        default:
          throw new RuntimeException("Invalid columnIndex=" + columnIndex);
      }
      return name;
    }

    public int getRowCount()
    {
      return midi.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
      Midel midel;
      if (rowIndex == (lastRowIndex))
      {
        midel = lastMidel;
      }
      else if (rowIndex == (lastRowIndex + 1))
      {
        midel = midi.getNext(lastMidel);
      }
      else if (rowIndex == (lastRowIndex - 1))
      {
        midel = midi.getPrevious(lastMidel);
      }
      else
      {
        Midel midel1 = midi.get(rowIndex);
        midel = midel1;
      }

      if (midel == null)
      {
        CommonDialog.showOkay(Tabulator.this, "Please select a row and try again");
        return null;
      }

      Object value;

      switch (columnIndex)
      {
        case 0:
          value = formatter.printf("%,d", midel.getTick());
          break;
        case 1:
          int channel = midel.getChannel();
          channel = Channel.getChannelNumber(channel);
          value = channel;
          break;
        case 2:
          value = getType(midel);
          break;
        case 3:
          value = midel.getDescription();
          break;
        default:
          throw new RuntimeException("Invalid columnIndex=" + columnIndex);
      }

      lastRowIndex = rowIndex;
      lastMidel = midel;
      return value;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
      return false;
    }

    public void removeTableModelListener(TableModelListener l)
    {
      listeners.remove(l);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
    }

  }

  public class MidiTableComponentListener implements ComponentListener
  {

    public void componentHidden(ComponentEvent e)
    {
    }

    public void componentMoved(ComponentEvent e)
    {
    }

    public void componentResized(ComponentEvent e)
    {
      Font font = midiTable.getFont();
      FontMetrics fontMetrics = getFontMetrics(font);
      int charWidth = fontMetrics.charWidth('M');
      TableColumnModel columnModel = midiTable.getColumnModel();
      int tickColumnWidth = 10 * charWidth;
      int channelColumnWidth = 10 * charWidth;
      int typeColumnWidth = 10 * charWidth;
      int descriptionColumnWidth = scrollPane.getViewport().getWidth() - tickColumnWidth - channelColumnWidth - typeColumnWidth;
      columnModel.getColumn(0).setPreferredWidth(tickColumnWidth);
      columnModel.getColumn(1).setPreferredWidth(channelColumnWidth);
      columnModel.getColumn(2).setPreferredWidth(typeColumnWidth);
      columnModel.getColumn(3).setPreferredWidth(descriptionColumnWidth);
    }

    public void componentShown(ComponentEvent e)
    {
    }

  }

  public class TabulatorSelectionListener implements ListSelectionListener
  {

    public void valueChanged(ListSelectionEvent e)
    {
      if (!isInReceiveSelection && !isInTableModelChange)
      {
        if (!e.getValueIsAdjusting())
        {
          sendSelection();
          messageBuilderPanel.editSelection();
        }
      }
    }

  }

  public class SelectionListener implements Listener
  {

    public void notify(Object event, Object source)
    {
      if (source != Tabulator.this)
      {
        isReceiveSelection = true;
        repaint(); // deferred until the tab is visible
      }
    }

  }

  public class TabulatorMidiListener implements MidiListener
  {

    public void onAddMidel(Midi midi, Midel addMidel)
    {
      isTableModelChange = true;
      repaint(); // deferred until the tab is visible
    }

    public void onRemoveMidel(Midi midi, Midel removeMidel)
    {
      isTableModelChange = true;
      repaint(); // deferred until the tab is visible
    }

  }

  public class DownwardListener implements Listener
  {
    public void notify(Object event, Object source)
    {
      if (source != Tabulator.this)
      {
        long nextCurrentTick = (Long)event;
        setCurrentTick(nextCurrentTick);
      }
    }
  }

  public class TabulatorAdjustmentListener implements AdjustmentListener
  {
    public void adjustmentValueChanged(AdjustmentEvent e)
    {
      // Filter out changes initiated by setCurrentTick or receiveSelection (via scrollTo), leaving only user-initiated changes, to avoid recursive loops
      if (!isInSetCurrentTick && !isInReceiveSelection)
      {
        // Also filter out any events that occur while a paint is pending (e.g. scrollbar change due to maximize of Studio window while Tabulator not visible)
        if (!isTableModelChange && !isReceiveSelection && !isSetCurrentTick)
        {
          long tick = getMiddleTick();
          // Note that Player uses isAdjusting
          e = new AdjustmentEvent((Adjustable)e.getSource(), e.getID(), e.getAdjustmentType(), (int)tick, e.getValueIsAdjusting());
          upwardBroker.publish(e, Tabulator.this);
        }
      }
    }
  }

  private class MessageBuilderPanel extends GridBagPanel
  {
    private NoteEditor noteEditor;
    private ChannelEditor channelEditor;
    private MetaEditor metaEditor;
    private SysexEditor sysexEditor;
    private JTabbedPane tabbedPane;

    private MessageBuilderPanel()
    {
      tabbedPane = new JTabbedPane();
      add(tabbedPane, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=c,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      noteEditor = new NoteEditor();
      tabbedPane.add("Note", noteEditor);

      channelEditor = new ChannelEditor();
      tabbedPane.add("Channel", channelEditor);

      metaEditor = new MetaEditor();
      tabbedPane.add("Meta", metaEditor);

      sysexEditor = new SysexEditor();
      tabbedPane.add("Sysex", sysexEditor);

      ButtonPanel buttonPanel = new ButtonPanel();
      add(buttonPanel, "x=0,y=2,top=0,left=0,bottom=0,right=0,anchor=c,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
    }

    public void editSelection()
    {
      int row = midiTable.getSelectedRow();
      if (row == -1)
      {
        return;
      }

      Midel midel = midi.get(row);

      MessageTypeEditor messageTypeEditor;

      if (midel instanceof Note)
      {
        messageTypeEditor = noteEditor;
      }
      else if (midel instanceof ChannelEvent)
      {
        messageTypeEditor = channelEditor;
      }
      else if (midel instanceof MetaEvent)
      {
        messageTypeEditor = metaEditor;
      }
      else if (midel instanceof SysexEvent)
      {
        messageTypeEditor = sysexEditor;
      }
      else
      {
        throw new RuntimeException("Missing editor");
      }

      messageTypeEditor.display(midel);
      tabbedPane.setSelectedComponent(messageTypeEditor);
    }

    private class ButtonPanel extends GridBagPanel
    {
      private ButtonPanel()
      {
        JPanel spacer = new JPanel(null);
        add(spacer, "x=0,y=0,top=5,left=0,bottom=5,right=5,anchor=e,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

        JButton resetButton = new JButton("New");
        resetButton.addActionListener(new ResetActionListener());
        add(resetButton, "x=1,y=0,top=5,left=0,bottom=5,right=5,anchor=e,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

        JButton updateButton = new JButton("Update");
        updateButton.addActionListener(new UpdateActionListener());
        add(updateButton, "x=2,y=0,top=5,left=0,bottom=5,right=5,anchor=e,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

        JButton insertButton = new JButton("Insert");
        insertButton.addActionListener(new InsertActionListener());
        add(insertButton, "x=3,y=0,top=5,left=0,bottom=5,right=5,anchor=e,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(new DeleteActionListener());
        add(deleteButton, "x=4,y=0,top=5,left=0,bottom=5,right=5,anchor=e,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");
      }

      public class ResetActionListener implements ActionListener
      {
        public void actionPerformed(ActionEvent e)
        {
          MessageTypeEditor currentEditor = (MessageTypeEditor)tabbedPane.getSelectedComponent();
          currentEditor.reset();
        }
      }

      public class UpdateActionListener implements ActionListener
      {
        public void actionPerformed(ActionEvent e)
        {
          MessageTypeEditor currentEditor = (MessageTypeEditor)tabbedPane.getSelectedComponent();
          currentEditor.update();
        }
      }

      public class InsertActionListener implements ActionListener
      {
        public void actionPerformed(ActionEvent e)
        {
          MessageTypeEditor currentEditor = (MessageTypeEditor)tabbedPane.getSelectedComponent();
          currentEditor.insert();
        }
      }

      public class DeleteActionListener implements ActionListener
      {
        public void actionPerformed(ActionEvent e)
        {
          MessageTypeEditor currentEditor = (MessageTypeEditor)tabbedPane.getSelectedComponent();
          currentEditor.delete();
        }
      }

    }

  }

  private abstract class MessageTypeEditor extends GridBagPanel
  {
    private Midel baseMidel;

    protected abstract Midel getMidel();

    /**
     * Derived classes must invoke base class.
     */
    protected void display(Midel midel)
    {
      baseMidel = midel;
    }

    /**
     * Derived classes must invoke base class.
     */
    protected void reset()
    {
      baseMidel = null;
    }

    protected void update()
    {
      try
      {
        int serialNumber = -1;
        if (baseMidel != null)
        {
          serialNumber = baseMidel.getSerialNumber();
          midi.remove(baseMidel);
        }
        Midel midel = getMidel();
        if (serialNumber != -1)
        {
          midel.setSerialNumber(serialNumber);
        }
        midi.add(midel);
        baseMidel = midel;
        int row = midi.getIndex(midel);
        if (row != -1)
        {
          selectRow(row);
        }
      }
      catch (Exception e)
      {
        CommonDialog.showOkay(this, e.getMessage());
      }
    }

    protected void insert()
    {
      try
      {
        Midel midel = getMidel();
        midi.add(midel);
        baseMidel = midel;
        int row = midi.getIndex(midel);
        if (row != -1)
        {
          selectRow(row);
        }
      }
      catch (Exception e)
      {
        CommonDialog.showOkay(this, e.getMessage());
      }
    }

    protected void delete()
    {
      try
      {
        Midel midel = getMidel();
        midi.remove(midel);
        baseMidel = null;
        int row = midi.getIndex(midel.getTick());
        if (row != -1)
        {
          selectRow(row);
        }
      }
      catch (Exception e)
      {
        CommonDialog.showOkay(this, e.getMessage());
      }
    }

  }

  private class NoteEditor extends MessageTypeEditor
  {
    private JSpinner tickSpinner;
    private JSpinner channelSpinner;
    private MidiKeySpinner midiKeySpinner;
    private JSpinner velocitySpinner;
    private JSpinner durationSpinner;

    private NoteEditor()
    {
      JLabel tickLabel = new JLabel("Tick:");
      add(tickLabel, "x=0,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel channelLabel = new JLabel("Channel:");
      add(channelLabel, "x=1,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      tickSpinner = new JSpinner();
      add(tickSpinner, "x=0,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      SpinnerNumberModel channelSpinnerModel = new SpinnerNumberModel(1, 1, 16, 1);
      channelSpinner = new JSpinner(channelSpinnerModel);
      add(channelSpinner, "x=1,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel midiKeyLabel = new JLabel("Midi Keyboard Key:");
      add(midiKeyLabel, "x=0,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel velocityLabel = new JLabel("Velocity:");
      add(velocityLabel, "x=1,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel durationLabel = new JLabel("Duration:");
      add(durationLabel, "x=2,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      midiKeySpinner = new MidiKeySpinner();
      add(midiKeySpinner, "x=0,y=3,top=0,left=5,bottom=10,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      velocitySpinner = new JSpinner();
      add(velocitySpinner, "x=1,y=3,top=0,left=5,bottom=10,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      durationSpinner = new JSpinner();
      add(durationSpinner, "x=2,y=3,top=0,left=5,bottom=10,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      makeEqualWidth();

      reset();
    }

    private long getTick()
    {
      long tick = ((Number)tickSpinner.getValue()).longValue();
      return tick;
    }

    private void setTick(long tick)
    {
      tickSpinner.setValue(tick);
    }

    private int getChannel()
    {
      int channel = ((Number)channelSpinner.getValue()).intValue();
      return channel;
    }

    private void setChannel(int channel)
    {
      channelSpinner.setValue(channel);
    }

    private int getKey()
    {
      int key = midiKeySpinner.getValue();
      return key;
    }

    private void setKey(int key)
    {
      midiKeySpinner.setValue(key);
    }

    private int getVelocity()
    {
      int velocity = ((Number)velocitySpinner.getValue()).intValue();
      return velocity;
    }

    private void setVelocity(int velocity)
    {
      velocitySpinner.setValue(velocity);
    }

    private long getDuration()
    {
      int duration = ((Number)durationSpinner.getValue()).intValue();
      return duration;
    }

    private void setDuration(long duration)
    {
      durationSpinner.setValue(duration);
    }

    public void reset()
    {
      setTick(DEFAULT_MIDI_TICK);
      setChannel(Channel.getChannelNumber(DEFAULT_MIDI_CHANNEL));
      setKey(DEFAULT_MIDI_KEY);
      setVelocity(DEFAULT_MIDI_VELOCITY);
      setDuration(DEFAULT_MIDI_DURATION);

      super.reset();
    }

    public void display(Midel midel)
    {
      Note note = (Note)midel;

      long tick = note.getTick();
      int channel = note.getChannel();
      int key = note.getKey();
      int velocity = note.getVelocity();
      long duration = note.getDuration();

      setTick(tick);
      setChannel(Channel.getChannelNumber(channel));
      setKey(key);
      setVelocity(velocity);
      setDuration(duration);

      super.display(midel);
    }

    @Override
    protected Midel getMidel()
    {
      long tick = getTick();
      int channel = Channel.getChannelIndex(getChannel());
      int key = getKey();
      int velocity = getVelocity();
      long duration = getDuration();
      Note note = new Note(channel, key, velocity, tick, duration);
      return note;
    }

  }

  private abstract class LabeledSpinner extends GridBagPanel
  {
    protected JLabel label;
    protected SpinnerNumberModel spinnerNumberModel;

    protected LabeledSpinner(int value, int minimum, int maximum, int stepSize, int numChars)
    {
      spinnerNumberModel = new SpinnerNumberModel(value, minimum, maximum, stepSize);
      JSpinner spinner = new JSpinner(spinnerNumberModel);
      spinner.addChangeListener(new SpinnerListener());
      add(spinner, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      label = new JLabel();
      Font font = label.getFont();
      FontMetrics fontMetrics = label.getFontMetrics(font);
      int width = fontMetrics.charWidth('M') * numChars;
      int height = fontMetrics.getHeight();
      Dimension size = new Dimension(width, height);
      label.setPreferredSize(size);
      label.setMinimumSize(size);
      label.setMaximumSize(size);
      add(label, "x=1,y=0,top=0,left=5,bottom=0,right=0,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");
    }

    public int getValue()
    {
      int value = spinnerNumberModel.getNumber().intValue();
      return value;
    }

    public void setValue(int value)
    {
      spinnerNumberModel.setValue(value);
      propagateSpinnerToLabel();
    }

    protected abstract void propagateSpinnerToLabel();

    public class SpinnerListener implements ChangeListener
    {
      public void stateChanged(ChangeEvent e)
      {
        propagateSpinnerToLabel();
      }
    }

  }

  private class MidiKeySpinner extends LabeledSpinner
  {
    private MidiKeySpinner()
    {
      super(60, 0, 127, 1, 3);
    }

    protected void propagateSpinnerToLabel()
    {
      int key = getValue();
      label.setText(NoteName.getNoteName(key, true));
    }
  }

  private class ChannelEditor extends MessageTypeEditor
  {
    private JSpinner tickSpinner;
    private JSpinner channelSpinner;
    private JComboBox shortMessageCommandComboBox;
    private JTextField shortMessageValueTextField;

    private ChannelEditor()
    {
      JLabel tickLabel = new JLabel("Tick:");
      add(tickLabel, "x=0,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel channelLabel = new JLabel("Channel:");
      add(channelLabel, "x=1,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      tickSpinner = new JSpinner();
      add(tickSpinner, "x=0,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      SpinnerNumberModel channelSpinnerModel = new SpinnerNumberModel(1, 1, 16, 1);
      channelSpinner = new JSpinner(channelSpinnerModel);
      add(channelSpinner, "x=1,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel typeLabel = new JLabel("Type:");
      add(typeLabel, "x=0,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel valueLabel = new JLabel("Value:");
      add(valueLabel, "x=1,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

      shortMessageCommandComboBox = new JComboBox(MidiConstants.SMD.getSmdBySynopsis());
      add(shortMessageCommandComboBox, "x=0,y=3,top=0,left=5,bottom=10,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      shortMessageValueTextField = new JTextField();
      add(shortMessageValueTextField, "x=1,y=3,top=0,left=5,bottom=10,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

      makeEqualWidth();

      reset();
    }

    private long getTick()
    {
      long tick = ((Number)tickSpinner.getValue()).longValue();
      return tick;
    }

    private void setTick(long tick)
    {
      tickSpinner.setValue(tick);
    }

    private int getChannel()
    {
      int channel = ((Number)channelSpinner.getValue()).intValue();
      return channel;
    }

    private void setChannel(int channel)
    {
      channelSpinner.setValue(channel);
    }

    private int getCommand()
    {
      SMD smd = (SMD)shortMessageCommandComboBox.getSelectedItem();
      int command = smd.getCommand();
      return command;
    }

    private void setType(int type, int subType)
    {
      SMD smd = SMD.find(type, subType);
      shortMessageCommandComboBox.setSelectedItem(smd);
    }

    private int[] getData()
    {
      SMD smd = (SMD)shortMessageCommandComboBox.getSelectedItem();
      int encoding = smd.getEncoding();

      String text = shortMessageValueTextField.getText();
      int[] data = new int[2];

      switch (encoding)
      {
        case MidiConstants.SMD_DATA2:
          data[0] = smd.getSubCommand();
          data[1] = Integer.parseInt(text);
          break;
        case MidiConstants.SMD_DATA1_DATA2:
          fromData1Data2(text, data);
          break;
        case MidiConstants.SMD_14BIT:
          ChannelEvent.from14BitValue(text, data);
          break;
        case MidiConstants.SMD_PC:
          data[0] = Integer.parseInt(text);
          break;
      }

      return data;
    }

    private void fromData1Data2(String text, int[] data)
    {
      String[] tokens = text.split("[ \t,]+");
      if (tokens.length != 2)
      {
        throw new RuntimeException("Expected two values, for example: 1 2");
      }

      data[0] = Integer.parseInt(tokens[0]);
      data[1] = Integer.parseInt(tokens[1]);
    }

    private void setData(int data1, int data2)
    {
      SMD smd = (SMD)shortMessageCommandComboBox.getSelectedItem();
      int encoding = smd.getEncoding();

      switch (encoding)
      {
        case MidiConstants.SMD_DATA2:
          shortMessageValueTextField.setText(Integer.toString(data2));
          break;
        case MidiConstants.SMD_DATA1_DATA2:
          shortMessageValueTextField.setText(Integer.toString(data1) + " " + Integer.toString(data2));
          break;
        case MidiConstants.SMD_14BIT:
          shortMessageValueTextField.setText(Integer.toString(ChannelEvent.get14BitValue(data1, data2)));
          break;
        case MidiConstants.SMD_PC:
          shortMessageValueTextField.setText(Integer.toString(data1));
          break;
      }
    }

    public void reset()
    {
      setTick(DEFAULT_MIDI_TICK);
      setChannel(Channel.getChannelNumber(DEFAULT_MIDI_CHANNEL));
      setType(MidiConstants.SM_COMMAND_CONTROL_CHANGE, MidiConstants.CC_VOLUME);
      setData(MidiConstants.CC_VOLUME, 64);

      super.reset();
    }

    public void display(Midel midel)
    {
      ChannelEvent channelEvent = (ChannelEvent)midel;

      long tick = channelEvent.getTick();
      int channel = Channel.getChannelNumber(channelEvent.getChannel());
      int command = channelEvent.getCommand();
      int data1 = channelEvent.getData1();
      int data2 = channelEvent.getData2();

      setTick(tick);
      setChannel(channel);
      setType(command, data1);
      setData(data1, data2);

      super.display(midel);
    }

    @Override
    protected Midel getMidel()
    {
      long tick = getTick();
      int channel = Channel.getChannelIndex(getChannel());
      int command = getCommand();
      int[] data = getData();
      ShortMessage shortMessage = new ShortMessage();
      Catcher.setMessage(shortMessage, command, channel, data[0], data[1]);
      ChannelEvent channelEvent;

      switch (command)
      {
        case MidiConstants.SM_COMMAND_PROGRAM_CHANGE:
          channelEvent = new ProgramChange(tick, shortMessage);
          break;
        default:
          channelEvent = new ChannelEvent(tick, shortMessage);
          break;
      }

      return channelEvent;
    }

  }

  private class MetaEditor extends MessageTypeEditor
  {
    private JSpinner tickSpinner;
    private JSpinner channelSpinner;
    private JComboBox metaTypeComboBox;
    private JTextField metaValueTextField;

    private MetaEditor()
    {
      JLabel tickLabel = new JLabel("Tick:");
      add(tickLabel, "x=0,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      tickSpinner = new JSpinner();
      add(tickSpinner, "x=0,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel channelLabel = new JLabel("Channel:");
      add(channelLabel, "x=1,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      channelSpinner = new JSpinner();
      add(channelSpinner, "x=1,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel typeLabel = new JLabel("Type:");
      add(typeLabel, "x=0,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      metaTypeComboBox = new JComboBox(MidiConstants.MMD.getMmdBySynopsis());
      add(metaTypeComboBox, "x=0,y=3,top=0,left=5,bottom=10,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel valueLabel = new JLabel("Value:");
      add(valueLabel, "x=1,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

      metaValueTextField = new JTextField();
      add(metaValueTextField, "x=1,y=3,top=0,left=5,bottom=10,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

      makeEqualWidth();

      reset();
    }

    private long getTick()
    {
      long tick = ((Number)tickSpinner.getValue()).longValue();
      return tick;
    }

    private void setTick(long tick)
    {
      tickSpinner.setValue(tick);
    }

    private int getChannel()
    {
      int channel = ((Number)channelSpinner.getValue()).intValue();
      return channel;
    }

    private void setChannel(int channel)
    {
      channelSpinner.setValue(channel);
    }

    private int getType()
    {
      Object selectedItem = metaTypeComboBox.getSelectedItem();
      MMD mmd = (MMD)selectedItem;
      int type = mmd.getType();
      return type;
    }

    private void setType(int type)
    {
      MMD mmd = MMD.find(type);
      metaTypeComboBox.setSelectedItem(mmd);
    }

    private byte[] getData()
    {
      int type = getType();
      MMD mmd = MMD.find(type);
      int encoding = mmd.getEncoding();

      String text = metaValueTextField.getText();
      byte[] data = null;

      switch (encoding)
      {
        case MidiConstants.MME_STRING:
          data = text.getBytes();
          break;
        case MidiConstants.MME_HEX:
          data = Hex.fromHexString(text);
          break;
        case MidiConstants.MME_NONE:
          data = new byte[0];
          break;
        case MidiConstants.MME_BPM:
          data = TempoChange.fromString(text);
          break;
        case MidiConstants.MME_TSC:
          data = TimeSignatureChange.fromString(text);
          break;
      }

      return data;
    }

    private void setData(byte[] data)
    {
      int type = getType();
      MMD mmd = MMD.find(type);
      int encoding = mmd.getEncoding();

      switch (encoding)
      {
        case MidiConstants.MME_STRING:
          metaValueTextField.setText(new String(data));
          break;
        case MidiConstants.MME_HEX:
          metaValueTextField.setText(Hex.toHexString(data));
          break;
        case MidiConstants.MME_NONE:
          metaValueTextField.setText(null);
          break;
        case MidiConstants.MME_BPM:
          metaValueTextField.setText(TempoChange.toString(data));
          break;
        case MidiConstants.MME_TSC:
          metaValueTextField.setText(TimeSignatureChange.toString(data));
          break;
      }
    }

    public void reset()
    {
      setTick(DEFAULT_MIDI_TICK);
      setChannel(Channel.getChannelNumber(DEFAULT_MIDI_CHANNEL));
      setType(MidiConstants.MM_TEXT);
      setData("".getBytes());

      super.reset();
    }

    public void display(Midel midel)
    {
      MetaEvent metaEvent = (MetaEvent)midel;

      long tick = metaEvent.getTick();
      int channel = Channel.getChannelNumber(metaEvent.getChannel());
      int type = metaEvent.getType();
      byte[] data = metaEvent.getData();

      setTick(tick);
      setChannel(channel);
      setType(type);
      setData(data);

      super.display(midel);
    }

    @Override
    protected Midel getMidel()
    {
      long tick = getTick();
      int channel = Channel.getChannelIndex(getChannel());
      int type = getType();
      byte[] data = getData();
      MetaMessage metaMessage = new MetaMessage();
      Catcher.setMessage(metaMessage, type, data, data.length);
      MetaEvent metaEvent;

      switch (type)
      {
        case MidiConstants.MM_TEMPO:
          metaEvent = new TempoChange(tick, metaMessage);
          break;
        case MidiConstants.MM_TIME_SIGNATURE:
          metaEvent = new TimeSignatureChange(tick, metaMessage);
          break;
        default:
          metaEvent = new MetaEvent(tick, metaMessage);
          break;
      }

      metaEvent.setChannel(channel);
      return metaEvent;
    }

  }

  private class SysexEditor extends MessageTypeEditor
  {
    private JSpinner tickSpinner;
    private JSpinner channelSpinner;
    private JTextField valueTextField;

    private SysexEditor()
    {
      JLabel tickLabel = new JLabel("Tick:");
      add(tickLabel, "x=0,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel channelLabel = new JLabel("Channel:");
      add(channelLabel, "x=1,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      tickSpinner = new JSpinner();
      add(tickSpinner, "x=0,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      channelSpinner = new JSpinner();
      add(channelSpinner, "x=1,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel valueLabel = new JLabel("Value:");
      add(valueLabel, "x=0,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

      valueTextField = new JTextField();
      add(valueTextField, "x=0,y=3,top=0,left=5,bottom=10,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

      makeEqualWidth();

      reset();
    }

    private long getTick()
    {
      long tick = ((Number)tickSpinner.getValue()).longValue();
      return tick;
    }

    private void setTick(long tick)
    {
      tickSpinner.setValue(tick);
    }

    private int getChannel()
    {
      int channel = ((Number)channelSpinner.getValue()).intValue();
      return channel;
    }

    private void setChannel(int channel)
    {
      channelSpinner.setValue(channel);
    }

    private byte[] getData()
    {
      String text = valueTextField.getText();
      byte[] data = Hex.fromHexString(text);
      return data;
    }

    private void setData(byte[] data)
    {
      String text = Hex.toHexString(data);
      valueTextField.setText(text);
    }

    public void reset()
    {
      setTick(DEFAULT_MIDI_TICK);
      setChannel(Channel.getChannelNumber(DEFAULT_MIDI_CHANNEL));
      setData("".getBytes());

      super.reset();
    }

    public void display(Midel midel)
    {
      SysexEvent sysexEvent = (SysexEvent)midel;

      long tick = sysexEvent.getTick();
      int channel = Channel.getChannelNumber(sysexEvent.getChannel());
      byte[] data = sysexEvent.getData();

      setTick(tick);
      setChannel(channel);
      setData(data);

      super.display(midel);
    }

    @Override
    protected Midel getMidel()
    {
      long tick = getTick();
      int channel = Channel.getChannelIndex(getChannel());
      byte[] data = getData();
      SysexMessage sysexMessage = new SysexMessage();
      Catcher.setMessage(sysexMessage, MidiConstants.SYSEX_STATUS, data, data.length);
      SysexEvent sysexEvent = new SysexEvent(tick, sysexMessage);
      sysexEvent.setChannel(channel);
      return sysexEvent;
    }

  }

}
