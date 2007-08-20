// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.riffcrafter.common.midi.Channel;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.library.dialog.WidthConstrainedList;
import com.riffcrafter.library.dialog.WidthConstrainedTextField;
import com.riffcrafter.studio.app.Commander;
import com.riffcrafter.studio.app.Editor;


public class CommandPanel extends GridBagPanel
{
  private Editor editor;

  private Commander commander;
  private JTextField textField;
  private JList listBox;
  private DefaultListModel listModel;
  private SettingsPanel settingsPanel;

  public CommandPanel()
  {
    this.commander = new Commander();
    CommandMainPanel commandMainPanel = new CommandMainPanel();
    add(commandMainPanel, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=e,fill=b,weightx=1,weighty=1,gridwidth=1,gridheight=1");
    settingsPanel.fromCommander(commander);
  }

  public void selectEditor(Editor editor)
  {
    this.editor = editor;
  }

  private void onCommandEntered()
  {
    String command = textField.getText();
    if (!(command == null || command.isEmpty()))
    {
      if (!isSameAsLastCommand(command))
      {
        listModel.add(0, command);
      }
      textField.setText(null);
      onCommandEntered(command);
    }
  }

  private void onCommandEntered(String command)
  {
    if (editor == null)
    {
      CommonDialog.showOkay(this, "Please create or open a MIDI file and try again");
      return;
    }
    
    processCommand(command);
    editor.setSelection(null, false, false);
  }

  private boolean isSameAsLastCommand(String command)
  {
    boolean isSame = false;
    if (listModel.size() > 0)
    {
      String historyItem = (String)listModel.get(0);
      isSame = historyItem.equals(command);
    }
    return isSame;
  }

  private void processCommand(String command)
  {
    try
    {
      settingsPanel.toCommander(commander);
      Midi midi = commander.process(editor, command);
      settingsPanel.fromCommander(commander);
      
      if (midi.size() > 0)
      {
        Midi selection = editor.getSelection();
        if (selection == null || selection.size() == 0)
        {
          editor.pasteAbsolute(midi);
          long tick = commander.getTick();
          editor.setCurrentTick(tick);
        }
        else
        {
          long tick = selection.getFirstTick();
          editor.delete();
          editor.setCurrentTick(tick);
          editor.pasteRelative(midi);
        }
      }
      else
      {
        long tick = commander.getTick();
        editor.setCurrentTick(tick);
      }
    }
    catch (Exception e)
    {
      CommonDialog.showOkay(this, e.toString());
    }
  }

  private void restoreFromHistory()
  {
    String command = (String)listBox.getSelectedValue();
    textField.setText(command);
  }

  private void executeFromHistory()
  {
    textField.setText(null);
    String command = (String)listBox.getSelectedValue();
    onCommandEntered(command);
  }
  
  private void clear()
  {
    listModel.removeAllElements();
  }

  private class CommandMainPanel extends GridBagPanel
  {
    private CommandMainPanel()
    {
      JLabel commandLabel = new JLabel("Enter Command:");
      add(commandLabel, "x=0,y=0,top=5,left=5,bottom=0,right=5,anchor=e,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      textField = new WidthConstrainedTextField();
      add(textField, "x=0,y=1,top=0,left=5,bottom=5,right=5,anchor=e,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JButton runButton = new JButton("Run");
      add(runButton, "x=4,y=1,top=0,left=5,bottom=5,right=5,anchor=e,fill=h,weightx=0,weighty=0,gridwidth=1,gridheight=1");

      JLabel listLabel = new JLabel("or Select Command from History:");
      add(listLabel, "x=0,y=2,top=5,left=5,bottom=0,right=5,anchor=e,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      listModel = new DefaultListModel();
      listBox = new WidthConstrainedList(listModel);
      listBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      JScrollPane scrollPane = new JScrollPane(listBox);
      scrollPane.getViewport().setBackground(listBox.getBackground());
      add(scrollPane, "x=0,y=3,top=0,left=5,bottom=5,right=5,anchor=e,fill=b,weightx=1,weighty=1,gridwidth=1,gridheight=2");

      JButton clearButton = new JButton("Clear");
      add(clearButton, "x=4,y=3,top=0,left=5,bottom=5,right=5,anchor=n,fill=h,weightx=0,weighty=0,gridwidth=1,gridheight=1");

      JButton helpButton = new JButton("Help");
      helpButton.addActionListener(new HelpActionListener());
      add(helpButton, "x=4,y=4,top=0,left=5,bottom=5,right=5,anchor=n,fill=h,weightx=0,weighty=0,gridwidth=1,gridheight=1");

      settingsPanel = new SettingsPanel();
      add(settingsPanel, "x=0,y=5,top=5,left=5,bottom=5,right=5,anchor=n,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
      
      textField.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          onCommandEntered();
        }
      });

      runButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          onCommandEntered();
        }
      });

      listBox.addListSelectionListener(new ListSelectionListener()
      {

        public void valueChanged(ListSelectionEvent e)
        {
          restoreFromHistory();
        }
      });

      listBox.addMouseListener(new MouseAdapter()
      {

        @Override
        public void mouseClicked(MouseEvent e)
        {
          super.mouseClicked(e);
          if (e.getClickCount() == 2)
          {
            executeFromHistory();
          }
        }

      });

      clearButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          clear();
        }
      });
    }

  }

  public class HelpActionListener implements ActionListener
  {

    public void actionPerformed(ActionEvent e)
    {
      new HelpDialog(CommandPanel.this, "Commander.Help");
    }

  }

  private class SettingsPanel extends GridBagPanel
  {
    private JSpinner tickSpinner;
    private JSpinner lastTickSpinner;
    private JSpinner channelSpinner;
    private JSpinner keySpinner;
    private JSpinner velocitySpinner;
    private JSpinner durationSpinner;
    private JSpinner gapSpinner;
    private JSpinner octaveSpinner;

    private SettingsPanel()
    {
      JLabel tickLabel = new JLabel("Tick (T)");
      add(tickLabel, "x=0,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
      
      tickSpinner = new JSpinner();
      add(tickSpinner, "x=0,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel lastTickLabel = new JLabel("Last Tick");
      add(lastTickLabel, "x=1,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
      
      lastTickSpinner = new JSpinner();
      add(lastTickSpinner, "x=1,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel channelLabel = new JLabel("Channel (C)");
      add(channelLabel, "x=2,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      SpinnerNumberModel channelSpinnerModel = new SpinnerNumberModel(1, 1, 16, 1);
      channelSpinner = new JSpinner(channelSpinnerModel);
      add(channelSpinner, "x=2,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
      
      JLabel keyLabel = new JLabel("Key (K)");
      add(keyLabel, "x=3,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      SpinnerNumberModel keySpinnerModel = new SpinnerNumberModel(60, 0, 127, 1);
      keySpinner = new JSpinner(keySpinnerModel);
      add(keySpinner, "x=3,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel velocityLabel = new JLabel("Velocity (V)");
      add(velocityLabel, "x=0,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      SpinnerNumberModel velocitySpinnerModel = new SpinnerNumberModel(64, 0, 127, 1);
      velocitySpinner = new JSpinner(velocitySpinnerModel);
      add(velocitySpinner, "x=0,y=3,top=0,left=5,bottom=5,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel durationLabel = new JLabel("Duration (D)");
      add(durationLabel, "x=1,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      durationSpinner = new JSpinner();
      add(durationSpinner, "x=1,y=3,top=0,left=5,bottom=5,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel gapLabel = new JLabel("Gap (G)");
      add(gapLabel, "x=2,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      gapSpinner = new JSpinner();
      add(gapSpinner, "x=2,y=3,top=0,left=5,bottom=5,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel octaveLabel = new JLabel("Octave (O)");
      add(octaveLabel, "x=3,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
      
      SpinnerNumberModel octaveSpinnerModel = new SpinnerNumberModel(4, 1, 9, 1);
      octaveSpinner = new JSpinner(octaveSpinnerModel);
      add(octaveSpinner, "x=3,y=3,top=0,left=5,bottom=5,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
    }
    
    public void fromCommander(Commander commander)
    {
      tickSpinner.setValue((Long)commander.getTick());
      lastTickSpinner.setValue((Long)commander.getLastTick());
      channelSpinner.setValue((Integer)Channel.getChannelNumber(commander.getChannel()));
      keySpinner.setValue((Integer)commander.getKey());
      velocitySpinner.setValue((Integer)commander.getVelocity());
      durationSpinner.setValue((Long)commander.getDuration());
      gapSpinner.setValue((Integer)commander.getGap());
      octaveSpinner.setValue((Integer)commander.getOctave());
    }
    
    public void toCommander(Commander commander)
    {
      commander.setTick(((Number)tickSpinner.getValue()).longValue());
      commander.setLastTick(((Number)lastTickSpinner.getValue()).longValue());
      commander.setChannel(Channel.getChannelIndex(((Number)channelSpinner.getValue()).intValue()));
      commander.setKey(((Number)keySpinner.getValue()).intValue());
      commander.setVelocity((Integer)velocitySpinner.getValue());
      commander.setDuration(((Number)durationSpinner.getValue()).longValue());
      commander.setGap(((Number)gapSpinner.getValue()).intValue());
      commander.setOctave(((Number)octaveSpinner.getValue()).intValue());
    }
  }
}
