// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.app;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat.Encoding;

import com.riffcrafter.common.midi.Converter;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.common.midi.MidiConstants;
import com.riffcrafter.common.midi.Note;
import com.riffcrafter.common.thirdparty.FFT;

// The trick is not detecting note duration, it's detecting the articulation
// of a note (i.e. when is a note in two consecutive samples two separate notes?)

// For example 22,050 samples per second, 1,024 samples per window means 21.53
// iterations per second, 46 ms per iteration resolution. In actual practice, with
// this code, we average about 47 ms per iteration.

public class Recorder
{
  // These parameters affect frequency range
  private static final boolean BIG_ENDIAN = true;
  private static final int NUMBER_OF_CHANNELS = 1;
  private static final int BITS_PER_SAMPLE = 16;
  private static final float SAMPLES_PER_SECOND = 22050;
  private static final int FRAME_SIZE = (BITS_PER_SAMPLE / 8) * NUMBER_OF_CHANNELS;

  // These parameters affect frequency resolution
  private static final int FFT_BITS = 12; // 12
  private static final int FFT_BUFFER_SIZE = 1 << FFT_BITS;
  private static final int FFT_REAL_SIZE = FFT_BUFFER_SIZE / 2;
  private static final double HERTZ_PER_INDEX = SAMPLES_PER_SECOND / FFT_BUFFER_SIZE;

  // These parameters affect time resolution
  private static final int PCM_WINDOW_SIZE = 256; // 1024
  private static final int PCM_WINDOW_BYTES = PCM_WINDOW_SIZE * 2;
  private static final int PCM_FFT_SHIFT_REGISTER_SIZE = FFT_BUFFER_SIZE - PCM_WINDOW_SIZE;
  private static final int PCM_FFT_SHIFT_REGISTER_BYTES = PCM_FFT_SHIFT_REGISTER_SIZE * 2;

  public static final int MIN_SENSITIVITY = 0;
  public static final int MAX_SENSITIVITY = 200;

  public static final int MIN_CLIP = 0;
  public static final int MAX_CLIP = 1000;

  public static final int MIN_DURATION = 0;
  public static final int MAX_DURATION = 200;

  // HERTZ_PER_INDEX is 5.38 at 22,050 samples per second and 4,096 bytes per FFT buffer. This
  // means the lowest midi keyboard key change we can detect is 42 to 43. 

  private static final int FIRST_MIDI_KEY = 43;

  // SAMPLES_PER_SECOND is 22,050, which gives us a Nyquist frequency of 11,025. This
  // means that the highest midi keyboard key that we can detect is 124.

  private static final int LAST_MIDI_KEY = 124;

  private boolean isRunning;
  private boolean isRecording;
  private TargetDataLine inputLine;

  private int sensitivity = (MAX_SENSITIVITY - MIN_SENSITIVITY) / 2;
  private int clip = (MAX_CLIP - MIN_CLIP) / 2;
  private int minimumDuration = (MAX_DURATION - MIN_DURATION) / 2;

  private FFT fft;
  private double[] xr;
  private double[] xi;
  private long[] keyMillis;
  private byte[] keyVelocities;

  private Callable consumer;

  private int channel;
  private Editor editor;

  private long startTime;
  private long lastTime;
  private long baseTick;

  private TreeSet<Note> recordedNotes;

  public int available;

  private int samples;

  public Recorder(Callable consumer)
  {
    this.consumer = consumer;

    AudioFormat audioFormat = new AudioFormat(Encoding.PCM_SIGNED, SAMPLES_PER_SECOND, BITS_PER_SAMPLE, NUMBER_OF_CHANNELS, FRAME_SIZE, SAMPLES_PER_SECOND, BIG_ENDIAN);
    inputLine = getInputLine(audioFormat);

    fft = new FFT(FFT_BITS);
    xr = new double[FFT_BUFFER_SIZE];
    xi = new double[FFT_BUFFER_SIZE];
    keyMillis = new long[MidiConstants.MAX_MIDI_KEYS];
    keyVelocities = new byte[MidiConstants.MAX_MIDI_KEYS];
  }

  public void selectChannel(Editor editor, int channel)
  {
    if (editor == null && isRunning)
    {
      stop();
    }

    this.editor = editor;
    this.channel = channel;
  }

  public double[] getXr()
  {
    return xr;
  }

  public byte[] getKeys()
  {
    return keyVelocities;
  }

  public void setSensitivity(int sensitivity)
  {
    this.sensitivity = sensitivity;
  }

  public void setClip(int clip)
  {
    this.clip = clip;
  }

  public void setMinimumDuration(int minimumDuration)
  {
    this.minimumDuration = minimumDuration;
  }

  public void monitor()
  {
    isRecording = false;
    start();
  }

  public void record()
  {
    isRecording = true;
    recordedNotes = new TreeSet<Note>(new Comparator<Note>()
    {
      public int compare(Note o1, Note o2)
      {
        long deltaTick = o1.getTick() - o2.getTick();
        if (deltaTick != 0)
        {
          return (int)deltaTick;
        }
        return o1.hashCode() - o2.hashCode();
      }
    });
    start();
  }

  private void start()
  {
    if (editor == null)
    {
      return;
    }

    if (isRunning)
    {
      stop();
    }

    Arrays.fill(keyMillis, (long)0);
    Arrays.fill(keyVelocities, (byte)0);

    available = 0;
    samples = 0;

    open(inputLine);
    isRunning = true;
    inputLine.start();
    Thread thread = new Thread(new RecorderRunnable(), "Recorder");
    startTime = System.currentTimeMillis();
    baseTick = editor.getCurrentTick();
    thread.start();
  }

  public void stop()
  {
    Arrays.fill(keyVelocities, (byte)0);

    if (samples > 0)
    {
      long millisPerIteration = (System.currentTimeMillis() - startTime) / samples;
      //System.out.println("Recorder.stop: samples=" + samples + ", avg(available)=" + (available / samples) + ", " + millisPerIteration + " ms/sample");
    }

    inputLine.stop();
    inputLine.drain();
    inputLine.close();
    isRunning = false;
  }

  public boolean hasRecordedNotes()
  {
    return recordedNotes != null && recordedNotes.size() > 0;
  }

  public void clearRecordedNotes()
  {
    recordedNotes = null;
  }

  public void filterRecordedNotes()
  {
    ArrayList<ArrayList<Note>> noteGroups = new ArrayList<ArrayList<Note>>();
    for (Note note : recordedNotes)
    {
      ArrayList<Note> noteGroup = findNoteGroup(note, noteGroups);
      if (noteGroup == null)
      {
        noteGroup = new ArrayList<Note>();
        noteGroups.add(noteGroup);
      }
      noteGroup.add(note);
    }

    Midi notesToRemove = new Midi();

    for (ArrayList<Note> noteGroup : noteGroups)
    {
      long maxDuration = 0;
      Note maxDurationNote = null;
      for (Note noteGroupNote : noteGroup)
      {
        long duration = noteGroupNote.getDuration();
        if (duration > maxDuration)
        {
          maxDuration = duration;
          maxDurationNote = noteGroupNote;
        }
      }
      for (Note noteGroupNote : noteGroup)
      {
        if (noteGroupNote != maxDurationNote)
        {
          notesToRemove.add(noteGroupNote);
        }
      }
    }

    editor.setSelection(notesToRemove, false, false);
    //editor.delete();
  }

  private ArrayList<Note> findNoteGroup(Note note, ArrayList<ArrayList<Note>> noteGroups)
  {
    for (ArrayList<Note> noteGroup : noteGroups)
    {
      for (Note noteGroupNote : noteGroup)
      {
        if (noteGroupNote.isAdjacentKeys(note) && noteGroupNote.isOverlappingTicks(note))
        {
          return noteGroup;
        }
      }
    }
    return null;
  }

  private void processNotes()
  {
    long currentTimeMillis = System.currentTimeMillis();
    final ArrayList<Note> notes = new ArrayList<Note>();

    for (int i = FIRST_MIDI_KEY; i < LAST_MIDI_KEY; i++)
    {
      double f = Converter.convertKeyToFrequency(i);
      int index = (int)Math.round(f / Recorder.HERTZ_PER_INDEX);
      double amplitude = Math.abs(xr[index]);
      if (amplitude > sensitivity)
      {
        if (keyMillis[i] == 0)
        {
          keyMillis[i] = currentTimeMillis;
        }
        keyVelocities[i] = (byte)Math.max(keyVelocities[i], getVelocity(index));
      }
      else
      {
        if (keyMillis[i] != 0)
        {
          //System.out.println("Recorder.processNotes: sensitivity=" + sensitivity + ", millis=" + currentTimeMillis + ", keyVelocities[" + i + "]=" + keyVelocities[i] + ", amplitude=" + amplitude);
          if (amplitude > 1)
          {
            continue;
          }
          if (isRecording)
          {
            long milliTick = keyMillis[i] - startTime;
            long milliDuration = currentTimeMillis - keyMillis[i];
            Note note = new Note(channel, i, keyVelocities[i], milliTick, milliDuration);
            Midi.convertMillisToTicks(note);
            if (note.getDuration() > minimumDuration)
            {
              note.setTick(note.getTick() + baseTick);
              notes.add(note);
              recordedNotes.add(note);
            }
            keyMillis[i] = 0;
          }
          keyVelocities[i] = 0;
        }
      }
    }

    EventQueue.invokeLater(new Runnable()
    {
      public void run()
      {
        for (Note note : notes)
        {
          // TODO: Use a pub/sub or listener pattern to broadcast the notes
          editor.addNoteAbsolute(note, false);
        }
      }
    });
  }

  private int getVelocity(int index)
  {
    int invertedClip = MAX_CLIP - clip;
    if (invertedClip == 0)
    {
      invertedClip = 1;
    }
    int velocityRange = MidiConstants.LAST_VELOCITY - MidiConstants.DEFAULT_VELOCITY;
    // Map 0 to clip in the xr scale to DEFAULT_VELOCITY to LAST_VELOCITY in the velocity scale
    double volume = (Math.abs(xr[index]) / invertedClip) * velocityRange;
    int velocity = MidiConstants.DEFAULT_VELOCITY + (int)volume;
    if (velocity > MidiConstants.LAST_VELOCITY)
    {
      velocity = MidiConstants.LAST_VELOCITY;
    }
    return velocity;
  }

  private TargetDataLine getInputLine(AudioFormat audioFormat)
  {
    try
    {
      return AudioSystem.getTargetDataLine(audioFormat);
    }
    catch (LineUnavailableException e1)
    {
      throw new RuntimeException(e1);
    }
  }

  private void open(TargetDataLine dataLine)
  {
    try
    {
      dataLine.open();
    }
    catch (LineUnavailableException e)
    {
      throw new RuntimeException(e);
    }
  }

  public void notifyConsumer()
  {
    try
    {
      consumer.call();
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  // Given that we need FFT_BUFFER_SIZE bytes on each iteration, there are
  // several ways to get there:
  //
  // 1. Clear the FFT buffer and read a (shorter) sample of PCM data into it. This
  // is what resetBufferAndRead does.
  //
  // 2. Maintain a shift register of buffers. Shift old PCM samples to the front
  // and append new PCM samples to the end.
  //
  // The first approach favors better time resolution. The second approach
  // favors better volume resolution.

  public class RecorderRunnable implements Runnable
  {
    private byte[] window;

    public void run()
    {
      resetBufferAndRead();
    }

    public void resetBufferAndRead()
    {
      window = new byte[2 * xr.length];

      while (isRunning)
      {
        available += inputLine.available();
        samples++;

        Arrays.fill(window, (byte)0);

        int readCount = inputLine.read(window, 0, PCM_WINDOW_BYTES);
        if (readCount == PCM_WINDOW_BYTES)
        {
          for (int i = 0, j = 0; i < window.length; i += 2, j++)
          {
            xr[j] = (short)(window[i] << 8 | (window[i + 1] & 0xff));
          }
          fft.doFFT(xr, xi, false);
          processNotes();
          notifyConsumer();
        }
      }
    }

    public void readBufferAndShift()
    {
      window = new byte[2 * xr.length];
      Arrays.fill(window, (byte)0);
      while (isRunning)
      {
        for (int i = 0, j = PCM_WINDOW_BYTES; i < PCM_FFT_SHIFT_REGISTER_BYTES; i++, j++)
        {
          window[i] = window[j];
        }

        available += inputLine.available();
        samples++;

        int readCount = inputLine.read(window, PCM_FFT_SHIFT_REGISTER_BYTES, PCM_WINDOW_BYTES);
        if (readCount == PCM_WINDOW_BYTES)
        {

          for (int i = 0, j = 0; i < window.length; i += 2, j++)
          {
            xr[j] = (short)(window[i] << 8 | (window[i + 1] & 0xff));
          }

          fft.doFFT(xr, xi, false);
          processNotes();
          notifyConsumer();
        }
      }
    }

  }

}
