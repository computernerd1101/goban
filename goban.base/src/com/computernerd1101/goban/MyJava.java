package com.computernerd1101.goban;

import java.util.*;

import kotlin.jvm.JvmWildcard;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLongArray;

public class MyJava {

    public static void main(String[] args) {
        int[][][] array = new int[2][3][];
        Goban goban = new Goban();
    }


    public int foo;

    public int getBar() { return foo; }

    public void setBar(int bar) { foo = bar; }

    public static <E> void testSet(Set<E> set) {
        for(E e: set) {
            System.out.println(e);
        }
    }

}
