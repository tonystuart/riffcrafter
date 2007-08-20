// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.midi;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;
import javax.swing.JScrollBar;

import com.riffcrafter.common.midi.Midel;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.library.util.Broker;
import com.riffcrafter.library.util.Broker.Listener;
import com.riffcrafter.studio.app.Editor.Bridger;

public class Scroller extends JPanel
{
  private Midi midi;
  private Notator notator;
  private JScrollBar verticalScrollBar;
  private JScrollBar horizontalScrollBar;
  private JPanel filler;

  private boolean isInSetCurrentTick;

  private DownwardListener downwardListener = new DownwardListener();
  private Broker upwardBroker = new Broker();

  private boolean isInSetScrollBarMaxima;

  public Scroller(Notator notator, Midi midi, Bridger bridger)
  {
    this.notator = notator;
    notator.addMouseListener(new ScrollerlMouseListener());
    notator.addComponentListener(new NotatorComponentListener());

    this.midi = midi;
    midi.addMidiListener(new ScrollerMidiListener());
    int ticksPerMeasure = midi.findTicksPerMeasure(0);

    verticalScrollBar = new JScrollBar(JScrollBar.VERTICAL);
    verticalScrollBar.setValue(0);
    verticalScrollBar.setMinimum(0);
    verticalScrollBar.setBlockIncrement(100);
    verticalScrollBar.setUnitIncrement(10);
    verticalScrollBar.addAdjustmentListener(new VerticalScrollBarListener());

    horizontalScrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
    horizontalScrollBar.setValue(0);
    horizontalScrollBar.setMinimum(0);
    horizontalScrollBar.setBlockIncrement(ticksPerMeasure);
    horizontalScrollBar.setUnitIncrement(Midi.DEFAULT_RESOLUTION);
    horizontalScrollBar.addAdjustmentListener(new HorizontalScrollBarListener());

    filler = new JPanel();
    filler.setFocusable(false);

    setBackground(notator.getBackground());
    add(verticalScrollBar);
    add(horizontalScrollBar);
    add(filler);
    add(notator);

    bridger.subscribe(downwardListener, upwardBroker);
  }

  private int getVerticalMaximum()
  {
    Dimension notatorPreferredSize = notator.getPreferredSize();
    int verticalMaximum = notatorPreferredSize.height - notator.getHeight() + verticalScrollBar.getVisibleAmount();
    return verticalMaximum;
  }

  private int getHorizontalMaximum()
  {
    int maxTick = (int)midi.getMaxTick();
    int horizontalMaximum = maxTick + horizontalScrollBar.getVisibleAmount() + midi.findTicksPerMeasure(maxTick) * 5;
    return horizontalMaximum;
  }

  private void setScrollBarMaxima()
  {
    // Not sure why JScrollBar thinks it should call adjustmentValueChanged under these circumstances, but it does
    isInSetScrollBarMaxima = true;

    int verticalMaximum = getVerticalMaximum();
    if (verticalMaximum > 0)
    {
      verticalScrollBar.setMaximum(verticalMaximum);
    }

    int horizontalMaximum = getHorizontalMaximum();
    if (horizontalMaximum > 0)
    {
      horizontalScrollBar.setMaximum(horizontalMaximum);
    }

    isInSetScrollBarMaxima = false;
  }

  public long getCurrentTick()
  {
    return horizontalScrollBar.getValue();
  }

  public void setCurrentTick(long tickPosition)
  {
    isInSetCurrentTick = true;
    horizontalScrollBar.setValue((int)tickPosition); // onTickChange is invoked via adjustmentValueChanged
    isInSetCurrentTick = false;
  }

  private void scrollIntoView(Point currentPoint)
  {
    int height = notator.getHeight();
    if (currentPoint.y < 0)
    {
      int delta = currentPoint.y;
      verticalScrollBar.setValue(verticalScrollBar.getValue() + delta);
    }
    else if (currentPoint.y > height)
    {
      int delta = (currentPoint.y - height);
      verticalScrollBar.setValue(verticalScrollBar.getValue() + delta);
    }

    int width = notator.getWidth();

    if (currentPoint.x > width)
    {
      int delta = (currentPoint.x - width) * notator.getTicksPerPixel();
      int tick = horizontalScrollBar.getValue() + delta;
      notator.notifyMouseOutsideWindow(tick);
      horizontalScrollBar.setValue(tick);
    }

    else if (currentPoint.x < 0)
    {
      int delta = currentPoint.x * notator.getTicksPerPixel();
      int tick = horizontalScrollBar.getValue() + delta;
      if (tick < 0)
      {
        tick = 0;
      }
      notator.notifyMouseOutsideWindow(tick);
      horizontalScrollBar.setValue(tick);
    }
  }

  public void scrollChannelIntoView(int channel)
  {
    Rectangle r = notator.getYChannel(channel);
    if (r != null)
    {
      int origin = verticalScrollBar.getValue();
      if (!isValid())
      {
        return; // ignore requests until we're initialized
      }
      int windowHeight = getHeight() - horizontalScrollBar.getHeight();
      if (r.y < origin)
      {
        verticalScrollBar.setValue(r.y);
      }
      else if ((r.y + r.height) > (origin + windowHeight))
      {
        int top = r.y - (windowHeight - r.height); // we want window to go down, so top goes up!
        verticalScrollBar.setValue(top);
      }
    }
  }

  public void doLayout()
  {
    int width = getWidth();
    int height = getHeight();

    int vsbWidth = verticalScrollBar.getMinimumSize().width;
    int hsbHeight = horizontalScrollBar.getMinimumSize().height;

    Dimension notatorPreferredSize = notator.getPreferredSize();

    long horizontalMaximum = getHorizontalMaximum();

    boolean isVsbNeeded = notatorPreferredSize.height > (height - hsbHeight);
    boolean isHsbNeeded = horizontalMaximum > (width - vsbWidth);

    if (!isVsbNeeded)
    {
      vsbWidth = 0;
    }

    if (!isHsbNeeded)
    {
      hsbHeight = 0;
    }

    notator.setBounds(0, 0, width - vsbWidth, height - hsbHeight);
    verticalScrollBar.setBounds(width - vsbWidth, 0, vsbWidth, height - hsbHeight);
    horizontalScrollBar.setBounds(0, height - hsbHeight, width - vsbWidth, hsbHeight);
    filler.setBounds(width - vsbWidth, height - hsbHeight, vsbWidth, hsbHeight);
  }

  private void onMidiUpdate()
  {
    // TODO: At this point, it is quite likely that the Notator has not
    // processed the add message, because it just schedules it and then
    // waits until it gets a paint. This is by design, otherwise the
    // overhead for each modification would be excessive. 
    //      setScrollBarMaxima();
    //      doLayout();
    // We work around this (as of Java build 1.6.0-b105) by queueing,
    // the operation. Presumably it is invoked after the Notator gets
    // its paint message because it works.
    EventQueue.invokeLater(new Runnable()
    {
      public void run()
      {
        setScrollBarMaxima();
        doLayout();
      }
    });
  }

  private class ScrollerMidiListener implements Midi.MidiListener
  {
    public void onAddMidel(Midi midi, Midel selection)
    {
      onMidiUpdate();
    }

    public void onRemoveMidel(Midi midi, Midel selection)
    {
      onMidiUpdate();
    }
  }

  class VerticalScrollBarListener implements AdjustmentListener
  {
    public void adjustmentValueChanged(AdjustmentEvent e)
    {
      if (!isInSetScrollBarMaxima)
      {
        int verticalOffset = verticalScrollBar.getValue();
        notator.setVerticalOffset(verticalOffset);
        notator.repaint();
      }
    }
  }

  class HorizontalScrollBarListener implements AdjustmentListener
  {
    public void adjustmentValueChanged(AdjustmentEvent e)
    {
      int tickPosition = horizontalScrollBar.getValue();
      notator.setCurrentTick(tickPosition, false);
      ;
      if (!isInSetCurrentTick)
      {
        // Filter out changes initiated by setCurrentTick, leaving only user-initiated changes, to avoid recursive loops
        upwardBroker.publish(e, Scroller.this);
      }
    }

  }

  public class ScrollerlMouseListener implements MouseListener, MouseMotionListener
  {

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
      notator.addMouseMotionListener(this);
    }

    public void mouseReleased(MouseEvent e)
    {
      notator.removeMouseMotionListener(this);
    }

    public void mouseDragged(MouseEvent e)
    {
      scrollIntoView(e.getPoint());
    }

    public void mouseMoved(MouseEvent e)
    {
    }

  }

  public class NotatorComponentListener implements ComponentListener
  {

    public void componentHidden(ComponentEvent e)
    {
    }

    public void componentMoved(ComponentEvent e)
    {
    }

    public void componentResized(ComponentEvent e)
    {
      // Called on initial Notator layout (before first paint) and then all subsequent resize operations
      setScrollBarMaxima();
    }

    public void componentShown(ComponentEvent e)
    {
    }

  }

  public class DownwardListener implements Listener
  {
    public void notify(Object event, Object source)
    {
      if (source != Scroller.this)
      {
        long currentTick = (Long)event;
        setCurrentTick(currentTick);
      }
    }
  }

}
