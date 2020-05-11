package com.computernerd1101.goban;

import java.util.*;

import kotlin.jvm.JvmWildcard;

import java.util.Set;

public class MyJava {

    public static void main(String[] args) {
        GoPointSet set = GoPointSet.EMPTY;
    }

    public static <E> void testSet(Set<E> set) {
        for(E e: set) {
            System.out.println(e);
        }
    }

}
