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

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.riffcrafter.common.midi.Channel;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.common.midi.MidiConstants;
import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.studio.app.Editor;

public class ChannelPanel extends GridBagPanel
{

  private Editor editor;
  private int channel;

  private JTabbedPane channelTabbedPane;
  private ChannelTabPanel channelTabPanel;
  private JPanel[] emptyPanels;

  public ChannelPanel()
  {
    setBorder("empty:5,5,5,5");

    channelTabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

    emptyPanels = new JPanel[MidiConstants.MAX_CHANNELS];

    for (int i = 0; i < MidiConstants.MAX_CHANNELS; i++)
    {
      int channelNumber = Channel.getChannelNumber(i);
      String title = Integer.toString(channelNumber);
      emptyPanels[i] = new JPanel(null);
      channelTabbedPane.addTab(title, emptyPanels[i]);
    }

    channelTabbedPane.addChangeListener(new ChannelTabListener());

    channelTabPanel = new ChannelTabPanel();
    channelTabbedPane.setComponentAt(0, channelTabPanel);

    add(channelTabbedPane, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=c,fill=b,weightx=1,weighty=1,gridwidth=1,gridheight=1");
  }

  public void selectEditor(Editor editor)
  {
    this.editor = editor;
    onChannelTabSelected();
  }

  public void selectChannel(int channel)
  {
    this.channel = channel;
    channelTabbedPane.setSelectedIndex(channel); // results in call to onChannelTabSelected
  }
  
  private void onChannelTabSelected()
  {
    // Ensure the current tab displays the single shared instance of the channelTabPanel
    displayChannelTabPane();
    
    // Ensure the channelTabPanel displays the current editor and channel
    channelTabPanel.displayChannel(editor, channel);
    
    if (editor != null)
    {
      // Ensure the channel is visible in the Notators
      editor.displayChannel(channel);
    }
  }

  /**
   * Ensure the currently selected channel tab displays the
   * single shared instance of the channelTabPanel.
   * 
   * If the currently selected channel tab is already displaying
   * the channelTabPanel then do nothing. Otherwise, determine
   * which tab is displaying the channelTabPanel and set it back
   * to an empty panel, then display the channelTabPanel in the
   * current tab.
   */
  private void displayChannelTabPane()
  {
    channel = channelTabbedPane.getSelectedIndex();
    Component component = channelTabbedPane.getComponent(channel);
    if (component != channelTabPanel)
    {
      int previousChannel = channelTabbedPane.indexOfComponent(channelTabPanel);
      if (previousChannel != -1)
      {
        channelTabbedPane.setComponentAt(previousChannel, emptyPanels[previousChannel]);
      }
      channelTabbedPane.setComponentAt(channel, channelTabPanel);
    }
  }

  public void onNotatorInstrumentChange(int channel, int instrument)
  {
    if (channel == this.channel)
    {
      channelTabPanel.onNotatorInstrumentChange(instrument);
    }
  }

  public void onMidiChange()
  {
    channelTabPanel.onMidiChange();
  }

  public void onMidiSelect(Midi selection)
  {
    // Selection can be null on undo
    if (selection != null)
    {
      // NB: AnalyzerPanel.onMidiSelect selects notes for whatever channel we select here
      // NB: We also use this when file is first opened to get first active channel
      int[] activeChannels = selection.getActiveChannels();
      if (activeChannels.length > 0)
      {
        channel = activeChannels[0];
        channelTabbedPane.setSelectedIndex(channel); // this invokes ChannelPanel.selectChannel via stateChanged
        channelTabPanel.onMidiSelect(selection);
      }
    }
  }

  private class ChannelTabListener implements ChangeListener
  {
    public void stateChanged(ChangeEvent e)
    {
      onChannelTabSelected();
    }
  }

  public class ChannelTabPanel extends GridBagPanel
  {
    private Settings settings;
    private JTabbedPane tabbedPane;
    private AnalyzerPanel analyzerPanel;

    public ChannelTabPanel()
    {
      setBorder("empty:5,5,5,5");
      tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM, JTabbedPane.SCROLL_TAB_LAYOUT);
      add(tabbedPane, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=w,fill=b,weightx=1,weighty=1,gridwidth=1,gridheight=1");

      settings = new Settings();
      tabbedPane.addTab("Control", settings);

      analyzerPanel = new AnalyzerPanel();
      tabbedPane.addTab("Analysis", analyzerPanel);
    }

    public void onNotatorInstrumentChange(int instrument)
    {
      settings.onNotatorInstrumentChange(instrument);
    }

    public void onMidiChange()
    {
      analyzerPanel.onMidiChange();
    }

    public void onMidiSelect(Midi selection)
    {
      // TODO: Determine whether we can afford to select the item in the analyzer tree even if analyzer not visible (i.e. because we want it selected if it should become visible)
      if (tabbedPane.getSelectedComponent() == analyzerPanel)
      {
        displayChannel(editor, channel);
        analyzerPanel.onMidiSelect(selection);
      }
    }

    public void displayChannel(Editor editor, int channel)
    {
      settings.displayChannel(editor, channel);
      analyzerPanel.displayChannel(editor, channel);
    }

  }

  public class Settings extends GridBagPanel
  {
    private KeyboardInputPanel keyboardInputPanel;
    private RecorderInputPanel recorderInputPanel;
    private MidiInputPanel midiInputPanel;

    private InstrumentPanel instrumentPanel;
    private SynthesizerPanel synthesizerPanel;

    public Settings()
    {
      InputPanel inputPanel = new InputPanel();
      inputPanel.setBorder("empty:5,5,0,5;titled:Input Source");
      add(inputPanel, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=nw,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

      //      keyboardInputPanel = new KeyboardInputPanel();
      //      keyboardInputPanel.setBorder("empty:5,5,0,5;titled:Keyboard Input");
      //      add(keyboardInputPanel, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=nw,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");
      //
      //      recorderInputPanel = new RecorderInputPanel();
      //      recorderInputPanel.setBorder("empty:5,5,0,5;titled:Microphone Input");
      //      add(recorderInputPanel, "x=0,y=1,top=0,left=0,bottom=0,right=0,anchor=nw,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

      instrumentPanel = new InstrumentPanel();
      instrumentPanel.setBorder("empty:5,5,0,5;titled:Instrument Assignment");
      add(instrumentPanel, "x=0,y=2,top=0,left=0,bottom=0,right=0,anchor=nw,fill=b,weightx=1,weighty=1,gridwidth=*,gridheight=1");

      synthesizerPanel = new SynthesizerPanel();
      synthesizerPanel.setBorder("empty:5,5,5,5;titled:Channel Overrides");
      add(synthesizerPanel, "x=0,y=3,top=0,left=0,bottom=0,right=0,anchor=nw,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");
    }

    public void onNotatorInstrumentChange(int instrument)
    {
      instrumentPanel.onNotatorInstrumentChange(instrument);
    }

    public void displayChannel(Editor editor, int channel)
    {
      keyboardInputPanel.selectChannel(editor, channel);
      recorderInputPanel.selectChannel(editor, channel);
      midiInputPanel.selectChannel(editor, channel);
      instrumentPanel.selectChannel(editor, channel);
      synthesizerPanel.selectChannel(editor, channel);
    }

    public class InputPanel extends GridBagPanel
    {
      private InputPanel()
      {
        keyboardInputPanel = new KeyboardInputPanel();
        keyboardInputPanel.setBorder("empty:10,5,5,5");

        recorderInputPanel = new RecorderInputPanel();
        recorderInputPanel.setBorder("empty:10,5,5,5");

        midiInputPanel = new MidiInputPanel();
        midiInputPanel.setBorder("empty:10,5,5,5");

        JTabbedPane inputTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM, JTabbedPane.SCROLL_TAB_LAYOUT);
        inputTabbedPane.addTab("Keyboard", keyboardInputPanel);
        inputTabbedPane.addTab("Microphone", recorderInputPanel);
        inputTabbedPane.addTab("MIDI", midiInputPanel);
        add(inputTabbedPane, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=nw,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
      }
    }
  }

}
