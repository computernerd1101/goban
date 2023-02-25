package com.computernerd1101.goban.internal;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

@SuppressWarnings({"unchecked", "rawtypes", "unused"})
public class AtomicArray52<E> {

    public static final AtomicIntegerFieldUpdater<? super AtomicArray52<?>> SIZE =
            AtomicIntegerFieldUpdater.newUpdater(AtomicArray52.class, "size");

    private static final AtomicReferenceFieldUpdater[] UPDATE = new AtomicReferenceFieldUpdater[52];

    static {
        char[] buffer = new char[3];
        buffer[0] = 'e';
        for(int index = 0; index < 52; index++) {
            int n;
            if (index < 10) {
                buffer[1] = (char)('0' + index);
                n = 2;
            } else {
                buffer[1] = (char)('0' + index / 10);
                buffer[2] = (char)('0' + index % 10);
                n = 3;
            }
            UPDATE[index] = AtomicReferenceFieldUpdater.newUpdater(
                    AtomicArray52.class, Object.class, new String(buffer, 0, n).intern());
        }
    }

    @SuppressWarnings("unchecked")
    public static <E> AtomicReferenceFieldUpdater<AtomicArray52<E>, E> update(int index) {
        return (AtomicReferenceFieldUpdater<AtomicArray52<E>, E>)UPDATE[index];
    }

    @Nullable
    public final E get(int index) { return (E)UPDATE[index].get(this); }
    public final void set(int index, @Nullable E e) { UPDATE[index].set(this, e); }

    public volatile int size = 0;

    @Nullable public volatile E e0 = null;
    @Nullable public volatile E e1 = null;
    @Nullable public volatile E e2 = null;
    @Nullable public volatile E e3 = null;
    @Nullable public volatile E e4 = null;
    @Nullable public volatile E e5 = null;
    @Nullable public volatile E e6 = null;
    @Nullable public volatile E e7 = null;
    @Nullable public volatile E e8 = null;
    @Nullable public volatile E e9 = null;
    @Nullable public volatile E e10 = null;
    @Nullable public volatile E e11 = null;
    @Nullable public volatile E e12 = null;
    @Nullable public volatile E e13 = null;
    @Nullable public volatile E e14 = null;
    @Nullable public volatile E e15 = null;
    @Nullable public volatile E e16 = null;
    @Nullable public volatile E e17 = null;
    @Nullable public volatile E e18 = null;
    @Nullable public volatile E e19 = null;
    @Nullable public volatile E e20 = null;
    @Nullable public volatile E e21 = null;
    @Nullable public volatile E e22 = null;
    @Nullable public volatile E e23 = null;
    @Nullable public volatile E e24 = null;
    @Nullable public volatile E e25 = null;
    @Nullable public volatile E e26 = null;
    @Nullable public volatile E e27 = null;
    @Nullable public volatile E e28 = null;
    @Nullable public volatile E e29 = null;
    @Nullable public volatile E e30 = null;
    @Nullable public volatile E e31 = null;
    @Nullable public volatile E e32 = null;
    @Nullable public volatile E e33 = null;
    @Nullable public volatile E e34 = null;
    @Nullable public volatile E e35 = null;
    @Nullable public volatile E e36 = null;
    @Nullable public volatile E e37 = null;
    @Nullable public volatile E e38 = null;
    @Nullable public volatile E e39 = null;
    @Nullable public volatile E e40 = null;
    @Nullable public volatile E e41 = null;
    @Nullable public volatile E e42 = null;
    @Nullable public volatile E e43 = null;
    @Nullable public volatile E e44 = null;
    @Nullable public volatile E e45 = null;
    @Nullable public volatile E e46 = null;
    @Nullable public volatile E e47 = null;
    @Nullable public volatile E e48 = null;
    @Nullable public volatile E e49 = null;
    @Nullable public volatile E e50 = null;
    @Nullable public volatile E e51 = null;
    @Nullable public volatile E e52 = null;

}
