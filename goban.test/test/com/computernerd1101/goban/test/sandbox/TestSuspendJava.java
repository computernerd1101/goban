package com.computernerd1101.goban.test.sandbox;

import kotlin.coroutines.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestSuspendJava implements Continuation<Integer> {

    public static void main(String[] args) {
        TestSuspendJava completion = new TestSuspendJava();
        System.out.println(foobar(1, 2, completion));
        TestSuspendKt.resumeSuspendIfTrue("TestSuspendJava");
    }

    public static @Nullable Object foobar(int foo, int bar, @NotNull Continuation<? super Integer> completion) {
        return TestSuspendKt.suspendFoobar(foo, bar, completion);
    }

    @Override
    public void resumeWith(@NotNull Object o) {
        System.out.println(o);
    }

    @NotNull
    @Override
    public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
    }
}
