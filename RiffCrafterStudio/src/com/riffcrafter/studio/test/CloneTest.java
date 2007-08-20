// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.studio.test;

public class CloneTest
{

  public static void main(String[] args)
  {
    A a1 = new A(3, 4);
    A a2 = a1.clone();
    System.out.println("a1=" + a1);
    System.out.println("a2=" + a2);
    B b1 = new B(5);
    B b2 = (B)b1.clone();
    System.out.println("b1=" + b1);
    System.out.println("b2=" + b2);
  }

  private static class A implements Cloneable
  {
    protected int x;
    protected int y;

    public A()
    {

    }

    public A(int x, int y)
    {
      this.x = x;
      this.y = y;
    }

    @Override
    protected A clone()
    {
      try
      {
        return (A)super.clone();
      }
      catch (CloneNotSupportedException e)
      {
        throw new RuntimeException(e);
      }
    }

    public String toString()
    {
      return "x=" + x + ", y=" + y;
    }

  }

  private static class B extends A
  {
    private int z;

    private B(int z)
    {
      this.z = z;
      x = 42;
    }

    public String toString()
    {
      return "x=" + x + ", y=" + y + ", z=" + z;
    }

  }

}
