// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.dialog;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.riffcrafter.common.midi.Instruments;
import com.riffcrafter.common.midi.Midel;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.common.midi.Note;
import com.riffcrafter.common.midi.Midi.KeyScore;
import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.studio.app.Analyzer;
import com.riffcrafter.studio.app.Editor;
import com.riffcrafter.studio.app.Analyzer.AnalyzerNode;

public class AnalyzerPanel extends GridBagPanel
{
  private Editor editor;
  private int channel;

  private JTree tree;
  private TreePanel treePanel;
  private JRadioButton groupByMeasureRadioButton;
  private JRadioButton groupByGapRadioButton;
  private JSpinner minimumGroupSizeSpinner;
  private JSpinner percentAverageGapSpinner;
  private JTable keySignatureTable;
  private JCheckBox buildGrammarCheckBox;

  private TableModel defaultKeySignatureTableModel = new KeyScoresTableModel();

  private RefreshListener refreshListener = new RefreshListener();
  private boolean isInSelectChannel;
  private boolean isInOnMidiSelect;
  private boolean isInSetSelectionPaths;

  public AnalyzerPanel()
  {
    JLabel structureAnalysisLabel = new JLabel("Structure Analysis:");
    add(structureAnalysisLabel, "x=0,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

    treePanel = new TreePanel();
    add(treePanel, "x=0,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=b,weightx=1,weighty=1,gridwidth=*,gridheight=1");

    JLabel keySignatureLabel = new JLabel("Key Analysis for Selected Item (may be inaccurate if key changes in item):");
    // TODO: This label expanded the width of this tab (and all the others), until I made it fill=h, weightx=1, gridwidth=*
    add(keySignatureLabel, "x=0,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

    keySignatureTable = new JTable();
    keySignatureTable.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
    JScrollPane keySignatureScrollPane = new JScrollPane(keySignatureTable);
    keySignatureScrollPane.getViewport().setBackground(keySignatureTable.getBackground());
    add(keySignatureScrollPane, "x=0,y=3,top=0,left=5,bottom=0,right=5,anchor=w,fill=b,weightx=1,weighty=0.5,gridwidth=*,gridheight=1");

    GroupPanel groupPanel = new GroupPanel();
    add(groupPanel, "x=0,y=4,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

    buildGrammarCheckBox = new JCheckBox("Identify unique and repeating patterns");
    buildGrammarCheckBox.addActionListener(refreshListener);
    buildGrammarCheckBox.setSelected(false);
    add(buildGrammarCheckBox, "x=0,y=5,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

    JButton expandAllButton = new JButton("Expand All");
    expandAllButton.addActionListener(new ExpandAllListener());
    add(expandAllButton, "x=1,y=5,top=5,left=5,bottom=5,right=0,anchor=e,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

    JButton collapseAllButton = new JButton("Collapse All");
    collapseAllButton.addActionListener(new CollapseAllListener());
    add(collapseAllButton, "x=2,y=5,top=5,left=5,bottom=5,right=5,anchor=e,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

  }

  private AnalyzerNode getRoot()
  {
    AnalyzerNode root = null;
    if (editor != null)
    {
      root = editor.getChannelAnalysis(channel);
    }
    return root;
  }

  private AnalyzerNode getAnalysis()
  {
    AnalyzerNode root = getRoot();
    if (root == null)
    {
      root = analyze();
    }
    return root;
  }

  @Override
  protected void paintComponent(Graphics g)
  {
    getAnalysis();
    super.paintComponent(g);
  }

  public void displayChannel(Editor editor, int channel)
  {
    // Now that we propagate selections using onMidiSelect, we execute
    // this code frequently, both for Notator selections, as well as Analyzer
    // selections. This not only uses CPU time, but also causes the Analyzer
    // tree to be collapsed to the selected node. We work around this by only
    // changing the channel if it is not the current channel.
    if (editor != this.editor || channel != this.channel) // TODO: add !isInSelectPaths if necessary
    {
      this.editor = editor;
      this.channel = channel;

      // The key signature analysis is updated via setSelectionPath if a selection exists
      keySignatureTable.setModel(defaultKeySignatureTableModel);

      if (editor == null)
      {
        tree.setModel(null);
      }
      else
      {
        AnalyzerNode root = editor.getChannelAnalysis(channel);
        if (root == null)
        {
          tree.setModel(null);
          repaint();
        }
        else
        {
          setModel(root);
          TreePath path = editor.getChannelAnalysisSelection(channel);
          if (path != null)
          {
            isInSelectChannel = true;
            tree.setSelectionPath(path);
            isInSelectChannel = false;
          }
        }
      }
    }
  }

  public void onMidiChange()
  {
    editor.setChannelAnalysis(channel, null);
    repaint();
  }

  public void onMidiSelect(Midi selection)
  {
    if (!isInSetSelectionPaths)
    {
      isInOnMidiSelect = true;
      TreePath firstPath = null;

      for (Midel midel : selection.getMidels())
      {
        if (midel instanceof Note)
        {
          Note note = (Note)midel;
          if (note.getChannel() == channel)
          {
            TreePath treePath = find(note);
            if (firstPath == null)
            {
              firstPath = treePath;
              collapseAll();
            }
            // We have to be careful here because selecting a node should result
            // in our CatchEverythingSelectionMode.setSelectionPaths being invoked.
            // I verified that this is the case when SINGLE_TREE_SELECTION, else
            // not the case. However, we leave our isIn... check in place just in
            // case this changes.
            //
            // In some ways, it would be nice to propagate this far enough so that it
            // could display the key analysis. However, it is a bit tricky because
            // we would want to do the key analysis on all the selected notes, which
            // implies a different kind of analysis than that which is done by the
            // select method.
            tree.addSelectionPath(treePath);
          }
        }
      }

      if (firstPath != null)
      {
        tree.scrollPathToVisible(firstPath);
      }

      isInOnMidiSelect = false;
    }
  }

  @SuppressWarnings("unchecked")
  private TreePath find(Note note)
  {
    AnalyzerNode root = getAnalysis();
    Enumeration enumeration = root.depthFirstEnumeration();
    while (enumeration.hasMoreElements())
    {
      AnalyzerNode analyzerNode = (AnalyzerNode)enumeration.nextElement();
      Object userObject = analyzerNode.getUserObject();
      if (note.equals(userObject))
      {
        TreeNode[] path = analyzerNode.getPath();
        TreePath treePath = new TreePath(path);
        return treePath;
      }
    }
    return null;
  }

  @SuppressWarnings("unused")
  private void expandAll()
  {
    TreePath selectionPath = tree.getSelectionPath();

    for (int i = 0; i < tree.getRowCount(); i++)
    {
      tree.expandRow(i);
    }

    tree.scrollPathToVisible(selectionPath);
  }

  private void expandSelectedNode()
  {
    TreePath selectionPath = tree.getSelectionPath();
    if (selectionPath == null)
    {
      selectionPath = tree.getPathForRow(0);
      if (selectionPath == null)
      {
        return;
      }
    }

    TreePath path;
    int selectedRow = tree.getRowForPath(selectionPath);
    do
    {
      tree.expandRow(selectedRow++);
      path = tree.getPathForRow(selectedRow);
    }
    while (path != null && selectionPath.isDescendant(path));

    tree.scrollPathToVisible(selectionPath);
  }

  @SuppressWarnings("unused")
  private void collapseAll()
  {
    int rowCount = tree.getRowCount();

    for (int i = rowCount - 1; i >= 0; i--)
    {
      tree.collapseRow(i);
    }
  }

  private void collapseSelectedNode()
  {
    TreePath selectionPath = tree.getSelectionPath();
    if (selectionPath == null)
    {
      selectionPath = tree.getPathForRow(0);
      if (selectionPath == null)
      {
        return;
      }
    }

    int firstRow = tree.getRowForPath(selectionPath);
    int nextRow = firstRow + 1;
    while (selectionPath.isDescendant(tree.getPathForRow(nextRow)))
    {
      nextRow++;
    }

    for (int i = nextRow - 1; i >= firstRow; i--)
    {
      tree.collapseRow(i);
    }

    tree.scrollPathToVisible(selectionPath);
  }

  private AnalyzerNode analyze()
  {
    if (editor == null)
    {
      return null;
    }

    boolean isBuildGrammar = buildGrammarCheckBox.isSelected();
    boolean isGroupByMeasure = groupByMeasureRadioButton.isSelected();
    boolean isGroupByGap = groupByGapRadioButton.isSelected();
    int minimumGroupSize = ((Number)minimumGroupSizeSpinner.getValue()).intValue();
    int percentAverageGap = ((Number)percentAverageGapSpinner.getValue()).intValue();

    Midi midi = editor.getMidi();
    Analyzer analyzer = new Analyzer();
    AnalyzerNode root = analyzer.createInitialTree(midi, channel, isGroupByMeasure, isGroupByGap, minimumGroupSize, percentAverageGap);
    if (isBuildGrammar)
    {
      analyzer.buildGrammar(root);
    }
    setModel(root);
    editor.setChannelAnalysis(channel, root);
    editor.setChannelAnalysisSelection(channel, null);
    return root;
  }

  private void setModel(AnalyzerNode root)
  {
    DefaultTreeModel treeModel = new DefaultTreeModel(root, false);
    tree.setModel(treeModel);
  }

  private void select(AnalyzerNode analyzerNode)
  {
    if (analyzerNode != null)
    {
      Midi midi = analyzerNode.getMidi();
      if (editor != null)
      {
        // Do not play selection when user switches between file or channel tabs
        boolean isToBePlayed = !isInSelectChannel;
        boolean isToBePlayedVisually = isToBePlayed;
        editor.setSelection(midi, isToBePlayed, isToBePlayedVisually);
      }
      if (channel != Instruments.DRUM_CHANNEL)
      {
        ArrayList<KeyScore> keyScores = midi.getKeyScores(50);
        TableModel tableModel = new KeyScoresTableModel(keyScores);
        keySignatureTable.setModel(tableModel);
      }
    }
  }

  private void select(TreePath path)
  {
    editor.setChannelAnalysisSelection(channel, path);
    AnalyzerNode analyzerNode = (AnalyzerNode)path.getLastPathComponent();
    select(analyzerNode);
  }

  private class GroupPanel extends GridBagPanel
  {
    private GroupPanel()
    {
      setBorder("titled:Form Groups of Notes...");

      ButtonGroup buttonGroup = new ButtonGroup();

      JRadioButton groupNoneRadioButton = new JRadioButton("Using only chords");
      groupNoneRadioButton.addActionListener(refreshListener);
      buttonGroup.add(groupNoneRadioButton);
      add(groupNoneRadioButton, "x=0,y=0,top=0,left=5,bottom=0,right=5,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=*,gridheight=1");

      groupByMeasureRadioButton = new JRadioButton("Using the chords and notes in each measure");
      groupByMeasureRadioButton.addActionListener(refreshListener);
      buttonGroup.add(groupByMeasureRadioButton);
      groupByMeasureRadioButton.setSelected(true);
      add(groupByMeasureRadioButton, "x=0,y=1,top=5,left=5,bottom=0,right=5,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=*,gridheight=1");

      groupByGapRadioButton = new JRadioButton("From");
      groupByGapRadioButton.addActionListener(refreshListener);
      buttonGroup.add(groupByGapRadioButton);
      add(groupByGapRadioButton, "x=0,y=2,top=5,left=5,bottom=0,right=0,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

      SpinnerNumberModel minimumGroupSizeModel = new SpinnerNumberModel(5, 1, 99, 1);
      minimumGroupSizeSpinner = new JSpinner(minimumGroupSizeModel);
      add(minimumGroupSizeSpinner, "x=1,y=2,top=5,left=0,bottom=0,right=0,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

      JLabel groupLabel1 = new JLabel("or more notes when gap exceeds");
      add(groupLabel1, "x=2,y=2,top=5,left=5,bottom=0,right=0,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

      SpinnerNumberModel percentAverageGapModel = new SpinnerNumberModel(200, 1, 500, 1);
      percentAverageGapSpinner = new JSpinner(percentAverageGapModel);
      add(percentAverageGapSpinner, "x=3,y=2,top=5,left=5,bottom=0,right=0,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

      JLabel groupLabel2 = new JLabel("% of average");
      add(groupLabel2, "x=4,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
    }
  }

  public class ExpandAllListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      expandSelectedNode();
    }
  }

  public class CollapseAllListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      collapseSelectedNode();
    }
  }

  public class RefreshListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      editor.clearAllAnalysis();
      analyze();
    }
  }

  private class TreePanel extends GridBagPanel
  {
    private TreePanel()
    {
      tree = new JTree(new Object[0]);
      tree.setSelectionModel(new CatchEverythingSelectionModel());
      tree.setDragEnabled(true);
      //      tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
      tree.setRootVisible(true);
      JScrollPane scrollPane = new JScrollPane(tree);
      add(scrollPane, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=e,fill=b,weightx=1,weighty=1,gridwidth=1,gridheight=1");
    }
  }

  // This is how you get a JTree to notify you on mouse click or
  // keyboard key, even if the tree node is already selected.

  public class CatchEverythingSelectionModel extends DefaultTreeSelectionModel
  {

    @Override
    public void setSelectionPaths(TreePath[] pPaths)
    {
      super.setSelectionPaths(pPaths);
      // Here we worry about two kinds of infinite recursion. The first
      // is when the user has selected some notes in the Notator, and
      // that's been propagated as far as setting them in the analysis
      // tree. We don't want to detect that as a normal user-clicks-on
      // nodes-in-analysis-tree and select the notes again. The second
      // is when the user clicks on the notes in the analysis tree. That
      // makes it to the Notators and on to OnMidiSelect, but before
      // getting to our inOnMidiSelect, there's a call to selectChannel,
      // which would also cause recursion. There must be a better way. Grrr.
      if (!isInOnMidiSelect && !isInSetSelectionPaths)
      {
        if (pPaths[0] != null)
        {
          isInSetSelectionPaths = true;
          select(pPaths[0]);
          isInSetSelectionPaths = false;
        }
      }
    }

  }

  private class KeyScoresTableModel implements TableModel
  {
    private ArrayList<KeyScore> keyScores;
    private ArrayList<TableModelListener> listeners = new ArrayList<TableModelListener>();
    private String[] columnNames = new String[] { "Rank", "Key", "Synopsis", "Relative Key", "Accidentals", "Triads", "Thirds" };

    public KeyScoresTableModel()
    {
    }

    public KeyScoresTableModel(ArrayList<KeyScore> keyScores)
    {
      this.keyScores = keyScores;
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
      return keyScores == null ? 0 : columnNames.length;
    }

    public String getColumnName(int columnIndex)
    {
      return columnNames[columnIndex];
    }

    public int getRowCount()
    {
      return keyScores == null ? 0 : keyScores.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
      Object value = null;
      if (keyScores != null)
      {
        KeyScore keyScore = keyScores.get(rowIndex);
        switch (columnIndex)
        {
          case 0:
            value = keyScore.getRank();
            break;
          case 1:
            value = keyScore.getKey();
            break;
          case 2:
            value = keyScore.getSynopsis();
            break;
          case 3:
            value = keyScore.getRelativeKey();
            break;
          case 4:
            value = keyScore.getAccidentals();
            break;
          case 5:
            value = keyScore.getTriads();
            break;
          case 6:
            value = keyScore.getThirds();
            break;
        }
      }
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

}
