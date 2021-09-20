package com.computernerd1101.goban.test.sandbox;


import com.computernerd1101.goban.*;

import java.lang.reflect.Constructor;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class MyJava {

    public static void main(String[] args) throws Exception {
        Constructor<MyInnerClass> innerClassConstructor = MyInnerClass.class.getConstructor(MyJava.class, int.class);
        System.out.println(Arrays.toString(innerClassConstructor.getGenericParameterTypes()));
        MyInnerClass instance = innerClassConstructor.newInstance(new MyJava(), 42);
        instance.print();
    }

    public class MyInnerClass {

        public MyInnerClass(int i) {
            value = i;
        }

        public final int value;

        public void print() {
            System.out.println(MyJava.this);
            System.out.println("value = " + value);
        }

    }

    @NotNull
    public String notNullToNotNull(@NotNull String string) {
        return string;
    }

    @Nullable
    public String nullableToNullable(@Nullable String string) {
        return string;
    }

    @NotNull
    public String nullableToNotNull(@Nullable String string) {
        return string == null ? "" : string;
    }

    @Nullable
    public String notNullToNullable(@NotNull String string) {
        return string.isEmpty() ? null : string;
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
