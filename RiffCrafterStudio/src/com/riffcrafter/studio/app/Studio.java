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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.library.dialog.TopEtchedBorder;
import com.riffcrafter.library.services.Request;
import com.riffcrafter.library.services.Response;
import com.riffcrafter.library.util.Helper;
import com.riffcrafter.library.util.Resources;
import com.riffcrafter.library.util.Settings;
import com.riffcrafter.studio.dialog.CommonDialog;
import com.riffcrafter.studio.dialog.ControlPanel;
import com.riffcrafter.studio.dialog.FileDialog.OpenDialog;
import com.riffcrafter.studio.midi.Player;

public final class Studio extends JFrame
{
  private JToolBar toolBar;
  private StatusBar statusBar;
  private ControlPanel controlPanel;
  private JTabbedPane tabbedPane;
  private JToggleButton playButton;

  private Action repeatAction;
  private Action selectionAction;
  private Action roundToMeasureAction;
  private Action fillToMeasureAction;

  private Player player;

  private ActionListener actionListener = new StudioActionListener();
  private StudioWindowListener studioWindowListener = new StudioWindowListener();
  private StudioComponentListener studioComponentListener = new StudioComponentListener();
  private DesktopChangeListener desktopChangeListener = new DesktopChangeListener();

  public static void main(final String[] args)
  {
    final Studio studio = new Studio();
    studio.processArguments(args);
    EventQueue.invokeLater(new Runnable()
    {
      public void run()
      {
        studio.createUserInterface(args);
      }
    });
  }

  public void createUserInterface(final String[] args)
  {
    setSystemLookAndFeel();

    Splash splash = new Splash();

    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // route everything through close

    player = new Player(this);

    JMenuBar menuBar = Resources.getMenuBar("Main", actionListener);
    setJMenuBar(menuBar);

    toolBar = Resources.getToolBar("Main", actionListener);
    toolBar.setFloatable(false);
    add(toolBar, BorderLayout.NORTH);

    String welcomeText = Resources.get("Application.Welcome", "");
    statusBar = new StatusBar(welcomeText);
    add(statusBar, BorderLayout.SOUTH);

    setIconImage(Resources.getImage("Application.Desktop.Icon"));

    JPanel borderPanel = new JPanel(new BorderLayout());
    borderPanel.setBorder(new CompoundBorder(new TopEtchedBorder(this), new EmptyBorder(5, 5, 5, 5)));
    add(borderPanel, BorderLayout.CENTER);

    tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    tabbedPane.addChangeListener(desktopChangeListener);
    tabbedPane.setVisible(false);
    borderPanel.add(tabbedPane, BorderLayout.CENTER);

    controlPanel = new ControlPanel();
    controlPanel.setBorder("empty:0,5,0,0");
    borderPanel.add(controlPanel, BorderLayout.EAST);

    updateTitle(null);
    restoreWindowParameters();

    addComponentListener(studioComponentListener);
    addWindowListener(studioWindowListener);

    setVisible(true);

    splash.invokeAfterLicenseAcceptance(new Runnable()
    {
      public void run()
      {
        processFiles(args);
      }
    });

  }

  public Image getResourceImage(String iconKey)
  {
    try
    {
      return Resources.getImage(iconKey);
    }
    catch (Exception e)
    {
      return null;
    }
  }

  public void processArguments(String[] args)
  {
    for (int i = 0; i < args.length; i++)
    {
      if (args[i].startsWith("--"))
      {
        if (args[i].equals("--clear-settings"))
        {
          Settings.clear();
          System.exit(0);
        }
        else
        {
          System.err.println("Usage: java Studio [--clear-settings] [midi-files]");
          System.exit(1);
        }
      }
    }
  }

  private void processFiles(String[] args)
  {
    if (args.length == 0)
    {
      promptForInitialAction();
      return;
    }

    for (int i = 0; i < args.length; i++)
    {
      if (!args[i].startsWith("--"))
      {
        openFile(args[i]);
      }
    }

  }

  private void setSystemLookAndFeel()
  {
    try
    {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  public void setStatusText(String text)
  {
    statusBar.setText(text);
  }

  private void promptForInitialAction()
  {
    String title = Resources.get("Application.InitialAction.Title");
    String prompt = Resources.get("Application.InitialAction.Prompt");
    String newLabel = Resources.get("Application.InitialAction.Label.New");
    String existingLabel = Resources.get("Application.InitialAction.Label.Existing");
    String cancelLabel = Resources.get("Application.InitialAction.Label.Cancel");

    Object[] options = { newLabel, existingLabel, cancelLabel };
    int response = JOptionPane.showOptionDialog(this, prompt, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

    switch (response)
    {
      case JOptionPane.YES_OPTION:
        createNewFile();
        break;
      case JOptionPane.NO_OPTION:
        open();
        break;
    }
  }

  public void createNewFile()
  {
    Editor editor = new Editor(this);
    editor.openNew();
    addToDesktop(editor);
  }

  private void open()
  {
    OpenDialog openDialog = new OpenDialog(this);
    String fileName = openDialog.showDialog();
    if (fileName == null)
    {
      return;
    }
    if (isOpen(fileName))
    {
      return;
    }
    openFile(fileName);
  }

  private boolean isOpen(String fileName)
  {
    Editor editor = find(fileName);
    if (editor == null)
    {
      return false;
    }

    activate(editor);
    syncPlayState();
    return true;
  }

  public void activate(Editor editor)
  {
    int tabCount = tabbedPane.getTabCount();
    for (int i = 0; i < tabCount; i++)
    {
      if (tabbedPane.getComponentAt(i) == editor)
      {
        tabbedPane.setSelectedIndex(i);
        return;
      }
    }
  }

  public void onModify(Editor editor)
  {
    propagateEditorTitleToTitleBar(editor);
  }

  public void selectEditor()
  {
    Editor editor = getSelectedEditor();
    controlPanel.selectEditor(editor);
    propagateEditorTitleToTitleBar(editor);
    syncPlayState();
  }

  public void onNotatorInstrumentChange(int channel, int instrument)
  {
    controlPanel.onNotatorInstrumentChange(channel, instrument);
  }

  public void onMidiChange()
  {
    controlPanel.onMidiChange();

  }

  public void onMidiSelect(Midi selection)
  {
    controlPanel.onMidiSelect(selection);
  }

  public void selectChannel(int channel)
  {
    controlPanel.selectChannel(channel);
  }

  private void propagateEditorTitleToTitleBar(Editor editor)
  {
    String title = null;
    if (editor != null)
    {
      title = editor.getTitle();
      String shortTitle = editor.getShortTitle();
      int index = tabbedPane.indexOfComponent(editor);
      if (!shortTitle.equals(tabbedPane.getTitleAt(index)))
      {
        tabbedPane.setTitleAt(index, shortTitle);
      }
    }
    updateTitle(title);
  }

  private void updateTitle(String title)
  {
    if (title == null)
    {
      setTitle(Resources.get("Application.Title.Default"));
    }
    else
    {
      setTitle(Resources.format("Application.Title.Compound", title));
    }
  }

  public Editor find(String fileName)
  {
    int tabCount = tabbedPane.getTabCount();
    for (int i = 0; i < tabCount; i++)
    {
      Editor editor = (Editor)tabbedPane.getComponentAt(i);
      if (fileName.equals(editor.getFileName()))
      {
        return editor;
      }
    }
    return null;
  }

  private void openFile(String fileName)
  {
    try
    {
      Editor editor = new Editor(this);
      editor.openFile(fileName);
      addToDesktop(editor);
      activate(editor);
      syncPlayState();
    }
    catch (Exception e)
    {
      CommonDialog.showOkay(this, e.toString());
    }
  }

  private void syncPlayState()
  {
    // If the play button is selected
    if (isPlaying())
    {
      stop(); // stop whatever is playing
      start(); // start the new file
    }
    // Otherwise not the play button, but see if a selection is playing
    else if (player.isPlaying())
    {
      stop(); // stop the selection, but don't start anything
    }
  }

  private void addToDesktop(Editor editor)
  {
    tabbedPane.addTab(editor.getShortTitle(), editor);
    tabbedPane.setSelectedComponent(editor);
    if (tabbedPane.getTabCount() == 1)
    {
      tabbedPane.setVisible(true);
    }
    editor.setVisible(true);
  }

  public void removeFromDesktop(Editor editor)
  {
    tabbedPane.remove(editor);
    if (tabbedPane.getTabCount() == 0)
    {
      tabbedPane.setVisible(false);
    }
  }

  public Dimension scaleDimension(Dimension dimension, int widthPercent, int heightPercent)
  {
    dimension.width = dimension.width * widthPercent / 100;
    dimension.height = dimension.height * heightPercent / 100;
    return dimension;
  }

  /**
   * NB: This must be called before adding the ComponentListener!
   * 
   */
  private void restoreWindowParameters()
  {
    Rectangle defaultBounds = new Rectangle();
    Rectangle bounds = Settings.getRectangle(Settings.STUDIO_BOUNDS_KEY, defaultBounds);
    if (bounds.isEmpty())
    {
      Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
      scaleDimension(size, 70, 70);
      setSize(size);
      setLocationByPlatform(true);
    }
    else
    {
      int extendedState = Settings.getInt(Settings.STUDIO_STATE_KEY, NORMAL);
      setExtendedState(extendedState);
      setBounds(bounds);
    }
  }

  private void saveWindowParameters()
  {
    int extendedState = getExtendedState();
    Settings.put(Settings.STUDIO_STATE_KEY, extendedState);

    if (extendedState == NORMAL)
    {
      Rectangle bounds = getBounds();
      Settings.put(Settings.STUDIO_BOUNDS_KEY, bounds);
    }

  }

  private Editor getSelectedEditor()
  {
    Editor editor = (Editor)tabbedPane.getSelectedComponent();
    return editor;
  }

  private void processCommand(ActionEvent e)
  {
    String command = e.getActionCommand();
    Editor editor = getSelectedEditor();
    if (command.equals("About"))
    {
      new Splash();
    }
    else if (command.equals("New"))
    {
      createNewFile();
    }
    else if (command.equals("Open"))
    {
      open();
    }
    else if (command.equals("CloseAll"))
    {
      closeAll();
    }
    else if (command.equals("Topics"))
    {
      help();
    }
    else if (command.equals("Play"))
    {
      play();
    }
    else if (command.equals("Stop"))
    {
      stop();
    }
    else if (command.equals("Repeat"))
    {
      toggleRepeatAction();
    }
    else if (command.equals("RoundToMeasure"))
    {
      // State is toggled by underlying action
    }
    else if (command.equals("FillToMeasure"))
    {
      // State is toggled by underlying action
    }
    else if (command.equals("Selection"))
    {
      // State is toggled by underlying action
    }
    else if (editor != null)
    {
      editor.processCommand(e);
    }
  }

  private void help()
  {
    //    URI uri = Resources.getUri("Application.Help.Uri");
    //    File currentDirectory = new File (".");
    //    System.out.println("currentDirectory="+currentDirectory.getCanonicalPath());
    try
    {
      File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
      String parentPath = jarFile.getParent().replace("\\", "/");
      if (!parentPath.endsWith("/"))
      {
        // NB: Prevent double slashes in case parent is root (i.e. /) because Windows doesn't like them.
        parentPath = parentPath + "/";
      }
      parentPath = parentPath + "WebContent/index.html";
      String uriPath = "file:///" + parentPath;
      URI uri = new URI(uriPath);
      Helper.browse(uri);
    }
    catch (Exception e)
    {
      CommonDialog.showOkay(this, e.toString());
    }
  }

  public void closeMainWindow()
  {
    if (!closeAll())
    {
      return;
    }
    System.exit(0);
  }

  public boolean closeAll()
  {
    for (Editor editor : getEditors())
    {
      activate(editor);
      if (!editor.close())
      {
        return false;
      }
    }
    return true;
  }

  public ArrayList<Editor> getEditors()
  {
    ArrayList<Editor> editors = new ArrayList<Editor>();
    int tabCount = tabbedPane.getTabCount();
    for (int i = 0; i < tabCount; i++)
    {
      Editor editor = (Editor)tabbedPane.getComponentAt(i);
      editors.add(editor);
    }
    return editors;
  }

  public Response invoke(Request request)
  {
    return null;
  }

  public boolean isRepeat()
  {
    if (repeatAction == null)
    {
      repeatAction = Resources.getCachedAction("Repeat", actionListener);
    }
    return (Boolean)repeatAction.getValue(Action.SELECTED_KEY);
  }

  private void toggleRepeatAction()
  {
    boolean isRepeat = isRepeat();
    player.setLoop(isRepeat);
  }

  public boolean isSelection()
  {
    if (selectionAction == null)
    {
      selectionAction = Resources.getCachedAction("Selection", actionListener);
    }
    return (Boolean)selectionAction.getValue(Action.SELECTED_KEY);
  }

  public boolean isRoundToMeasure()
  {
    if (roundToMeasureAction == null)
    {
      roundToMeasureAction = Resources.getCachedAction("RoundToMeasure", actionListener);
    }
    return (Boolean)roundToMeasureAction.getValue(Action.SELECTED_KEY);
  }

  public boolean isFillToMeasure()
  {
    if (fillToMeasureAction == null)
    {
      fillToMeasureAction = Resources.getCachedAction("FillToMeasure", actionListener);
    }
    return (Boolean)fillToMeasureAction.getValue(Action.SELECTED_KEY);
  }

  private boolean isPlay()
  {
    return getPlayButton().isSelected();
  }

  public JToggleButton getPlayButton()
  {
    if (playButton == null)
    {
      playButton = (JToggleButton)Resources.find(toolBar, "Play");
    }
    return playButton;
  }

  public void play()
  {
    if (isPlay())
    {
      start();
    }
    else
    {
      stop();
    }
  }

  private void start()
  {
    Editor editor = getSelectedEditor();
    if (editor != null)
    {
      editor.play();
      Action action = Resources.getCachedAction("Play", actionListener);
      action.putValue(Action.SELECTED_KEY, Boolean.TRUE);
    }
  }

  public void stop()
  {
    player.stop();
    Action action = Resources.getCachedAction("Play", actionListener);
    action.putValue(Action.SELECTED_KEY, Boolean.FALSE);
  }

  public boolean isPlaying()
  {
    Action action = Resources.getCachedAction("Play", actionListener);
    Object value = action.getValue(Action.SELECTED_KEY);
    return (Boolean)value;
  }

  public Player getPlayer()
  {
    return player;
  }

  private class Splash extends JDialog
  {

    private boolean isClosed;
    private Runnable runnable;

    private Splash()
    {
      super(JOptionPane.getFrameForComponent(Studio.this));
      String title = Resources.get("Splash.Title.DialogBox");
      setTitle(title);
      setModal(true);
      SplashPanel splashPanel = new SplashPanel();
      add(splashPanel);
      setSize(new Dimension(500, 300));
      setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      addWindowListener(new SplashWindowListener());
      EventQueue.invokeLater(new Runnable()
      {
        public void run()
        {
          setLocationRelativeTo(Studio.this);
          setVisible(true);
        }
      });
    }

    public void invokeAfterLicenseAcceptance(Runnable runnable)
    {
      if (isClosed)
      {
        runnable.run();
      }
      else
      {
        this.runnable = runnable;
      }
    }

    private void accept()
    {
      Splash.this.dispose();
      isClosed = true;
      if (runnable != null)
      {
        runnable.run();
      }
    }

    private void decline()
    {
      System.exit(2);
    }

    private class SplashWindowListener extends WindowAdapter
    {
      public void windowClosing(WindowEvent e)
      {
        decline();
      }
    }

    private class SplashPanel extends GridBagPanel
    {
      private SplashPanel()
      {
        String fontName = Resources.get("Splash.Font.Name");
        int fontSizeSmall = Resources.getInt("Splash.Font.Size.Small");
        int fontSizeLarge = Resources.getInt("Splash.Font.Size.Large");
        Color foreground = Resources.getColor("Splash.Color.Foreground", getForeground());
        Color background = Resources.getColor("Splash.Color.Background", getBackground());

        String title = Resources.get("Splash.Title");
        String copyright1 = Resources.get("Splash.Copyright.Line1");
        String copyright2 = Resources.get("Splash.Copyright.Line2");
        String license = Resources.get("Splash.License");

        Font smallFont = new Font(fontName, Font.PLAIN, fontSizeSmall);
        Font largeFont = new Font(fontName, Font.PLAIN, fontSizeLarge);

        setBackground(background);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(largeFont);
        titleLabel.setForeground(foreground);
        titleLabel.setBackground(background);
        add(titleLabel, "x=0,y=0,top=5,left=5,bottom=0,right=5,anchor=c,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

        JLabel copyright1Label = new JLabel(copyright1);
        copyright1Label.setFont(smallFont);
        copyright1Label.setForeground(foreground);
        copyright1Label.setBackground(background);
        add(copyright1Label, "x=0,y=1,top=5,left=5,bottom=0,right=5,anchor=c,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

        JLabel copyright2Label = new JLabel(copyright2);
        copyright2Label.setFont(smallFont);
        copyright2Label.setForeground(foreground);
        copyright2Label.setBackground(background);
        add(copyright2Label, "x=0,y=2,top=0,left=5,bottom=0,right=5,anchor=c,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

        JTextArea licenseTextArea = new JTextArea(license);
        licenseTextArea.setFont(smallFont);
        licenseTextArea.setForeground(foreground);
        licenseTextArea.setBackground(background);
        licenseTextArea.setLineWrap(true);
        licenseTextArea.setWrapStyleWord(true);
        licenseTextArea.setEditable(false);

        JScrollPane licenseScrollPane = new JScrollPane(licenseTextArea);
        // licenseScrollPane.setBorder(null);
        licenseScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(licenseScrollPane, "x=0,y=3,top=10,left=5,bottom=0,right=5,anchor=w,fill=b,weightx=1,weighty=1,gridwidth=*,gridheight=1");

        JPanel spacer = new JPanel();
        spacer.setBackground(background);
        add(spacer, "x=0,y=4,top=5,left=5,bottom=0,right=0,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

        JButton acceptButton = new JButton("Accept");
        Studio.Splash.this.getRootPane().setDefaultButton(acceptButton);
        acceptButton.addActionListener(new AcceptButtonListener());
        add(acceptButton, "x=1,y=4,top=5,left=5,bottom=5,right=0,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

        JButton declineButton = new JButton("Decline");
        declineButton.addActionListener(new DeclineButtonListener());
        add(declineButton, "x=2,y=4,top=5,left=5,bottom=5,right=5,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");
      }
    }

    public class AcceptButtonListener implements ActionListener
    {
      public void actionPerformed(ActionEvent e)
      {
        accept();
      }
    }

    public class DeclineButtonListener implements ActionListener
    {
      public void actionPerformed(ActionEvent e)
      {
        decline();
      }
    }

  }

  public class ImageBackgroundPanel extends JPanel
  {
    private Image image;

    private int style;

    public ImageBackgroundPanel(int style)
    {
      this.style = style;
    }

    public void setImage(Image image)
    {
      this.image = image;

      // Tell super-class not to paint background over our image
      setOpaque(false);
    }

    public void paint(Graphics g)
    {
      Dimension d = getSize();
      if (style != 0)
      {
        g.setColor(getBackground());
        g.fillRect(0, 0, d.width, d.height);
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        if (style == 1)
        {
          float aspectRatio = (float)w / (float)h;
          int newWidth = d.width;
          int newHeight = (int)(newWidth / aspectRatio);
          if (newWidth > w || newHeight > h)
          {
            newHeight = d.height;
            newWidth = (int)(newHeight * aspectRatio);
          }
          g.drawImage(image, (d.width - newWidth) / 2, (d.height - newHeight) / 2, newWidth, newHeight, null);
        }
        else if (style == 2)
        {
          g.drawImage(image, (d.width - w) / 2, (d.height - h) / 2, w, h, null);
        }
        else if (style == 3)
        {
          g.drawImage(image, d.width - w, d.height - h, w, h, null);
        }
      }
      super.paint(g);
      g.drawLine(0, 0, d.width, 0);
      g.drawLine(0, d.height - 1, d.width, d.height - 1);
    }

  }

  private class StudioActionListener implements ActionListener
  {

    public void actionPerformed(ActionEvent e)
    {
      processCommand(e);
    }

  }

  private class StudioComponentListener extends ComponentAdapter
  {
    public void componentMoved(ComponentEvent e)
    {
      saveWindowParameters();
    }

    public void componentResized(ComponentEvent e)
    {
      saveWindowParameters();
    }
  }

  private class StudioWindowListener extends WindowAdapter
  {
    public void windowClosing(WindowEvent e)
    {
      closeMainWindow();
    }

  }

  private class DesktopChangeListener implements ChangeListener
  {
    public void stateChanged(ChangeEvent e)
    {
      selectEditor();
    }

  }

  public class StatusBar extends JPanel
  {
    private JLabel label = new JLabel();

    public StatusBar(String text)
    {
      super(new BorderLayout());
      add(label, BorderLayout.CENTER);
      setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      label.setText(text);
      setPreferredSize(getPreferredSize());
    }

    public void setText(String text)
    {
      label.setText(text);
    }
  }

}
