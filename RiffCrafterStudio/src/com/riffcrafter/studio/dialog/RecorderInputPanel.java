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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.riffcrafter.common.midi.KeyboardPanel;
import com.riffcrafter.common.midi.MidiConstants;
import com.riffcrafter.library.dialog.GridBagPanel;
import com.riffcrafter.library.dialog.ImageButtonGroup;
import com.riffcrafter.studio.app.Editor;
import com.riffcrafter.studio.app.Recorder;


public class RecorderInputPanel extends GridBagPanel
{
  private Recorder recorder;
  private ScopePanel scopePanel;
  private RecorderControlPanel recorderControlPanel;
  private SensitivityPanel sensitivityPanel;
  private ClipPanel clipPanel;
  private DurationPanel durationPanel;

  public RecorderInputPanel()
  {
    recorder = new Recorder(new ScopeCallable());

    scopePanel = new ScopePanel();
    scopePanel.setBackground(Color.BLACK);
    add(scopePanel, "x=0,y=0,top=0,left=5,bottom=0,right=5,anchor=w,fill=b,weightx=1,weighty=1,gridwidth=*,gridheight=1");

    recorderControlPanel = new RecorderControlPanel();
    recorderControlPanel.setBorder("titled: Control ");
    add(recorderControlPanel, "x=0,y=1,top=0,left=0,bottom=0,right=0,anchor=w,fill=n,weightx=0,weighty=0,gridwidth=1,gridheight=1");

    sensitivityPanel = new SensitivityPanel();
    sensitivityPanel.setBorder("titled: Sensitivity ");
    add(sensitivityPanel, "x=1,y=1,top=0,left=0,bottom=0,right=0,anchor=w,fill=h,weightx=1,weighty=1,gridwidth=1,gridheight=1");

    clipPanel = new ClipPanel();
    clipPanel.setBorder("titled: Clip ");
    add(clipPanel, "x=2,y=1,top=0,left=0,bottom=0,right=0,anchor=w,fill=h,weightx=1,weighty=1,gridwidth=1,gridheight=1");

    durationPanel = new DurationPanel();
    durationPanel.setBorder("titled: Duration ");
    add(durationPanel, "x=3,y=1,top=0,left=0,bottom=0,right=0,anchor=w,fill=h,weightx=1,weighty=1,gridwidth=1,gridheight=1");
  }

  public void selectChannel(Editor editor, int channel)
  {
    recorder.selectChannel(editor, channel);
  }

  public class RecorderControlPanel extends ImageButtonGroup
  {
    private static final int MONITOR = 1;
    private static final int RECORD = 2;
    private static final int STOP = 3;

    private RecorderControlPanel()
    {
      super(24, 24);
      add("Recorder-Monitor-16x16.png", MONITOR, "Monitor input levels without recording");
      add("Recorder-Record-16x16.png", RECORD, "Start recording to current channel");
      add("Recorder-Stop-16x16.png", STOP, "Stop monitoring or recording");
      addActionListener(new ControlPanelListener());
    }

    public class ControlPanelListener implements ActionListener
    {
      public void actionPerformed(ActionEvent e)
      {
        ImageButton button = (ImageButton)e.getSource();
        int value = button.getValue();

        switch (value)
        {
          case MONITOR:
            recorder.monitor();
            break;
          case RECORD:
            recorder.record();
            break;
          case STOP:
            recorder.stop();
            if (recorder.hasRecordedNotes())
            {
              if (CommonDialog.showYesNo(RecorderInputPanel.this, "Would you like to apply the input filter to select dissonant notes?"))
              {
                recorder.filterRecordedNotes();
              }
              recorder.clearRecordedNotes();
            }
            break;
        }
      }
    }

  }

  private void setComponentSize(Component component)
  {
    // Sliders must have a small preferred width. With the grid bag,
    // they are filled to available space. Else they end up very big.
    Dimension size = component.getPreferredSize();
    size.width = 30;
    component.setPreferredSize(size);
  }

  public class SensitivityPanel extends GridBagPanel
  {
    private JSlider sensitivitySlider;

    public SensitivityPanel()
    {
      sensitivitySlider = new JSlider(Recorder.MIN_SENSITIVITY, Recorder.MAX_SENSITIVITY);
      sensitivitySlider.addChangeListener(new SensitivityChangeListener());
      setComponentSize(sensitivitySlider);
      add(sensitivitySlider, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
    }

    public class SensitivityChangeListener implements ChangeListener
    {
      public void stateChanged(ChangeEvent e)
      {
        int sensitivity = sensitivitySlider.getMaximum() - sensitivitySlider.getValue();
        recorder.setSensitivity(sensitivity);
      }
    }
  }

  public class ClipPanel extends GridBagPanel
  {
    private JSlider clipSlider;

    public ClipPanel()
    {
      clipSlider = new JSlider(Recorder.MIN_CLIP, Recorder.MAX_CLIP);
      clipSlider.addChangeListener(new ClipChangeListener());
      setComponentSize(clipSlider);
      add(clipSlider, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
    }

    public class ClipChangeListener implements ChangeListener
    {
      public void stateChanged(ChangeEvent e)
      {
        int clip = clipSlider.getMaximum() - clipSlider.getValue();
        recorder.setClip(clip);
      }
    }
  }

  public class DurationPanel extends GridBagPanel
  {
    private JSlider durationSlider;

    public DurationPanel()
    {
      durationSlider = new JSlider(Recorder.MIN_DURATION, Recorder.MAX_DURATION);
      durationSlider.addChangeListener(new DurationChangeListener());
      setComponentSize(durationSlider);
      add(durationSlider, "x=0,y=0,top=0,left=0,bottom=0,right=0,anchor=w,fill=h,weightx=1,weighty=0,gridwidth=1,gridheight=1");
    }

    public class DurationChangeListener implements ChangeListener
    {
      public void stateChanged(ChangeEvent e)
      {
        recorder.setMinimumDuration(durationSlider.getValue());
      }
    }
  }

  public class ScopePanel extends JComponent
  {
    public ScopePanel()
    {
      Dimension size = new Dimension(KeyboardPanel.PREFERRED_WIDTH, KeyboardPanel.PREFERRED_HEIGHT);
      setMinimumSize(size);
      setPreferredSize(size);
      setMaximumSize(size);
    }

    public void paint(Graphics g)
    {
      int width = getWidth();
      int height = getHeight();
      g.setColor(getBackground());
      g.fillRect(0, 0, width, height);

      byte[] keys = recorder.getKeys();

      for (int x = 1; x < width; x++)
      {
        int index = x * keys.length / width;
        if (keys[index] > 0)
        {
          if (keys[index] == MidiConstants.LAST_VELOCITY)
          {
            g.setColor(Color.RED);
          }
          else
          {
            g.setColor(Color.GREEN);
          }
          double scaleFactor = (double)height / MidiConstants.LAST_VELOCITY;
          double barHeight = keys[index] * scaleFactor;
          //System.out.println("keys[" + index + "]=" + keys[index] + ", height=" + height + ", scaleFactor=" + scaleFactor + ", barHeight=" + barHeight);
          g.drawLine(x, height, x, height - (int)barHeight);
        }
      }
    }
  }

  public class ScopeCallable implements Callable
  {
    public Object call() throws Exception
    {
      scopePanel.repaint();
      return null;
    }
  }

}
