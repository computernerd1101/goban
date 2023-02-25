package com.computernerd1101.goban.poet

import java.io.FileWriter

fun main() {
    val buf = StringBuilder("""
       |package com.computernerd1101.goban.internal;
       |
       |import org.jetbrains.annotations.NotNull;
       |import java.util.concurrent.atomic.AtomicLongFieldUpdater;
       |
       |public class GobanRows1 {
       |    @NotNull public static GobanRows1 empty(int index) { return GobanRows104.EMPTY[index]; }
       |    @NotNull public static AtomicLongFieldUpdater<GobanRows1> getRow(int index) { return GobanRows104.ROWS[index]; }
    """.trimMargin())
    buf.append("""|
       |    GobanRows1() { }
       |    @NotNull public GobanRows1 newInstance() { return new GobanRows1(); }
       |    public int getSize() { return 1; }
       |    @SuppressWarnings("unused") public volatile long row0 = 0L;
       |    public final long get(int index) { return getRow(index).get(this); }
       |    public final void set(int index, long value) { getRow(index).set(this, value); }
    |""".trimMargin())
    for(i in 2..52) {
        // class GobanRows2 extends GobanRows1 {
        //     @Override @NotNull public GobanRows1 newInstance() { return new GobanRows2(); }
        //     @Override public int getSize() { return 2; }
        //     @SuppressWarnings("unused") public volatile long row1 = 0L;
        // }
        // ...
        // class GobanRows52 extends GobanRows51 {
        //     @Override @NotNull public GobanRows1 newInstance() { return new GobanRows52(); }
        //     @Override public int getSize() { return 52; }
        //     @SuppressWarnings("unused") public volatile long row51 = 0L;
        // }
        buf.append("}\nclass GobanRows").append(i).append(" extends GobanRows").append(i-1)
            .append(" {\n    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows").append(i)
            .append("(); }\n    @Override public int getSize() { return ").append(i)
            .append("; }\n    @SuppressWarnings(\"unused\") public volatile long row")
            .append(i-1).append(" = 0L;\n")
    }
    for(i in 54..104 step 2) {
        // class GobanRows54 extends GobanRows52 {
        //     @Override @NotNull public GobanRows1 newInstance() { return new GobanRows54(); }
        //     @Override public int getSize() { return 54; }
        //     @SuppressWarnings("unused") public volatile long row52 = 0L;
        //     @SuppressWarnings("unused") public volatile long row53 = 0L;
        // }
        // ...
        // final class GobanRows104 extends GobanRows102 {
        //     @Override @NotNull public GobanRows1 newInstance() { return new GobanRows104(); }
        //     @Override public int getSize() { return 104; }
        //     @SuppressWarnings("unused") public volatile long row102 = 0L;
        //     @SuppressWarnings("unused") public volatile long row103 = 0L;
        //     ...
        // }
        buf.append("}\n")
        if (i == 104) buf.append("@SuppressWarnings(\"unchecked\")\nfinal ")
        buf.append("class GobanRows")
            .append(i).append(" extends GobanRows").append(i-2)
            .append(" {\n    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows").append(i)
            .append("(); }\n    @Override public int getSize() { return ").append(i)
            .append("; }\n    @SuppressWarnings(\"unused\") public volatile long row")
            .append(i-2).append(" = 0L;\n    @SuppressWarnings(\"unused\") public volatile long row")
            .append(i-1).append("= 0L;\n")
    }

    buf.append("""
       |    static final GobanRows1[] EMPTY = {
       |        new GobanRows1(), new GobanRows2()
    """.trimMargin())
    for(i in 3..51 step 2) {
        // ,\n        new GobanRows3(), new GobanRows4()
        // ...
        // ,\n        new GobanRows51(), new GobanRows52()
        buf.append(",\n        new GobanRows").append(i).append("(), new GobanRows").append(i + 1).append("()")
    }
    for(i in 54..102 step 4) {
        // ,\n        new GobanRows54(), new GobanRows56()
        // ...
        // ,\n        new GobanRows102(), new GobanRows104()
        buf.append(",\n        new GobanRows").append(i).append("(), new GobanRows").append(i + 2).append("()")
    }
    buf.append("""|};
       |    static final AtomicLongFieldUpdater<GobanRows1>[] ROWS;
       |    static {
       |        @SuppressWarnings("rawtypes") AtomicLongFieldUpdater[] rows = new AtomicLongFieldUpdater[104];
       |        ROWS = (AtomicLongFieldUpdater<GobanRows1>[])rows;
       |        char[] buf = new char[6];
       |        buf[0] = 'r';
       |        buf[1] = 'o';
       |        buf[2] = 'w';
       |        for(int index = 0; index < 104; index++) {
       |            int nBuf;
       |            if (index >= 100) { // index is always < 104
       |                buf[3] = '1';
       |                buf[4] = '0';
       |                // buf[5] = (char)('0' + index - 100)
       |                //        = (char)(48  + index - 100)
       |                buf[5] = (char)(index - 52);
       |                nBuf = 6;
       |            } else if (index >= 10) {
       |                buf[3] = (char)('0' + index / 10);
       |                buf[4] = (char)('0' + index % 10);
       |                nBuf = 5;
       |            } else {
       |                buf[3] = (char)('0' + index);
       |                nBuf = 4;
       |            }
       |            int classIndex = index < 52 ? index : (index / 2 + 26); // (index - 52) / 2 + 52
       |            rows[index] = AtomicLongFieldUpdater.newUpdater(EMPTY[classIndex].getClass(),
       |                                                            new String(buf, 0, nBuf).intern());
       |        }
       |    }
       |};
    |""".trimMargin())
    FileWriter("goban.base/src/com/computernerd1101/goban/internal/GobanRows1.java").use { writer ->
        writer.append(buf)
    }
}
