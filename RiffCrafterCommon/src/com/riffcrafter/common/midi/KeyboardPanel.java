// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.common.midi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Timer;

import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.library.util.Broker;
import com.riffcrafter.library.util.Broker.Listener;

public class KeyboardPanel extends GridBagPanel
{
  private static final int PREFERRED_WHITE_KEY_WIDTH = 11;
  private static final int PREFERRED_WHITE_KEY_HEIGHT = 50;
  private static final int WHITE_KEYS_PER_OCTAVE = 7;
  private static final int TOTAL_KEYS_PER_OCTAVE = 12;
  private static final int BASE_KEY = TOTAL_KEYS_PER_OCTAVE * 2;
  private static final int TOTAL_KEYBOARD_OCTAVES = 6;
  private static final int TOTAL_WHITE_KEY_COUNT = TOTAL_KEYBOARD_OCTAVES * WHITE_KEYS_PER_OCTAVE;
  private static final int MARGIN = 1;

  public static final int PREFERRED_WIDTH = TOTAL_WHITE_KEY_COUNT * PREFERRED_WHITE_KEY_WIDTH + MARGIN;
  public static final int PREFERRED_HEIGHT = PREFERRED_WHITE_KEY_HEIGHT + MARGIN;

  private static final Color WHITE_KEY_COLOR = Color.WHITE;
  private static final Color BLACK_KEY_COLOR = Color.BLACK;
  private static final Color OUTLINE_COLOR = Color.BLACK;
  private static final Color PRESSED_COLOR = new Color(128, 128, 255); //Color.BLUE;

  private ArrayList<WhiteKey> whiteKeys = new ArrayList<WhiteKey>();
  private ArrayList<BlackKey> blackKeys = new ArrayList<BlackKey>();

  private PressedKeys pressedKeys = new PressedKeys();
  private ReleasedKeys releasedKeys = new ReleasedKeys();

  private Broker broker = new Broker();

  private MidiKeyboardMouseListener midiKeyboardMouseListener = new MidiKeyboardMouseListener();
  private MidiKeyboardComponentListener midiKeyboardComponentListener = new MidiKeyboardComponentListener();

  private int whiteKeyWidth;
  private int whiteKeyHeight;
  private int blackKeyWidth;
  private int blackKeyHeight;
  private int leftMargin;

  private long lastTime = -1;

  public KeyboardPanel()
  {
    Dimension preferredSize = new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);
    setMinimumSize(preferredSize);
    setPreferredSize(preferredSize);
    addMouseListener(midiKeyboardMouseListener);
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(new MidiKeyboardKeyListener());
    addComponentListener(midiKeyboardComponentListener);
    setFocusable(true);
    setToolTipText("");
  }

  private void updateMetrics()
  {
    Dimension size = getSize();
    whiteKeyWidth = size.width / TOTAL_WHITE_KEY_COUNT;
    whiteKeyHeight = size.height - 1;
    blackKeyWidth = whiteKeyWidth / 2;
    blackKeyHeight = whiteKeyHeight / 2;
    leftMargin = (size.width - (whiteKeyWidth * TOTAL_WHITE_KEY_COUNT)) / 2;
    repaint();
  }

  private void createKeys()
  {
    whiteKeys.clear();
    blackKeys.clear();

    KeyContext keyContext = new KeyContext();

    for (int i = 0; i < TOTAL_KEYBOARD_OCTAVES; i++)
    {
      whiteKeys.add(new WhiteKey(keyContext));
      blackKeys.add(new BlackKey(keyContext));
      whiteKeys.add(new WhiteKey(keyContext));
      blackKeys.add(new BlackKey(keyContext));
      whiteKeys.add(new WhiteKey(keyContext));
      whiteKeys.add(new WhiteKey(keyContext));
      blackKeys.add(new BlackKey(keyContext));
      whiteKeys.add(new WhiteKey(keyContext));
      blackKeys.add(new BlackKey(keyContext));
      whiteKeys.add(new WhiteKey(keyContext));
      blackKeys.add(new BlackKey(keyContext));
      whiteKeys.add(new WhiteKey(keyContext));
    }
  }

  public void subscribe(Listener listener)
  {
    broker.subscribe(listener);
  }

  private class MidiKeyboardMouseListener extends MouseAdapter
  {
    public void mousePressed(MouseEvent e)
    {
      addMouseMotionListener(midiKeyboardMouseListener);
      Key key = getKey(e.getPoint());
      if (key != null)
      {
        pressedKeys.press(key, e.isShiftDown());
      }
    }

    public void mouseReleased(MouseEvent e)
    {
      removeMouseMotionListener(midiKeyboardMouseListener);
      pressedKeys.release(e.isShiftDown());
    }

    public void mouseExited(MouseEvent e)
    {
      pressedKeys.release(e.isShiftDown());
    }

    public void mouseDragged(MouseEvent e)
    {
      pressedKeys.drag(e);
    }
  }

  public Key getKey(Point point)
  {
    // Search black keys first so they appear above white keys
    for (Key key : blackKeys)
    {
      if (key.contains(point))
      {
        return key;
      }
    }
    for (Key key : whiteKeys)
    {
      if (key.contains(point))
      {
        return key;
      }
    }
    return null;
  }

  public void paint(Graphics g)
  {
    Graphics2D g2d = (Graphics2D)g;
    Dimension size = getSize();

    g2d.setBackground(getBackground());
    g2d.clearRect(0, 0, size.width, size.height);

    g2d.setColor(WHITE_KEY_COLOR);
    g2d.fillRect(leftMargin, 0, TOTAL_WHITE_KEY_COUNT * whiteKeyWidth, whiteKeyHeight);

    // Paint white keys first so they appear below black keys
    for (Key key : whiteKeys)
    {
      key.paint(g2d);
    }
    for (Key key : blackKeys)
    {
      key.paint(g2d);
    }
  }

  @Override
  public String getToolTipText(MouseEvent e)
  {
    Key key = getKey(e.getPoint());
    String toolTipText;
    if (key == null)
    {
      toolTipText = null;
    }
    else
    {
      toolTipText = NoteName.getNoteName(key.key) + " (" + key.key + ")";
    }
    return toolTipText;
  }

  public abstract class Key extends Rectangle
  {
    int key;
    boolean isOn;
    private long startTime;

    public Key(int x, int y, int width, int height, int key)
    {
      super(x, y, width, height);
      this.key = key;
    }

    public abstract void paint(Graphics2D g2d);

    public boolean isOn()
    {
      return isOn;
    }

    public void on(boolean isChord)
    {
      isOn = true;
      if (isChord)
      {
        startTime = lastTime;
      }
      else
      {
        startTime = System.currentTimeMillis();
        lastTime = startTime;
      }
      broker.publish(new NoteOnEvent(key, isChord), this);
      repaint();
    }

    public void off(boolean isChord, boolean isFinalKey)
    {
      isOn = false;
      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;
      broker.publish(new NoteOffEvent(key, duration, isChord, isFinalKey), this);
      repaint();
    }

  }

  private class WhiteKey extends Key
  {
    private WhiteKey(KeyContext keyContext)
    {
      super(keyContext.getNextWhiteKeyX(), 0, whiteKeyWidth, whiteKeyHeight, keyContext.getNextKey());
    }

    @Override
    public void paint(Graphics2D g2d)
    {
      Color color;
      if (isOn())
      {
        g2d.setColor(PRESSED_COLOR);
        g2d.fill(this);
      }
      else if ((color = releasedKeys.getHighlight(this)) != null)
      {
        g2d.setColor(color);
        g2d.fill(this);
      }
      g2d.setColor(OUTLINE_COLOR);
      g2d.draw(this);
    }
  }

  private class BlackKey extends Key
  {
    private BlackKey(KeyContext keyContext)
    {
      super(keyContext.getNextBlackKeyX(), 0, blackKeyWidth, blackKeyHeight, keyContext.getNextKey());
    }

    @Override
    public void paint(Graphics2D g2d)
    {
      if (isOn())
      {
        g2d.setColor(PRESSED_COLOR);
        g2d.fill(this);
        g2d.setColor(OUTLINE_COLOR);
        g2d.draw(this);
      }
      else
      {
        g2d.setColor(BLACK_KEY_COLOR);
        g2d.fill(this);
      }
    }
  }

  public class KeyContext
  {
    private int x = leftMargin;
    private int key = BASE_KEY;

    private int getNextWhiteKeyX()
    {
      int nextX = x;
      x += whiteKeyWidth;
      return nextX;
    }

    private int getNextBlackKeyX()
    {
      int nextX = x - blackKeyWidth / 2;
      return nextX;
    }

    private int getNextKey()
    {
      int nextKey = key++;
      return nextKey;
    }
  }

  public static class NoteOnEvent
  {
    private int key;
    private boolean isChord;

    public NoteOnEvent(int key, boolean isChord)
    {
      this.key = key;
      this.isChord = isChord;
    }

    public int getKey()
    {
      return key;
    }

    public boolean isChord()
    {
      return isChord;
    }

  }

  public static class NoteOffEvent
  {
    private int key;
    private long duration;
    private boolean isFinalKey;
    private boolean isChord;

    public NoteOffEvent(int key, long duration, boolean isChord, boolean isFinalKey)
    {
      this.key = key;
      this.duration = duration;
      this.isChord = isChord;
      this.isFinalKey = isFinalKey;
    }

    public int getKey()
    {
      return key;
    }

    public long getDuration()
    {
      return duration;
    }

    public boolean isFinalKey()
    {
      return isFinalKey;
    }

    public boolean isChord()
    {
      return isChord;
    }
  }

  public class MidiKeyboardComponentListener implements ComponentListener
  {

    public void componentHidden(ComponentEvent e)
    {
    }

    public void componentMoved(ComponentEvent e)
    {
    }

    public void componentResized(ComponentEvent e)
    {
      updateMetrics();
      createKeys();
    }

    public void componentShown(ComponentEvent e)
    {
    }

  }

  public class MidiKeyboardKeyListener implements KeyEventPostProcessor
  {
    public boolean postProcessKeyEvent(KeyEvent e)
    {
      if (e.getKeyCode() == KeyEvent.VK_SHIFT && !e.isShiftDown())
      {
        if (pressedKeys.size() > 0)
        {
          pressedKeys.release(false);
        }
      }
      return false;
    }
  }

  private class PressedKeys
  {
    private boolean isChord;
    private ArrayList<Key> keys = new ArrayList<Key>();

    public void press(Key key, boolean isShiftDown)
    {
      if (lastTime == -1)
      {
        lastTime = System.currentTimeMillis();
      }
      key.on(isShiftDown);
      keys.add(key);
      if (isShiftDown)
      {
        this.isChord = true;
      }
    }

    public int size()
    {
      return keys.size();
    }

    private void release(boolean isShiftDown)
    {
      if (!isShiftDown && keys.size() != 0)
      {
        releaseAll();
      }
    }

    private void releaseAll()
    {
      int keyIndex = 0;
      int lastIndex = keys.size() - 1;
      releasedKeys.highlight(keys);
      for (Key key : keys)
      {
        key.off(isChord, keyIndex++ == lastIndex);
      }
      broker.publish(keys, this);
      keys.clear();
      lastTime = -1;
      isChord = false;
    }

    public void drag(MouseEvent e)
    {
      Key key = getKey(e.getPoint());
      int previousIndex = keys.size() - 1;
      Key previousKey = previousIndex < 0 ? null : keys.get(previousIndex);
      if (previousKey != null && previousKey != key)
      {
        release(e.isShiftDown());
      }
      if (key != null && previousKey != key)
      {
        press(key, e.isShiftDown());
      }
    }

  }

  class ReleasedKeys
  {
    private static final int MAX_RGB_COMPONENT = 255;
    private static final int MIN_RGB_COMPONENT = 192;
    private static final int RANGE_RGB_COMPONENT = MAX_RGB_COMPONENT - MIN_RGB_COMPONENT;

    private static final int RELEASE_TIMER_REFRESH_MS = 250;
    private static final int RELEASE_HIGHLIGHT_DURATION_MS = 6000;

    private ArrayList<ReleasedKey> keys = new ArrayList<ReleasedKey>();
    private ReleasedKeysTimerListener listener = new ReleasedKeysTimerListener();
    private Timer timer = new Timer(RELEASE_TIMER_REFRESH_MS, listener);

    private void highlight(ArrayList<Key> keys)
    {
      long startTime = System.currentTimeMillis();

      for (Key key : keys)
      {
        ReleasedKey releasedKey = new ReleasedKey(key, startTime);
        this.keys.remove(releasedKey); // using releasedKey.equals
        this.keys.add(releasedKey);
      }

      if (!timer.isRunning())
      {
        timer.start();
      }
      repaint();
    }

    private void expire()
    {
      long currentTime = System.currentTimeMillis();
      for (Iterator<ReleasedKey> iterator = keys.iterator(); iterator.hasNext();)
      {
        ReleasedKey releasedKey = iterator.next();
        if (currentTime - releasedKey.startTime > RELEASE_HIGHLIGHT_DURATION_MS)
        {
          iterator.remove();
        }
      }
      if (keys.size() == 0)
      {
        timer.stop();
      }
      repaint();
    }

    public boolean isHighlighted(Key key)
    {
      for (ReleasedKey releasedKey : keys)
      {
        if (releasedKey.key == key)
        {
          return true;
        }
      }
      return false;
    }

    public Color getHighlight(Key key)
    {
      for (ReleasedKey releasedKey : keys)
      {
        if (releasedKey.key == key)
        {
          int releaseTime = (int)(System.currentTimeMillis() - releasedKey.startTime);
          int gray = Math.min(MAX_RGB_COMPONENT, MIN_RGB_COMPONENT + (releaseTime * RANGE_RGB_COMPONENT) / RELEASE_HIGHLIGHT_DURATION_MS);
          Color color = new Color(gray, gray, 255);
          return color;
        }
      }
      return null;
    }

    private class ReleasedKeysTimerListener implements ActionListener
    {
      public void actionPerformed(ActionEvent e)
      {
        expire();
      }
    }

    private class ReleasedKey
    {
      private Key key;
      private long startTime;

      private ReleasedKey(Key key, long startTime)
      {
        this.key = key;
        this.startTime = startTime;
      }

      @Override
      public boolean equals(Object obj)
      {
        return obj != null && obj instanceof ReleasedKey && ((ReleasedKey)obj).key == this.key;
      }

    }
  }

}
