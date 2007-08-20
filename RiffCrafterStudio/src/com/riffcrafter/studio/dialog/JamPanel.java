// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.library.dialog.WidthConstrainedList;
import com.riffcrafter.library.dialog.WidthConstrainedTextField;
import com.riffcrafter.studio.app.Editor;
import com.riffcrafter.studio.dialog.JamPanel.MainJamPanel.JamSessionPanel;

// x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=e,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1

public class JamPanel extends GridBagPanel
{
  private static final int BLOCK_SIZE = 500;
  private static final String RSJM = "RSJM-"; // RiffCrafter Studio Jam Message
  private static final Color FLASH_BLACKGROUND = Color.ORANGE;

  private Editor editor;
  private String userId;
  private Messenger messenger;
  private MainJamPanel mainJamPanel;
  private FlashingTabbedPane jamSessionTabbedPane;

  private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

  public JamPanel()
  {
    displayLoginPanel();
  }

  public void selectEditor(Editor editor)
  {
    this.editor = editor;
  }

  private void displayLoginPanel()
  {
    LoginPanel loginPanel = new LoginPanel();
    removeAll();
    add(loginPanel, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=w,fill=b,weightx=1,weighty=1,gridwidth=1,gridheight=1");
    validate();
  }

  public void displayMainJamPanel()
  {
    mainJamPanel = new MainJamPanel();
    removeAll();
    add(mainJamPanel, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=w,fill=b,weightx=1,weighty=1,gridwidth=1,gridheight=1");
    validate();
  }

  private void setUserId(String userId)
  {
    this.userId = userId;
  }

  private boolean isLoggedIn()
  {
    return userId != null;
  }

  private String login(String messengerName, String userId, String password)
  {
    JamMessengerListener messengerListener = new JamMessengerListener();

    if (messengerName.equals(TestMessenger.DISPLAY_NAME))
    {
      messenger = new TestMessenger(messengerListener);
    }
    else
    {
      throw new RuntimeException("Unsupported messenger: " + messengerName);
    }

    String errorMessage = messenger.login(userId, password);
    if (errorMessage == null)
    {
      setUserId(userId);
      displayMainJamPanel();
    }
    return errorMessage;
  }

  private void logout()
  {
    setUserId(null);
    messenger.logout();
    displayLoginPanel();
  }

  public void displayMessage(Date timestamp, String sessionName, ArrayList<String> participants, String fromUser, String message)
  {
    JamSessionPanel jamSessionPanel = findJamSessionPanel(sessionName);
    if (jamSessionPanel == null)
    {
      jamSessionPanel = mainJamPanel.addJamSession(sessionName, participants);
    }

    jamSessionTabbedPane.flash(jamSessionPanel);

    if (message.startsWith(RSJM))
    {
      Midi midi = receiveBlocks(jamSessionPanel.getMessageBuffer(), message);
      if (midi != null)
      {
        MessageListElement messageListElement = new MusicMessageListElement(timestamp, fromUser, midi);
        jamSessionPanel.addRecentMessage(messageListElement);
      }
    }
    else
    {
      MessageListElement messageListElement = new MessageListElement(timestamp, fromUser, message);
      jamSessionPanel.addRecentMessage(messageListElement);
    }
  }

  private String sendBlocks(String sessionName, String buffer)
  {
    int offset = 0;
    int bytesRemaining = buffer.length();
    int blockCount = (bytesRemaining + (BLOCK_SIZE - 1)) / BLOCK_SIZE;
    for (int i = 1; i <= blockCount; i++)
    {
      if (i > 1)
      {
        try
        {
          Thread.sleep(100);
        }
        catch (InterruptedException e)
        {
          throw new RuntimeException(e);
        }
      }
      int blockSize = Math.min(BLOCK_SIZE, bytesRemaining);
      String block = buffer.substring(offset, offset + blockSize);
      String midiMessage = RSJM + i + "," + blockCount + "=" + block;
      String errorMessage = messenger.sendMessage(sessionName, midiMessage);
      if (errorMessage != null)
      {
        return errorMessage;
      }
      offset += blockSize;
      bytesRemaining -= blockSize;
    }
    return null;
  }

  private Midi receiveBlocks(MessageBuffer messageBuffer, String message)
  {
    Midi midi = null;
    try
    {
      int comma = message.indexOf(',', RSJM.length());
      int equals = message.indexOf('=', comma);
      int blockNumber = Integer.parseInt(message.substring(RSJM.length(), comma));
      int blockCount = Integer.parseInt(message.substring(comma + 1, equals));
      String block = message.substring(equals + 1);
      messageBuffer.addBlock(blockNumber, blockCount, block);
      if (messageBuffer.getBlockCount() == blockCount)
      {
        String base64 = messageBuffer.consume();
        midi = Midi.fromBase64(base64);
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return midi;
  }

  public void updateParticipants(String sessionName, ArrayList<String> participants)
  {
    JamSessionPanel jamSessionPanel = findJamSessionPanel(sessionName);
    if (jamSessionPanel == null)
    {
      jamSessionPanel = mainJamPanel.addJamSession(sessionName, participants);
    }
    jamSessionPanel.updateParticipants(participants);
  }

  public void updateStatus()
  {
    mainJamPanel.updateStatus();
  }

  private JamSessionPanel findJamSessionPanel(String sessionName)
  {
    int size = jamSessionTabbedPane.getTabCount();
    for (int i = 0; i < size; i++)
    {
      String tabTitle = jamSessionTabbedPane.getTitleAt(i);
      if (tabTitle.equals(sessionName))
      {
        JamSessionPanel jamSessionPanel = (JamSessionPanel)jamSessionTabbedPane.getComponentAt(i);
        return jamSessionPanel;
      }
    }
    return null;
  }

  public void close()
  {
    // We get notified on both normal logout and disconnect
    if (!isLoggedIn())
    {
      CommonDialog.showOkay(this, "Your instant messaging connection has closed.");
      logout();
    }
  }

  private class MessageBuffer
  {
    private char[] buffer;
    private int blocksReceived;
    private int bufferSize;
    private long lastTime;

    public void addBlock(int blockNumber, int blockCount, String block)
    {
      long thisTime = System.currentTimeMillis();

      if (buffer == null || (lastTime != 0 && thisTime - lastTime > 10000))
      {
        buffer = new char[blockCount * BLOCK_SIZE];
      }

      lastTime = thisTime;

      int length = block.length();
      int blockOffset = blockNumber - 1;
      int offset = blockOffset * BLOCK_SIZE;
      for (int i = 0; i < length; i++)
      {
        buffer[offset + i] = block.charAt(i);
      }
      bufferSize = Math.max(bufferSize, offset + length);
      blocksReceived++;
    }

    public String consume()
    {
      String bufferString = new String(buffer, 0, bufferSize);
      buffer = null;
      blocksReceived = 0;
      bufferSize = 0;
      return bufferString;
    }

    public int getBlockCount()
    {
      return blocksReceived;
    }
  }

  private class JamMessengerListener implements MessengerListener
  {
    public void displayMessage(final Date timestamp, final String sessionName, final ArrayList<String> participants, final String fromUser, final String message)
    {
      EventQueue.invokeLater(new Runnable()
      {
        public void run()
        {
          JamPanel.this.displayMessage(timestamp, sessionName, participants, fromUser, message);
        }
      });
    }

    public void updateParticipants(final String sessionName, final ArrayList<String> participants)
    {
      EventQueue.invokeLater(new Runnable()
      {
        public void run()
        {
          JamPanel.this.updateParticipants(sessionName, participants);
        }
      });
    }

    public void updateStatus()
    {
      EventQueue.invokeLater(new Runnable()
      {
        public void run()
        {
          JamPanel.this.updateStatus();
        }
      });
    }

    public void close()
    {
      EventQueue.invokeLater(new Runnable()
      {
        public void run()
        {
          JamPanel.this.close();
        }
      });
    }

  }

  private class LoginPanel extends GridBagPanel
  {
    private JComboBox messengerComboBox;
    private JTextField userIdTextField;
    private JPasswordField passwordTextField;

    private LoginPanel()
    {
      JLabel messengerLabel = new JLabel("Instant Messaging Provider:");
      add(messengerLabel, "x=0,y=0,top=5,left=5,bottom=0,right=5,anchor=nw,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

      Object[] items = new Object[] { TestMessenger.DISPLAY_NAME };
      // Object[] items = new Object[] { YahooMessenger.DISPLAY_NAME };
      messengerComboBox = new JComboBox(items);
      add(messengerComboBox, "x=0,y=1,top=0,left=5,bottom=0,right=5,anchor=nw,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel userIdLabel = new JLabel("User ID:");
      add(userIdLabel, "x=0,y=2,top=5,left=5,bottom=0,right=5,anchor=nw,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

      userIdTextField = new JTextField();
      add(userIdTextField, "x=0,y=3,top=0,left=5,bottom=0,right=5,anchor=nw,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JLabel passwordLabel = new JLabel("Password:");
      add(passwordLabel, "x=0,y=4,top=5,left=5,bottom=0,right=5,anchor=nw,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

      LoginActionListener loginActionListener = new LoginActionListener();

      passwordTextField = new JPasswordField();
      passwordTextField.addActionListener(loginActionListener);
      add(passwordTextField, "x=0,y=5,top=0,left=5,bottom=0,right=5,anchor=nw,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");

      JButton loginButton = new JButton("Login");
      loginButton.addActionListener(loginActionListener);
      add(loginButton, "x=0,y=6,top=15,left=0,bottom=0,right=5,anchor=ne,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

      JPanel spacerPanel = new JPanel();
      add(spacerPanel, "x=0,y=7,top=0,left=0,bottom=0,right=0,anchor=e,fill=b,weightx=1,weighty=1,gridwidth=1,gridheight=1");
    }

    public class LoginActionListener implements ActionListener
    {
      public void actionPerformed(ActionEvent e)
      {
        String messengerName = (String)messengerComboBox.getSelectedItem();
        String userId = userIdTextField.getText();
        String password = new String(passwordTextField.getPassword());
        String errorMessage = login(messengerName, userId, password);
        if (errorMessage != null)
        {
          CommonDialog.showOkay(JamPanel.this, errorMessage);
        }
      }

    }

  }

  public class MainJamPanel extends GridBagPanel
  {
    private JTree contactsTree;
    private JCheckBox playAsReceivedCheckBox;
    private JCheckBox playOnMouseClickCheckBox;
    private JScrollPane contactsTreeScrollPane;

    private MainJamPanel()
    {
      JLabel friendsLabel = new JLabel("Contacts:");
      add(friendsLabel, "x=0,y=1,top=5,left=5,bottom=0,right=5,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=*,gridheight=1");

      DefaultMutableTreeNode rootNode = messenger.getContacts();
      DefaultTreeModel contactsTreeModel = new DefaultTreeModel(rootNode);

      contactsTree = new JTree(contactsTreeModel);
      contactsTree.setRootVisible(false);
      contactsTree.setShowsRootHandles(true);
      contactsTree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      contactsTree.addMouseListener(new ContactsMouseListener());

      expandTree(contactsTree);

      contactsTreeScrollPane = new JScrollPane(contactsTree);
      contactsTreeScrollPane.getViewport().setBackground(contactsTree.getBackground());
      add(contactsTreeScrollPane, "x=0,y=2,top=0,left=5,bottom=0,right=5,anchor=w,fill=b,weightx=1,weighty=1,gridwidth=*,gridheight=1");

      JButton startButton = new JButton("Start Session");
      startButton.addActionListener(new StartButtonListener());
      add(startButton, "x=0,y=3,top=5,left=5,bottom=0,right=0,anchor=e,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

      jamSessionTabbedPane = new FlashingTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
      jamSessionTabbedPane.setVisible(false);
      add(jamSessionTabbedPane, "x=0,y=4,top=5,left=5,bottom=0,right=5,anchor=w,fill=b,weightx=1,weighty=1,gridwidth=*,gridheight=1");

      playAsReceivedCheckBox = new JCheckBox("Play music messages as they are received");
      playAsReceivedCheckBox.setSelected(true);
      add(playAsReceivedCheckBox, "x=0,y=5,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

      playOnMouseClickCheckBox = new JCheckBox("Play music messages when clicked by mouse");
      playOnMouseClickCheckBox.setSelected(false);
      add(playOnMouseClickCheckBox, "x=0,y=6,top=5,left=5,bottom=0,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

      String statusText = "You are logged in as " + userId;
      JLabel statusLabel = new JLabel(statusText);
      add(statusLabel, "x=0,y=10,top=15,left=5,bottom=5,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

      JButton logoutButton = new JButton("Logout");
      logoutButton.addActionListener(new LogoutActionListener());
      add(logoutButton, "x=1,y=10,top=15,left=5,bottom=5,right=5,anchor=e,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");
    }

    private void expandTree(JTree tree)
    {
      for (int i = 0; i < tree.getRowCount(); i++)
      {
        tree.expandRow(i);
      }
    }

    public void updateStatus()
    {
      // contactsTree.treeDidChange(); // methodDidNotWork!
      DefaultTreeModel defaultTreeModel = (DefaultTreeModel)contactsTree.getModel();
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)defaultTreeModel.getRoot();
      updateAll(defaultTreeModel, root);
    }

    private void updateAll(DefaultTreeModel defaultTreeModel, DefaultMutableTreeNode parent)
    {
      defaultTreeModel.nodeChanged(parent);
      int childCount = defaultTreeModel.getChildCount(parent);
      for (int i = 0; i < childCount; i++)
      {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode)defaultTreeModel.getChild(parent, i);
        updateAll(defaultTreeModel, child);
      }
    }

    private void addJamSession()
    {
      TreePath[] paths = contactsTree.getSelectionPaths();
      if (paths == null)
      {
        return;
      }

      ArrayList<String> participants = new ArrayList<String>();

      for (TreePath path : paths)
      {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
        Object userObject = node.getUserObject();
        if (!(userObject instanceof JamUser))
        {
          // Just expand / collapse
          return;
        }
        JamUser user = (JamUser)userObject;
        String userId = user.getId();
        participants.add(userId);
      }

      String sessionName;

      if (participants.size() == 1)
      {
        sessionName = participants.get(0);
      }
      else
      {
        sessionName = CommonDialog.getString(this, "Enter name for this jam session:");
        if (sessionName == null)
        {
          return;
        }
      }

      addJamSession(sessionName, participants);
    }

    private JamSessionPanel addJamSession(String sessionName, ArrayList<String> participants)
    {
      JamSessionPanel jamSessionPanel = findJamSessionPanel(sessionName);
      if (jamSessionPanel == null)
      {
        jamSessionPanel = new JamSessionPanel(sessionName, participants);
        jamSessionTabbedPane.addTab(sessionName, jamSessionPanel);
        if (!jamSessionTabbedPane.isVisible())
        {
          jamSessionTabbedPane.setVisible(true);
        }
      }
      jamSessionTabbedPane.setSelectedComponent(jamSessionPanel);
      return jamSessionPanel;
    }

    public class StartButtonListener implements ActionListener
    {
      public void actionPerformed(ActionEvent e)
      {
        int tabCount = jamSessionTabbedPane.getTabCount();

        addJamSession();

        if (tabCount == jamSessionTabbedPane.getTabCount())
        {
          CommonDialog.showOkay(JamPanel.this.mainJamPanel, "Please select one or more users from the contacts list and try again.");
        }
      }
    }

    public class ContactsMouseListener extends MouseAdapter
    {
      public void mouseClicked(MouseEvent e)
      {
        if (e.getClickCount() == 2)
        {
          addJamSession();
        }
      }
    }

    public class LogoutActionListener implements ActionListener
    {
      public void actionPerformed(ActionEvent e)
      {
        logout();
      }
    }

    public class JamSessionPanel extends GridBagPanel
    {
      private String sessionName;
      private JTextField messageTextField;
      private DefaultListModel recentMessageListModel;
      private JList recentMessageList;
      private DefaultListModel participantsListModel;
      public MessageBuffer messageBuffer;

      public JamSessionPanel(String sessionName, ArrayList<String> participants)
      {
        this.sessionName = sessionName;

        messenger.open(sessionName, participants);

        if (participants.size() > 1)
        {
          JLabel participantsLabel = new JLabel("Participants:");
          add(participantsLabel, "x=0,y=0,top=5,left=5,bottom=0,right=5,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=*,gridheight=1");

          participantsListModel = new DefaultListModel();
          updateParticipants(participants);
          JList participantsList = new JList(participantsListModel);
          JScrollPane participantsListScrollPane = new JScrollPane(participantsList);
          participantsListScrollPane.getViewport().setBackground(participantsList.getBackground());
          add(participantsListScrollPane, "x=0,y=1,top=0,left=5,bottom=0,right=5,anchor=w,fill=b,weightx=1,weighty=1,gridwidth=*,gridheight=1");
        }

        JLabel recentMessagesLabel = new JLabel("Recent Messages:");
        add(recentMessagesLabel, "x=0,y=2,top=5,left=5,bottom=0,right=5,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=*,gridheight=1");

        recentMessageListModel = new DefaultListModel();
        recentMessageList = new WidthConstrainedList(recentMessageListModel);
        recentMessageList.addMouseListener(new MessageListMouseListener());
        recentMessageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(recentMessageList);
        scrollPane.getViewport().setBackground(recentMessageList.getBackground());
        add(scrollPane, "x=0,y=3,top=0,left=5,bottom=0,right=5,anchor=w,fill=b,weightx=1,weighty=1,gridwidth=*,gridheight=1");

        JLabel yourMessageLabel = new JLabel("Your Message:");
        add(yourMessageLabel, "x=0,y=4,top=5,left=5,bottom=0,right=5,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=*,gridheight=1");

        SendActionListener sendActionListener = new SendActionListener();

        messageTextField = new WidthConstrainedTextField();
        messageTextField.addActionListener(sendActionListener);
        add(messageTextField, "x=0,y=5,top=0,left=5,bottom=5,right=5,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=*,gridheight=1");

        JButton sendButton = new JButton("Send Text");
        sendButton.addActionListener(sendActionListener);
        add(sendButton, "x=0,y=6,top=0,left=5,bottom=5,right=0,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

        JButton sendMusicButton = new JButton("Send Music");
        sendMusicButton.addActionListener(new SendMusicActionListener());
        add(sendMusicButton, "x=1,y=6,top=0,left=5,bottom=5,right=0,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

        JButton receiveMusicButton = new JButton("Receive Music");
        receiveMusicButton.addActionListener(new ReceiveMusicActionListener());
        add(receiveMusicButton, "x=2,y=6,top=0,left=5,bottom=5,right=0,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

        JButton clearHistoryButton = new JButton("Clear History");
        clearHistoryButton.addActionListener(new ClearHistoryButtonListener());
        add(clearHistoryButton, "x=3,y=6,top=0,left=5,bottom=5,right=5,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new CloseButtonListener());
        add(closeButton, "x=4,y=6,top=0,left=5,bottom=5,right=5,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");
      }

      public MessageBuffer getMessageBuffer()
      {
        if (messageBuffer == null)
        {
          messageBuffer = new MessageBuffer();
        }
        return messageBuffer;
      }

      public void updateParticipants(ArrayList<String> participants)
      {
        participantsListModel.removeAllElements();
        for (String participant : participants)
        {
          participantsListModel.addElement(participant);
        }
      }

      public void clearHistory()
      {
        recentMessageListModel.clear();
      }

      public void closeTab()
      {
        jamSessionTabbedPane.remove(this);
        if (jamSessionTabbedPane.getTabCount() == 0)
        {
          jamSessionTabbedPane.setVisible(false);
        }
      }

      public void addRecentMessage(MessageListElement messageListElement)
      {
        int size = recentMessageListModel.getSize();
        recentMessageListModel.add(size, messageListElement);
        recentMessageList.ensureIndexIsVisible(size);
        if (playAsReceivedCheckBox.isSelected())
        {
          if (messageListElement instanceof MusicMessageListElement)
          {
            MusicMessageListElement musicMessageListElement = (MusicMessageListElement)messageListElement;
            Midi midi = musicMessageListElement.getMidi();
            editor.play(midi);
          }
        }
      }

      private void sendMessage()
      {
        String message = messageTextField.getText();
        String errorMessage = messenger.sendMessage(sessionName, message);
        if (errorMessage == null)
        {
          MessageListElement messageListElement = new MessageListElement(new Date(), userId, message);
          addRecentMessage(messageListElement);
          messageTextField.setText("");
        }
      }

      private void sendMusic()
      {
        Midi midi = null;

        if (editor != null)
        {
          midi = editor.getSelection(true, true);
        }

        if (midi == null)
        {
          CommonDialog.showOkay(this, "Please select some music and try again.");
          return;
        }

        String base64 = midi.toBase64();
        String errorMessage = sendBlocks(sessionName, base64);
        if (errorMessage == null)
        {
          MusicMessageListElement musicMessageListElement = new MusicMessageListElement(new Date(), userId, midi);
          addRecentMessage(musicMessageListElement);
          messageTextField.setText("");
        }
      }

      private void receiveMusic()
      {
        try
        {
          MusicMessageListElement musicMessageListElement = (MusicMessageListElement)recentMessageList.getSelectedValue();
          Midi midi = musicMessageListElement.getMidi();
          editor.pasteRelative(midi);
        }
        catch (Exception e)
        {
          CommonDialog.showOkay(this, "Please select a recent music message and try again.");
          return;
        }
      }

      public class SendActionListener implements ActionListener
      {
        public void actionPerformed(ActionEvent e)
        {
          sendMessage();
        }
      }

      public class SendMusicActionListener implements ActionListener
      {
        public void actionPerformed(ActionEvent e)
        {
          sendMusic();
        }

      }

      public class ReceiveMusicActionListener implements ActionListener
      {
        public void actionPerformed(ActionEvent e)
        {
          receiveMusic();
        }

      }

      public class ClearHistoryButtonListener implements ActionListener
      {
        public void actionPerformed(ActionEvent e)
        {
          clearHistory();
        }
      }

      public class CloseButtonListener implements ActionListener
      {
        public void actionPerformed(ActionEvent e)
        {
          closeTab();
        }
      }

      public class MessageListMouseListener extends MouseAdapter
      {
        @Override
        public void mouseClicked(MouseEvent e)
        {
          try
          {
            if (playOnMouseClickCheckBox.isSelected())
            {
              MusicMessageListElement listElement = (MusicMessageListElement)recentMessageList.getSelectedValue();
              Midi midi = listElement.getMidi();
              editor.play(midi);
            }
          }
          catch (Exception exception)
          {

          }
        }
      }

    }

  }

  private class MessageListElement
  {
    protected Date timestamp;
    protected String user;
    protected String text;

    private MessageListElement()
    {

    }

    public MessageListElement(Date timestamp, String user, String text)
    {
      this.timestamp = timestamp;
      this.user = user;
      this.text = text;
    }

    public String toString()
    {
      if (timestamp == null)
      {
        timestamp = new Date();
      }
      String time = dateFormat.format(timestamp);
      return time + " " + user + ":  " + text;
    }

  }

  private class MusicMessageListElement extends MessageListElement
  {
    private Midi midi;

    public MusicMessageListElement(Date timestamp, String user, Midi midi)
    {
      this.timestamp = timestamp;
      this.user = user;
      this.midi = midi;
      this.text = "Music message containing " + midi.size() + " elements";
    }

    private Midi getMidi()
    {
      return midi;
    }

  }

  private class JamGroup
  {
    private String name;

    private JamGroup(String name)
    {
      this.name = name;
    }

    public String toString()
    {
      return name;
    }
  }

  private interface JamUser
  {
    public String getId();

    public boolean isAvailable();

    public String toString();
  }

  private interface Messenger
  {

    String login(String userId, String password);

    void open(String sessionName, ArrayList<String> participants);

    String sendMessage(String sessionName, String message);

    void close(String sessionName);

    void logout();

    public DefaultMutableTreeNode getContacts();
  }

  public interface MessengerListener
  {

    void displayMessage(Date timestamp, String sessionName, ArrayList<String> participants, String fromUser, String message);

    void updateParticipants(String sessionName, ArrayList<String> participants);

    void updateStatus();

    void close();

  }

  private class TestMessenger implements Messenger
  {
    public static final String DISPLAY_NAME = "Test Instant Messenger";

    private JamMessengerListener messengerListener;
    private HashMap<String, ArrayList<String>> conferences = new HashMap<String, ArrayList<String>>();

    public TestMessenger(JamMessengerListener messengerListener)
    {
      this.messengerListener = messengerListener;
    }

    public DefaultMutableTreeNode getContacts()
    {
      DefaultMutableTreeNode root = new DefaultMutableTreeNode();
      DefaultMutableTreeNode g1 = new DefaultMutableTreeNode(new JamGroup("g.1"));
      DefaultMutableTreeNode u11 = new DefaultMutableTreeNode(new TestJamUser("u.1.1"));
      DefaultMutableTreeNode u12 = new DefaultMutableTreeNode(new TestJamUser("u.1.2"));
      DefaultMutableTreeNode g2 = new DefaultMutableTreeNode(new JamGroup("g.1"));
      DefaultMutableTreeNode u21 = new DefaultMutableTreeNode(new TestJamUser("u.2.1"));
      DefaultMutableTreeNode u22 = new DefaultMutableTreeNode(new TestJamUser("u.2.2"));

      root.add(g1);
      g1.add(u11);
      g1.add(u12);
      root.add(g2);
      g2.add(u21);
      g2.add(u22);

      return root;
    }

    public String login(String userId, String password)
    {
      return null;
    }

    public void logout()
    {
    }

    public void open(String sessionName, ArrayList<String> participants)
    {
      conferences.put(sessionName, participants);
    }

    public void close(String sessionName)
    {
      conferences.remove(sessionName);
    }

    public String sendMessage(final String sessionName, final String message)
    {
      final ArrayList<String> participants = conferences.get(sessionName);
      Thread responder = new Thread(new Runnable()
      {
        public void run()
        {
          try
          {
            Thread.sleep(5000);
            messengerListener.displayMessage(new Date(), sessionName, participants, participants.get(0), message);
          }
          catch (Exception e)
          {

          }
        }
      });
      responder.start();
      return null;
    }

    private class TestJamUser implements JamUser
    {
      private String name;

      private TestJamUser(String name)
      {
        this.name = name;
      }

      public String getId()
      {
        return name;
      }

      public boolean isAvailable()
      {
        return true;
      }

      public String toString()
      {
        return name + " (available)";
      }
    }

  }

  private class FlashingTabbedPane extends JTabbedPane
  {

    private ArrayList<Integer> flashingTabs = new ArrayList<Integer>();
    private Timer timer = new Timer(500, new FlashingTabListener());
    private boolean isOpaque;

    public FlashingTabbedPane(int tabPlacement, int tabLayoutPolicy)
    {
      super(tabPlacement, tabLayoutPolicy);
      addChangeListener(new FlashingTabbedPaneListener());
    }

    @Override
    public void addTab(String title, Component component)
    {
      super.addTab(title, component); // NB: title is used to find tab
      int tabIndex = indexOfComponent(component);
      JLabel tabLabel = new JLabel(title);
      setOpaque(tabLabel, false);
      tabLabel.setBackground(FLASH_BLACKGROUND);
      setTabComponentAt(tabIndex, tabLabel);
    }

    public void flash(Component component)
    {
      int index = indexOfComponent(component);
      if (index != -1)
      {
        int selectedIndex = getSelectedIndex();
        if (index != selectedIndex)
        {
          flashingTabs.add(index);
          if (!timer.isRunning())
          {
            timer.start();
          }
        }
      }
    }

    public void onSelect()
    {
      int selectedIndex = getSelectedIndex();
      Integer tabKey = (Integer)selectedIndex;
      if (flashingTabs.contains(tabKey))
      {
        JLabel tabLabel = (JLabel)getTabComponentAt(selectedIndex);
        setOpaque(tabLabel, false);
        flashingTabs.remove(tabKey);
      }
    }

    private void onTimer()
    {
      if (flashingTabs.size() == 0)
      {
        timer.stop();
      }
      else
      {
        for (int tabIndex : flashingTabs)
        {
          JLabel tabLabel = (JLabel)getTabComponentAt(tabIndex);
          setOpaque(tabLabel, isOpaque);
        }
        isOpaque = !isOpaque;
      }
    }

    private void setOpaque(JLabel tabLabel, boolean isOpaque)
    {
      tabLabel.setOpaque(isOpaque);
      tabLabel.repaint();
    }

    public class FlashingTabbedPaneListener implements ChangeListener
    {
      public void stateChanged(ChangeEvent e)
      {
        onSelect();
      }
    }

    public class FlashingTabListener implements ActionListener
    {

      public void actionPerformed(ActionEvent e)
      {
        onTimer();
      }

    }

  }

}
