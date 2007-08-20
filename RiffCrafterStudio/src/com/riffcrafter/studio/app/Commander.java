// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.app;

import com.riffcrafter.common.midi.Channel;
import com.riffcrafter.common.midi.Midi;
import com.riffcrafter.common.midi.Note;

public class Commander
{
  public static final int DEFAULT_GAP = 32;
  
  // Bet you can do this with modulo arithmetic, too.

  private static final int[] offsets = new int[] {
      0, 2, 4, 5, 7, 9, 11
  };

  private static final char[] notes = new char[] {
      'c', 'd', 'e', 'f', 'g', 'a', 'b'
  };

  private int octave = 4;
  private int channel = 0;
  private long duration = 250;
  private int key = 60;
  private long tick = 0;
  private long lastTick = 0;
  private int velocity = 64;
  private int gap = DEFAULT_GAP;

  public Midi process(Editor editor, String text)
  {
    tick = editor.getCurrentTick();
    Lexer lexer = new Lexer(text);
    Token token;

    Midi midi = new Midi();

    while ((token = lexer.parse()).isPresent())
    {
      switch (token.getCommand())
      {
        case 'a':
        case 'b':
        case 'c':
        case 'd':
        case 'e':
        case 'f':
        case 'g':
          addNote(midi, token);
          break;
        case 'C':
          channel = resolveChannel(token, channel);
          break;
        case 'D':
          duration = resolve(token, duration);
          break;
        case 'G':
          gap = resolve(token, gap);
          break;
        case 'I':
          int ticks = resolve(token, 0);
          editor.modifyTicks(tick, ticks);
          break;
        case 'K':
          key = resolveKey(token, key);
          break;
        case 'M':
          tick = resolveMeasure(editor, token);
          break;
        case 'N':
          Note note = new Note(channel, key, velocity, tick, duration - gap);
          midi.add(note);
          lastTick = tick;
          tick += duration;
          break;
        case 'O':
          octave = resolveOctave(token, octave);
          break;
        case 'R':
          ticks = resolve(token, 0);
          editor.modifyTicks(tick, -ticks);
          break;
        case 'T':
          tick = resolveTick(editor, token, tick);
          break;
        case 'V':
          velocity = resolveVelocity(token, velocity);
          break;
        case '/':
          tick = lastTick;
          break;
        case '<':
          tick = 0;
          break;
        case '>':
          tick = editor.getMaxTick();
          break;
        default:
          throw new RuntimeException("Exepected one of a, b, c, d, e, f, g, h, k, m, n, r, t, v, /, <, >, received " + token.getCommand());
      }
    }

    return midi;
  }

  public int resolve(Token token, int value)
  {
    return (int)resolve(token, (long)value);
  }

  public long resolve(Token token, long value)
  {
    long number;

    if (token.hasNumber())
    {
      number = token.getNumber();
    }
    else if (token.getSign() != 0)
    {
      number = 1;
    }
    else
    {
      number = value;
    }

    switch (token.getSign())
    {
      case '+':
        value += number;
        break;
      case '-':
        value -= number;
        break;
      default:
        value = number;
        break;
    }

    return value;
  }

  private int resolveOctave(Token token, int octave)
  {
    octave = resolve(token, octave);
    if (octave < 1 || octave > 9)
    {
      throw new RuntimeException("Expected octave in range 1 through 9, received " + octave);
    }
    return octave; // only return it if valid
  }

  public int resolveChannel(Token token, int channel)
  {
    int channelNumber = Channel.getChannelNumber(channel);
    channelNumber = resolve(token, channelNumber);
    channel = Channel.getChannelIndex(channelNumber);
    if (channelNumber < 1 || channelNumber > 16)
    {
      throw new RuntimeException("Expected channel in range 1 through 16, received " + channelNumber);
    }
    return channel; // only return it if valid
  }

  private int resolveKey(Token token, int key)
  {
    key = resolve(token, key);
    if (key < 0 || key > 127)
    {
      throw new RuntimeException("Expected key in range 0 through 127, received " + key);
    }
    return key; // only return it if valid
  }

  private long resolveMeasure(Editor editor, Token token)
  {
    long ticksPerMeasure = editor.findTicksPerMeasure(tick);
    long measure = (tick / ticksPerMeasure);
    measure = resolve(token, measure);
    long lastMeasure = (editor.getMaxTick() / ticksPerMeasure) + 4;
    if (measure < 0 || measure > lastMeasure)
    {
      throw new RuntimeException("Expected measure in range 0 through " + lastMeasure + ", received " + measure);
    }
    tick = measure * ticksPerMeasure;
    return tick; // only return it if valid
  }

  private long resolveTick(Editor editor, Token token, long tick)
  {
    tick = resolve(token, tick);
    long lastTick = editor.getMaxTick();
    lastTick += 4 * editor.findTicksPerMeasure(lastTick);
    if (tick < 0 || tick > lastTick)
    {
      throw new RuntimeException("Expected tick in range 0 through " + lastTick + ", received " + lastTick);
    }
    return tick; // only return it if valid
  }

  private int resolveVelocity(Token token, int velocity)
  {
    velocity = resolve(token, velocity);
    if (velocity < 0 || velocity > 127)
    {
      throw new RuntimeException("Expected velocity in range 0 through 127, received " + velocity);
    }
    return velocity; // only return it if valid
  }

  private int findOffset(char c)
  {
    for (int i = 0; i < notes.length; i++)
    {
      if (notes[i] == c)
      {
        return offsets[i];
      }
    }
    return 0;
  }

  private void addNote(Midi midi, Token token)
  {
    octave = resolveOctave(token, octave);
    char c = token.getCommand();
    key = (1 + octave) * 12 + findOffset(c);
    switch (token.getSymbol())
    {
      case '@':
        key--;
        break;
      case '#':
        key++;
        break;
    }
    Note note = new Note(channel, key, velocity, tick, duration - gap);
    midi.add(note);
    lastTick = tick;
    tick += duration;
  }

  public int getChannel()
  {
    return channel;
  }

  public void setChannel(int channel)
  {
    this.channel = channel;
  }

  public long getDuration()
  {
    return duration;
  }

  public void setDuration(long duration)
  {
    this.duration = duration;
  }

  public int getGap()
  {
    return gap;
  }

  public void setGap(int gap)
  {
    this.gap = gap;
  }

  public int getKey()
  {
    return key;
  }

  public void setKey(int key)
  {
    this.key = key;
  }

  public long getLastTick()
  {
    return lastTick;
  }

  public void setLastTick(long lastTick)
  {
    this.lastTick = lastTick;
  }

  public int getOctave()
  {
    return octave;
  }

  public void setOctave(int octave)
  {
    this.octave = octave;
  }

  public long getTick()
  {
    return tick;
  }

  public void setTick(long tick)
  {
    this.tick = tick;
  }

  public int getVelocity()
  {
    return velocity;
  }

  public void setVelocity(int velocity)
  {
    this.velocity = velocity;
  }

  private class Lexer
  {
    private static final int INITIAL = 0;
    private static final int SIGN = 1;
    private static final int NUMBER = 2;
    private static final int SYMBOL = 3;
    private static final int FINAL = 4;

    private String text;
    private int next;
    private int last;

    private Lexer(String text)
    {
      this.text = text;
      next = 0;
      last = text.length();
    }

    private Token parse()
    {
      int state = INITIAL;
      Token token = new Token();

      while (next < last && state != FINAL)
      {
        char c = text.charAt(next);
        switch (state)
        {
          case INITIAL:
            if (!Character.isLetter(c) && "/<>".indexOf(c) == -1)
            {
              throw new RuntimeException("Expected command character at position " + next + " in " + text + ", received " + c);
            }
            token.setCommand(c);
            state = SIGN;
            next++;
            break;
          case SIGN:
            if (c == '+' || c == '-')
            {
              token.setSign(c);
              next++;
            }
            state = NUMBER;
            break;
          case NUMBER:
            if (Character.isDigit(c))
            {
              token.appendDigit(c);
              next++;
            }
            else
            {
              state = SYMBOL;
            }
            break;
          case SYMBOL:
            if (c == '#' || c == '@')
            {
              token.setSymbol(c);
              next++;
            }
            state = FINAL;
            break;
        }
      }
      return token;
    }
  }

  private class Token
  {
    private char command;
    private char sign;
    private int number;
    private char symbol;
    private boolean isPresent;
    private boolean hasNumber;

    public char getCommand()
    {
      return command;
    }

    public void setCommand(char command)
    {
      this.command = command;
      this.isPresent = true;
    }

    public boolean hasNumber()
    {
      return hasNumber;
    }

    public boolean isPresent()
    {
      return isPresent;
    }

    public int getNumber()
    {
      return number;
    }

    public char getSign()
    {
      return sign;
    }

    public void setSign(char sign)
    {
      this.sign = sign;
    }

    public char getSymbol()
    {
      return symbol;
    }

    public void setSymbol(char symbol)
    {
      this.symbol = symbol;
    }

    public void appendDigit(char c)
    {
      number = number * 10 + (c - '0');
      hasNumber = true;
    }

  }

}
