// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.io.File;
import java.util.Arrays;

import javax.sound.midi.Sequence;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.tree.TreePath;

import com.riffcrafter.common.midi.Catcher;
import com.riffcrafter.common.midi.Midel;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.common.midi.MidiConstants;
import com.riffcrafter.common.midi.Note;
import com.riffcrafter.common.midi.Midi.MidiListener;
import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.library.util.Broker;
import com.riffcrafter.library.util.Resources;
import com.riffcrafter.library.util.Broker.Listener;
import com.riffcrafter.studio.app.Analyzer.AnalyzerNode;
import com.riffcrafter.studio.dialog.CommonDialog;
import com.riffcrafter.studio.dialog.KeyboardInputPanel;
import com.riffcrafter.studio.dialog.FileDialog.SaveDialog;
import com.riffcrafter.studio.midi.StaffNotator;
import com.riffcrafter.studio.midi.GraphicalNotator;
import com.riffcrafter.studio.midi.Notator;
import com.riffcrafter.studio.midi.Player;
import com.riffcrafter.studio.midi.Scroller;
import com.riffcrafter.studio.midi.Tabulator;
import com.riffcrafter.studio.midi.Notator.NotatorListener;
import com.riffcrafter.studio.undo.UndoManager;
import com.riffcrafter.studio.undo.UndoableAdd;
import com.riffcrafter.studio.undo.UndoableRemove;

public class Editor extends JPanel
{
  private String fileName;
  private Midi midi;

  private Studio studio;

  private Notator graphicalNotator;
  private Scroller graphicalScroller;

  private Notator staffNotator;
  private Scroller staffScroller;

  private Bridger bridger;
  private JTabbedPane leftTabbedPane;

  private boolean isModified;
  private String shortTitle;

  private static int untitledCount;
  private int untitledNumber;

  private SelectionManager selectionManager = new SelectionManager();

  private UndoManager undoManager = new UndoManager(this);
  private int undoIndex = undoManager.getUndoIndex();

  private EditorMidiListener editorMidiListener = new EditorMidiListener();
  private NotatorListener editorNotatorListener = new EditorNotatorListener();

  private int velocity[] = new int[MidiConstants.MAX_CHANNELS];
  private int duration[] = new int[MidiConstants.MAX_CHANNELS];
  private int articulation[] = new int[MidiConstants.MAX_CHANNELS];
  private boolean isMute[] = new boolean[MidiConstants.MAX_CHANNELS];
  private boolean isSolo[] = new boolean[MidiConstants.MAX_CHANNELS];
  private AnalyzerNode channelAnalysis[] = new AnalyzerNode[MidiConstants.MAX_CHANNELS];
  private TreePath channelAnalysisSelection[] = new TreePath[MidiConstants.MAX_CHANNELS];
  private GridBagPanel graphicalNotatorPanel;
  private GridBagPanel staffNotatorPanel;
  private Tabulator tabulator;

  public Editor(Studio studio)
  {
    super(new BorderLayout());
    this.studio = studio;

    Arrays.fill(velocity, KeyboardInputPanel.DEFAULT_VELOCITY);
    Arrays.fill(duration, KeyboardInputPanel.DEFAULT_DURATION);
    Arrays.fill(articulation, KeyboardInputPanel.DEFAULT_ARTICULATION);
  }

  /**
   * To be used when creating a new file.
   */
  public void openNew()
  {
    midi = new Midi();
    initialize();
    open(null);
  }

  /**
   * To be used when editing an existing file
   */
  public void openFile(String fileName)
  {
    midi = new Midi(fileName);
    initialize();
    open(fileName);
    selectLowestChannel();
  }

  private void selectLowestChannel()
  {
    int[] channels = midi.getActiveChannels();
    if (channels.length > 0)
    {
      studio.selectChannel(channels[0]);
    }
  }
  
  private void initialize()
  {
    bridger = new Bridger();

    graphicalNotator = new GraphicalNotator(midi, selectionManager);
    graphicalScroller = new Scroller(graphicalNotator, midi, bridger);

    graphicalNotatorPanel = new GridBagPanel();
    graphicalNotatorPanel.setBorder("empty:5,5,5,5");
    graphicalNotatorPanel.add(graphicalScroller, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=e,fill=b,weightx=1,weighty=1,gridwidth=1,gridheight=1");

    staffNotator = new StaffNotator(midi, selectionManager);
    staffScroller = new Scroller(staffNotator, midi, bridger);

    staffNotatorPanel = new GridBagPanel();
    staffNotatorPanel.setBorder("empty:5,5,5,5");
    staffNotatorPanel.add(staffScroller, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=e,fill=b,weightx=1,weighty=1,gridwidth=1,gridheight=1");

    tabulator = new Tabulator(midi, selectionManager, bridger);
    leftTabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    leftTabbedPane.addTab("Graphical View", graphicalNotatorPanel);
    leftTabbedPane.addTab("Staff View", staffNotatorPanel);
    leftTabbedPane.addTab("Event View", tabulator);

    GridBagPanel leftPanel = new GridBagPanel();
    leftPanel.setBorder("empty:5,5,5,5");
    leftPanel.add(leftTabbedPane, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=e,fill=b,weightx=1,weighty=1,gridwidth=1,gridheight=1");

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(leftPanel, BorderLayout.CENTER);
    add(panel, BorderLayout.CENTER);
  }

  /**
   * Common helper for all open methods... not to be used directly.
   */
  private void open(String fileName)
  {
    this.fileName = fileName;
    midi.addMidiListener(editorMidiListener);
    graphicalNotator.addNotatorListener(editorNotatorListener);
    staffNotator.addNotatorListener(editorNotatorListener);
  }

  public String getFileName()
  {
    return fileName;
  }

  public String getTitle()
  {
    String title = fileName;

    if (title == null)
    {
      if (untitledNumber == 0)
      {
        untitledNumber = ++untitledCount;
      }
      title = Resources.format("Editor.Title.Default", untitledNumber);
    }

    shortTitle = new File(title).getName();

    if (isModified)
      title += " " + Resources.get("Editor.Title.Modified");

    return title;
  }

  public String getShortTitle()
  {
    if (shortTitle == null)
    {
      getTitle();
    }
    return shortTitle;
  }

  public void processCommand(ActionEvent e)
  {
    String command = e.getActionCommand();
    if (command.equals("Save"))
    {
      saveFile();
    }
    else if (command.equals("SaveAs"))
    {
      saveAs();
    }
    else if (command.equals("Close"))
    {
      close();
    }
    else if (command.equals("Begin"))
    {
      setCurrentTick(0);
    }
    else if (command.equals("End"))
    {
      setCurrentTick(midi.getMaxTick());
    }
    else if (command.equals("ToggleChannelStyle"))
    {
      graphicalNotator.toggleChannelStyle();
    }
    else if (command.equals("Cut"))
    {
      cut();
    }
    else if (command.equals("Copy"))
    {
      copy();
    }
    else if (command.equals("Paste"))
    {
      paste();
    }
    else if (command.equals("Delete"))
    {
      delete();
    }
    else if (command.equals("SelectAll"))
    {
      selectAll();
    }
    else if (command.equals("Undo"))
    {
      undo();
    }
    else if (command.equals("Redo"))
    {
      redo();
    }
    else if (command.equals("MoveUp"))
    {
      transpose(1);
    }
    else if (command.equals("MoveDown"))
    {
      transpose(-1);
    }
    else if (command.equals("MoveEarlier"))
    {
      modifyTicks(-20);
    }
    else if (command.equals("MoveLater"))
    {
      modifyTicks(+20);
    }
    else if (command.equals("Shorten"))
    {
      modifyDuration(-20);
    }
    else if (command.equals("Lengthen"))
    {
      modifyDuration(+20);
    }
    else if (command.equals("ChannelUp"))
    {
      modifyChannel(-1);
    }
    else if (command.equals("ChannelDown"))
    {
      modifyChannel(+1);
    }
    else if (command.equals("ReverseTime"))
    {
      reverseTimeByChannel();
    }
    else if (command.equals("ReversePitch"))
    {
      reversePitchByChannel();
    }
    else if (command.equals("EndNote"))
    {

    }
  }

  public void addNoteRelative(Note note, long gap, boolean isToBePlayed)
  {
    long tick = getCurrentTick();
    note.setTick(tick);
    addNoteAbsolute(note, isToBePlayed);
    setCurrentTick(tick + note.getDuration() + gap);
  }

  public void addNoteAbsolute(Note note, boolean isToBePlayed)
  {
    Midi targetMidi = new Midi();
    targetMidi.add(note);
    pasteAbsolute(targetMidi, isToBePlayed);
    long tick = note.getTick() + note.getDuration();
    setCurrentTick(tick);
  }

  public void addNote(Note note)
  {
    int channel = note.getChannel();
    int velocity = getVelocity(channel);
    int duration = getDuration(channel);
    int articulation = getArticulation(channel);
    long noteDuration = KeyboardInputPanel.articulate(duration, articulation);
    note.setVelocity(velocity);
    note.setDuration(noteDuration);
    midi.add(note);
    Midi newMidi = new Midi();
    newMidi.add(note);
    play(newMidi);
  }

  public boolean close()
  {
    if (isModified)
    {
      int response = CommonDialog.showYesNoCancel(this, Resources.get("File.Save.Prompt"));
      if (response == JOptionPane.DEFAULT_OPTION || response == JOptionPane.CANCEL_OPTION)
      {
        return false;
      }

      if (response == JOptionPane.YES_OPTION && !saveFile())
      {
        return false;
      }
    }

    studio.removeFromDesktop(this);
    return true;
  }

  private boolean saveFile()
  {
    boolean isSaved;
    if (fileName == null)
    {
      isSaved = saveAs();
    }
    else
    {
      if (!midi.isCurrentVersion())
      {
        String message = Resources.get("Editor.Save.Overwrite");
        if (!CommonDialog.showYesNo(this, message))
        {
          return false;
        }
      }
      isSaved = save();
    }
    return isSaved;
  }

  private boolean save()
  {
    Sequence sequence1 = midi.toSequence();
    Sequence sequence = sequence1;
    try
    {
      Catcher.write(sequence, MidiConstants.MULTIPLE_TRACK, new File(fileName));
    }
    catch (Exception e)
    {
      CommonDialog.showOkay(this, e.getMessage());
      return false;
    }
    setModified(false);
    undoIndex = undoManager.getUndoIndex();
    midi.setCurrentVersion(true);
    return true;
  }

  private boolean saveAs()
  {
    SaveDialog saveDialog = new SaveDialog(this);
    String fileName = saveDialog.showDialog();
    if (fileName == null)
    {
      return false;
    }
    Editor editor = studio.find(fileName);
    if (editor != null && editor != this)
    {
      // User has already been prompted to overwrite
      studio.removeFromDesktop(editor);
    }
    this.fileName = fileName;
    return save();
  }

  private void cut()
  {
    copy();
    delete();
  }

  protected void copy()
  {
    Midi selection = getSelection();
    if (selection == null)
    {
      CommonDialog.showOkay(this, Resources.get("Selection.Empty"));
      return;
    }
    String base64 = selection.toBase64();
    StringSelection stringSelection = new StringSelection(base64);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, stringSelection);
  }

  private void paste()
  {
    Midi clipboardMidi = getClipboardMidi();
    if (clipboardMidi != null)
    {
      boolean isFillToMeasure = studio.isFillToMeasure();
      boolean isAdvanceOnPaste = isFillToMeasure;
      pasteRelative(clipboardMidi, true, isAdvanceOnPaste, isFillToMeasure, true);
    }
  }

  public Midi getClipboardMidi()
  {
    Midi clipboardMidi = null;
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    Transferable transferable = clipboard.getContents(null);
    if (transferable != null)
    {
      try
      {
        String base64 = (String)transferable.getTransferData(DataFlavor.stringFlavor);
        clipboardMidi = Midi.fromBase64(base64);
      }
      catch (Exception e)
      {
        JOptionPane.showMessageDialog(this, Resources.get("Exception.ClipboardDataFormat"));
      }
    }
    return clipboardMidi;
  }

  public void redo()
  {
    if (undoManager.redo())
    {
      setModified(undoManager.getUndoIndex() != undoIndex);
      setSelection(null, false, false);
      setCurrentTick(undoManager.getCurrentTick());
    }
    else
    {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  public void undo()
  {
    if (undoManager.undo())
    {
      setModified(undoManager.getUndoIndex() != undoIndex);
      setSelection(null, false, false);
      setCurrentTick(undoManager.getCurrentTick());
    }
    else
    {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  // Menu/toolbar item
  public void play()
  {
    Player player = studio.getPlayer();

    player.stop();

    long tickPosition;
    Sequence sequence;

    Midi selection = getSelection();
    if (studio.isSelection() && selection != null && selection.size() > 0)
    {
      tickPosition = selection.getFirstTick();
      sequence = selection.toSequence();
    }
    else
    {
      tickPosition = getCurrentTick();
      sequence = midi.toSequence();
    }

    player.play(sequence, tickPosition, this, bridger);
  }

  public void playVisual(Midi midi)
  {
    Player player = studio.getPlayer();
    player.stop();
    long tickPosition = midi.getFirstTick();
    Sequence sequence = midi.toSequence();
    player.play(sequence, tickPosition, this, bridger);
  }

  public void play(Midi midi)
  {
    Player player = studio.getPlayer();
    player.stop();
    long tickPosition = midi.getFirstTick();
    Sequence sequence = midi.toSequence();
    player.play(sequence, tickPosition, this, null);
  }

  public void rest(long duration)
  {
    long tick = getCurrentTick();
    tick += duration;
    setCurrentTick(tick);
  }

  public void play(int channel, int key, int velocity)
  {
    studio.getPlayer().play(this, channel, key, velocity);
  }

  public void stop(int channel, int key)
  {
    studio.getPlayer().stop(channel, key);
  }

  public void modifyTicks(long tick, int ticks)
  {
    midi.modifyTicks(tick, ticks);
  }

  private void setModified(boolean isModified)
  {
    this.isModified = isModified;
    studio.onModify(this);
  }

  public Midi getMidi()
  {
    return midi;
  }

  public long getMaxTick()
  {
    return midi.getMaxTick();
  }

  public long getCurrentTick()
  {
    long currentTick;
    Component selectedComponent = leftTabbedPane.getSelectedComponent();
    if (selectedComponent == graphicalNotatorPanel)
    {
      currentTick = graphicalScroller.getCurrentTick();
    }
    else if (selectedComponent == staffNotatorPanel)
    {
      currentTick = staffScroller.getCurrentTick();
    }
    else if (selectedComponent == tabulator)
    {
      currentTick = tabulator.getCurrentTick();
    }
    else
    {
      throw new RuntimeException("Invalid component");
    }
    return currentTick;
  }

  public Object getCurrentView()
  {
    Object currentView;
    Component selectedComponent = leftTabbedPane.getSelectedComponent();
    if (selectedComponent == graphicalNotatorPanel)
    {
      currentView = graphicalNotator;
    }
    else if (selectedComponent == staffNotatorPanel)
    {
      currentView = staffNotator;
    }
    else if (selectedComponent == tabulator)
    {
      currentView = tabulator;
    }
    else
    {
      throw new RuntimeException("Invalid component");
    }
    return currentView;
  }

  public void setCurrentTick(long tick)
  {
    bridger.setCurrentTick(tick);
  }

  public Midi getSelection()
  {
    return selectionManager.getSelection();
  }

  public Midi getSelection(boolean isIncludeInstrument, boolean isIncludeTempo)
  {
    Midi selection = selectionManager.getSelection();
    if (selection != null && selection.size() > 0)
    {
      long firstTick = selection.getFirstTick();
      selection = selection.modifyTicks(-firstTick);
      if (isIncludeInstrument)
      {
        selection.applyProgramsFrom(midi, firstTick, 0);
      }
      if (isIncludeTempo)
      {
        selection.applyTempoFrom(midi, firstTick, 0);
      }
    }
    return selection;
  }

  public void setSelection(Midi selection, boolean isToBePlayed, boolean isPlayVisually)
  {
    selectionManager.setSelection(selection, this, isToBePlayed, isPlayVisually);
  }

  public long findTicksPerMeasure(long tick)
  {
    return midi.findTicksPerMeasure(tick);
  }

  public void pasteRelative(Midi sourceMidi)
  {
    pasteRelative(sourceMidi, true);
  }

  public void pasteRelative(Midi sourceMidi, boolean isToBePlayed)
  {
    long minTick = sourceMidi.getFirstTick();
    long currentTick = getCurrentTick();
    Midi targetMidi = new Midi();
    midi.add(sourceMidi, minTick, currentTick, targetMidi);
    setSelection(targetMidi, isToBePlayed, false);
  }

  // TODO: Replace pasteRelative above with pasteRelative below and re-test.

  public void pasteRelative(Midi sourceMidi, boolean isToBePlayed, boolean isAdvanceOnPaste, boolean isFillToMeasure, boolean isResetFirstTick)
  {
    Midi targetMidi = new Midi();
    long currentTick = getCurrentTick();
    long firstTick = sourceMidi.getFirstTick();

    long baseTick = 0;
    if (isResetFirstTick)
    {
      baseTick = firstTick;
    }

    midi.add(sourceMidi, baseTick, currentTick, targetMidi);
    setSelection(targetMidi, isToBePlayed, false);

    if (isAdvanceOnPaste)
    {
      long sourceMidiLength = sourceMidi.getMaxTick() - baseTick;
      long nextTick = currentTick + sourceMidiLength;
      if (isFillToMeasure)
      {
        int ticksPerMeasure = midi.findTicksPerMeasure(currentTick);
        long nextMeasure = (nextTick + ticksPerMeasure - 1) / ticksPerMeasure;
        nextTick = nextMeasure * ticksPerMeasure;
      }
      setCurrentTick(nextTick);
    }
  }

  public void pasteAbsolute(Midi selection)
  {
    pasteAbsolute(selection, true);
  }

  public void pasteAbsolute(Midi targetMidi, boolean isToBePlayed)
  {
    midi.add(targetMidi);
    setSelection(targetMidi, isToBePlayed, false);
  }

  public void delete()
  {
    Midi selection = getSelection();
    if (selection == null)
    {
      CommonDialog.showOkay(this, Resources.get("Selection.Empty"));
      return;
    }
    midi.remove(selection);
    setSelection(null, false, false);
    long maxTick = midi.getMaxTick();
    if (getCurrentTick() > maxTick)
    {
      setCurrentTick(maxTick);
    }
  }

  public void selectAll()
  {
    Midi selection = new Midi();
    for (Midel midel : midi.getMidels())
    {
      selection.add(midel);
    }

    setSelection(selection, false, false);
  }

  public void displayChannel(int channel)
  {
    graphicalScroller.scrollChannelIntoView(channel);
    staffScroller.scrollChannelIntoView(channel);
  }

  private interface Transformer
  {
    public Midi transform(Midi selection);
  }

  public void replaceSelection(Transformer transformer)
  {
    Midi selection = getSelection();
    if (selection == null || selection.size() == 0)
    {
      CommonDialog.showOkay(this, Resources.get("Selection.Empty"));
    }
    else
    {
      Midi targetMidi = transformer.transform(selection);
      if (targetMidi != null)
      {
        midi.remove(selection);
        midi.add(targetMidi);
        setSelection(targetMidi, true, false);
      }
    }
  }

  public void moveNotes(final int deltaTicks, final int deltaKeys)
  {
    replaceSelection(new Transformer()
    {
      public Midi transform(Midi selection)
      {
        return selection.move(deltaTicks, deltaKeys);
      }
    });
  }

  public void transpose(final int deltaKeys)
  {
    replaceSelection(new Transformer()
    {
      public Midi transform(Midi selection)
      {
        return selection.transpose(deltaKeys);
      }
    });
  }

  public void modifyTicks(final int deltaTicks)
  {
    replaceSelection(new Transformer()
    {
      public Midi transform(Midi selection)
      {
        return selection.modifyTicks(deltaTicks);
      }
    });
  }

  public void modifyDuration(final int deltaDuration)
  {
    replaceSelection(new Transformer()
    {
      public Midi transform(Midi selection)
      {
        return selection.modifyDuration(deltaDuration);
      }
    });
  }

  public void modifyChannel(final int deltaChannel)
  {
    replaceSelection(new Transformer()
    {
      public Midi transform(Midi selection)
      {
        return selection.modifyChannel(deltaChannel);
      }
    });
  }

  public void reverseTimeByChannel()
  {
    replaceSelection(new Transformer()
    {
      public Midi transform(Midi selection)
      {
        return selection.reverseTimeByChannel();
      }
    });
  }

  public void reversePitchByChannel()
  {
    replaceSelection(new Transformer()
    {
      public Midi transform(Midi selection)
      {
        return selection.reversePitchByChannel();
      }
    });
  }

  public int getInstrument(int channel)
  {
    return midi.getProgram(channel, getCurrentTick());
  }

  public void setInstrument(int channel, int program, boolean isUpdate)
  {
    long tick = getCurrentTick();
    midi.changeProgram(channel, tick, program, isUpdate);
    studio.getPlayer().getSynthesizer().getChannels()[channel].programChange(program);
  }

  public int getVelocity(int channel)
  {
    return velocity[channel];
  }

  public void setVelocity(int channel, int velocity)
  {
    this.velocity[channel] = velocity;
  }

  public int getDuration(int channel)
  {
    return duration[channel];
  }

  public void setDuration(int channel, int duration)
  {
    this.duration[channel] = duration;
  }

  public int getArticulation(int channel)
  {
    return articulation[channel];
  }

  public void setArticulation(int channel, int articulation)
  {
    this.articulation[channel] = articulation;
  }

  public boolean isMute(int channel)
  {
    return isMute[channel];
  }

  public void setMute(int channel, boolean isMute)
  {
    this.isMute[channel] = isMute;
    studio.getPlayer().setMute(channel, isMute);
  }

  public boolean isSolo(int channel)
  {
    return isSolo[channel];
  }

  public void setSolo(int channel, boolean isSolo)
  {
    this.isSolo[channel] = isSolo;
    studio.getPlayer().setSolo(channel, isSolo);
  }

  public void clearAllAnalysis()
  {
    for (int i = 0; i < channelAnalysis.length; i++)
    {
      channelAnalysis[i] = null;
    }
  }

  public AnalyzerNode getChannelAnalysis(int channel)
  {
    return channelAnalysis[channel];
  }

  public void setChannelAnalysis(int channel, AnalyzerNode root)
  {
    channelAnalysis[channel] = root;
  }

  public TreePath getChannelAnalysisSelection(int channel)
  {
    return channelAnalysisSelection[channel];
  }

  public void setChannelAnalysisSelection(int channel, TreePath path)
  {
    channelAnalysisSelection[channel] = path;
  }

  private class EditorMidiListener implements MidiListener
  {

    public void onAddMidel(Midi midi, Midel addMidel)
    {
      setModified(true);
      undoManager.addEdit(new UndoableAdd(midi, addMidel));
      studio.onMidiChange();
    }

    public void onRemoveMidel(Midi midi, Midel removeMidel)
    {
      setModified(true);
      undoManager.addEdit(new UndoableRemove(midi, removeMidel));
      studio.onMidiChange();
    }

  }

  private class EditorNotatorListener implements NotatorListener
  {
    public void fireMove(int deltaTicks, int deltaKeys)
    {
      moveNotes(deltaTicks, deltaKeys);
    }

    public void fireHover(String text)
    {
      studio.setStatusText(text);
    }

    public void fireInstrumentChange(int channel, int instrument)
    {
      studio.onNotatorInstrumentChange(channel, instrument);
    }

    public void fireNote(Note note)
    {
      addNote(note);
    }
  }

  public class Bridger
  {
    // downward = player to view
    //   upward = view to player

    private Broker downwardBroker = new Broker();
    private Listener upwardListener = new UpwardListener();

    private boolean isBridging;
    private boolean isBusy;

    public void subscribe(Listener downwardListener, Broker upwardBroker)
    {
      downwardBroker.subscribe(downwardListener);
      upwardBroker.subscribe(upwardListener);
    }

    public void start()
    {
      isBridging = true;
    }

    public void stop()
    {
      isBridging = false;
    }

    public void setCurrentTick(long currentTick)
    {
      if (!isBusy)
      {
        isBusy = true;
        downwardBroker.publish(currentTick, this);
        isBusy = false;
      }
    }

    public class UpwardListener implements Listener
    {
      public void notify(Object event, Object source)
      {
        if (!isBusy)
        {
          isBusy = true;

          AdjustmentEvent e = (AdjustmentEvent)event;
          if (isBridging)
          {
            // Route through player, receive notification via setCurrentTick
            studio.getPlayer().onScrollerChange(e);
          }
          else
          {
            // If the player is not running, then short-circuit back to the downward
            // broker... the targets will ignore if they're the source.
            downwardBroker.publish((long)e.getValue(), source);
          }

          isBusy = false;
        }
      }

    }
  }

  public class SelectionManager
  {
    private Midi selection;
    private Broker broker = new Broker();

    public void subscribe(Listener listener)
    {
      broker.subscribe(listener);
    }

    public Midi getSelection()
    {
      return selection;
    }

    public void setSelection(Midi selection, Object source, boolean isToBePlayed, boolean isPlayVisually)
    {
      if (studio.isRoundToMeasure())
      {
        Rectangle rectangleInTicks = graphicalNotator.getSelectionRectangleInTicks();
        if (rectangleInTicks != null)
        {
          int left = rectangleInTicks.x;
          int right = left + rectangleInTicks.width;
          selection = selection.roundToMeasure(left, right);
        }
      }

      this.selection = selection;
      broker.publish(selection, source);

      // TODO: Consider whether everything below here should be moved to subscribers

      studio.onMidiSelect(selection);

      if (selection != null && isToBePlayed)
      {
        if (isPlayVisually)
        {
          playVisual(selection);
        }
        else
        {
          play(selection);
        }
      }
    }
  }

}
