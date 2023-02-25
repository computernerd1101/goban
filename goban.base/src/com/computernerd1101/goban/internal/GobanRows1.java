package com.computernerd1101.goban.internal;

import org.jetbrains.annotations.NotNull;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

public class GobanRows1 {
    @NotNull public static GobanRows1 empty(int index) { return GobanRows104.EMPTY[index]; }
    @NotNull public static AtomicLongFieldUpdater<GobanRows1> getRow(int index) { return GobanRows104.ROWS[index]; }
    GobanRows1() { }
    @NotNull public GobanRows1 newInstance() { return new GobanRows1(); }
    public int getSize() { return 1; }
    @SuppressWarnings("unused") public volatile long row0 = 0L;
    public final long get(int index) { return getRow(index).get(this); }
    public final void set(int index, long value) { getRow(index).set(this, value); }
}
class GobanRows2 extends GobanRows1 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows2(); }
    @Override public int getSize() { return 2; }
    @SuppressWarnings("unused") public volatile long row1 = 0L;
}
class GobanRows3 extends GobanRows2 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows3(); }
    @Override public int getSize() { return 3; }
    @SuppressWarnings("unused") public volatile long row2 = 0L;
}
class GobanRows4 extends GobanRows3 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows4(); }
    @Override public int getSize() { return 4; }
    @SuppressWarnings("unused") public volatile long row3 = 0L;
}
class GobanRows5 extends GobanRows4 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows5(); }
    @Override public int getSize() { return 5; }
    @SuppressWarnings("unused") public volatile long row4 = 0L;
}
class GobanRows6 extends GobanRows5 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows6(); }
    @Override public int getSize() { return 6; }
    @SuppressWarnings("unused") public volatile long row5 = 0L;
}
class GobanRows7 extends GobanRows6 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows7(); }
    @Override public int getSize() { return 7; }
    @SuppressWarnings("unused") public volatile long row6 = 0L;
}
class GobanRows8 extends GobanRows7 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows8(); }
    @Override public int getSize() { return 8; }
    @SuppressWarnings("unused") public volatile long row7 = 0L;
}
class GobanRows9 extends GobanRows8 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows9(); }
    @Override public int getSize() { return 9; }
    @SuppressWarnings("unused") public volatile long row8 = 0L;
}
class GobanRows10 extends GobanRows9 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows10(); }
    @Override public int getSize() { return 10; }
    @SuppressWarnings("unused") public volatile long row9 = 0L;
}
class GobanRows11 extends GobanRows10 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows11(); }
    @Override public int getSize() { return 11; }
    @SuppressWarnings("unused") public volatile long row10 = 0L;
}
class GobanRows12 extends GobanRows11 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows12(); }
    @Override public int getSize() { return 12; }
    @SuppressWarnings("unused") public volatile long row11 = 0L;
}
class GobanRows13 extends GobanRows12 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows13(); }
    @Override public int getSize() { return 13; }
    @SuppressWarnings("unused") public volatile long row12 = 0L;
}
class GobanRows14 extends GobanRows13 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows14(); }
    @Override public int getSize() { return 14; }
    @SuppressWarnings("unused") public volatile long row13 = 0L;
}
class GobanRows15 extends GobanRows14 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows15(); }
    @Override public int getSize() { return 15; }
    @SuppressWarnings("unused") public volatile long row14 = 0L;
}
class GobanRows16 extends GobanRows15 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows16(); }
    @Override public int getSize() { return 16; }
    @SuppressWarnings("unused") public volatile long row15 = 0L;
}
class GobanRows17 extends GobanRows16 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows17(); }
    @Override public int getSize() { return 17; }
    @SuppressWarnings("unused") public volatile long row16 = 0L;
}
class GobanRows18 extends GobanRows17 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows18(); }
    @Override public int getSize() { return 18; }
    @SuppressWarnings("unused") public volatile long row17 = 0L;
}
class GobanRows19 extends GobanRows18 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows19(); }
    @Override public int getSize() { return 19; }
    @SuppressWarnings("unused") public volatile long row18 = 0L;
}
class GobanRows20 extends GobanRows19 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows20(); }
    @Override public int getSize() { return 20; }
    @SuppressWarnings("unused") public volatile long row19 = 0L;
}
class GobanRows21 extends GobanRows20 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows21(); }
    @Override public int getSize() { return 21; }
    @SuppressWarnings("unused") public volatile long row20 = 0L;
}
class GobanRows22 extends GobanRows21 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows22(); }
    @Override public int getSize() { return 22; }
    @SuppressWarnings("unused") public volatile long row21 = 0L;
}
class GobanRows23 extends GobanRows22 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows23(); }
    @Override public int getSize() { return 23; }
    @SuppressWarnings("unused") public volatile long row22 = 0L;
}
class GobanRows24 extends GobanRows23 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows24(); }
    @Override public int getSize() { return 24; }
    @SuppressWarnings("unused") public volatile long row23 = 0L;
}
class GobanRows25 extends GobanRows24 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows25(); }
    @Override public int getSize() { return 25; }
    @SuppressWarnings("unused") public volatile long row24 = 0L;
}
class GobanRows26 extends GobanRows25 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows26(); }
    @Override public int getSize() { return 26; }
    @SuppressWarnings("unused") public volatile long row25 = 0L;
}
class GobanRows27 extends GobanRows26 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows27(); }
    @Override public int getSize() { return 27; }
    @SuppressWarnings("unused") public volatile long row26 = 0L;
}
class GobanRows28 extends GobanRows27 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows28(); }
    @Override public int getSize() { return 28; }
    @SuppressWarnings("unused") public volatile long row27 = 0L;
}
class GobanRows29 extends GobanRows28 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows29(); }
    @Override public int getSize() { return 29; }
    @SuppressWarnings("unused") public volatile long row28 = 0L;
}
class GobanRows30 extends GobanRows29 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows30(); }
    @Override public int getSize() { return 30; }
    @SuppressWarnings("unused") public volatile long row29 = 0L;
}
class GobanRows31 extends GobanRows30 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows31(); }
    @Override public int getSize() { return 31; }
    @SuppressWarnings("unused") public volatile long row30 = 0L;
}
class GobanRows32 extends GobanRows31 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows32(); }
    @Override public int getSize() { return 32; }
    @SuppressWarnings("unused") public volatile long row31 = 0L;
}
class GobanRows33 extends GobanRows32 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows33(); }
    @Override public int getSize() { return 33; }
    @SuppressWarnings("unused") public volatile long row32 = 0L;
}
class GobanRows34 extends GobanRows33 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows34(); }
    @Override public int getSize() { return 34; }
    @SuppressWarnings("unused") public volatile long row33 = 0L;
}
class GobanRows35 extends GobanRows34 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows35(); }
    @Override public int getSize() { return 35; }
    @SuppressWarnings("unused") public volatile long row34 = 0L;
}
class GobanRows36 extends GobanRows35 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows36(); }
    @Override public int getSize() { return 36; }
    @SuppressWarnings("unused") public volatile long row35 = 0L;
}
class GobanRows37 extends GobanRows36 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows37(); }
    @Override public int getSize() { return 37; }
    @SuppressWarnings("unused") public volatile long row36 = 0L;
}
class GobanRows38 extends GobanRows37 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows38(); }
    @Override public int getSize() { return 38; }
    @SuppressWarnings("unused") public volatile long row37 = 0L;
}
class GobanRows39 extends GobanRows38 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows39(); }
    @Override public int getSize() { return 39; }
    @SuppressWarnings("unused") public volatile long row38 = 0L;
}
class GobanRows40 extends GobanRows39 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows40(); }
    @Override public int getSize() { return 40; }
    @SuppressWarnings("unused") public volatile long row39 = 0L;
}
class GobanRows41 extends GobanRows40 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows41(); }
    @Override public int getSize() { return 41; }
    @SuppressWarnings("unused") public volatile long row40 = 0L;
}
class GobanRows42 extends GobanRows41 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows42(); }
    @Override public int getSize() { return 42; }
    @SuppressWarnings("unused") public volatile long row41 = 0L;
}
class GobanRows43 extends GobanRows42 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows43(); }
    @Override public int getSize() { return 43; }
    @SuppressWarnings("unused") public volatile long row42 = 0L;
}
class GobanRows44 extends GobanRows43 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows44(); }
    @Override public int getSize() { return 44; }
    @SuppressWarnings("unused") public volatile long row43 = 0L;
}
class GobanRows45 extends GobanRows44 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows45(); }
    @Override public int getSize() { return 45; }
    @SuppressWarnings("unused") public volatile long row44 = 0L;
}
class GobanRows46 extends GobanRows45 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows46(); }
    @Override public int getSize() { return 46; }
    @SuppressWarnings("unused") public volatile long row45 = 0L;
}
class GobanRows47 extends GobanRows46 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows47(); }
    @Override public int getSize() { return 47; }
    @SuppressWarnings("unused") public volatile long row46 = 0L;
}
class GobanRows48 extends GobanRows47 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows48(); }
    @Override public int getSize() { return 48; }
    @SuppressWarnings("unused") public volatile long row47 = 0L;
}
class GobanRows49 extends GobanRows48 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows49(); }
    @Override public int getSize() { return 49; }
    @SuppressWarnings("unused") public volatile long row48 = 0L;
}
class GobanRows50 extends GobanRows49 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows50(); }
    @Override public int getSize() { return 50; }
    @SuppressWarnings("unused") public volatile long row49 = 0L;
}
class GobanRows51 extends GobanRows50 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows51(); }
    @Override public int getSize() { return 51; }
    @SuppressWarnings("unused") public volatile long row50 = 0L;
}
class GobanRows52 extends GobanRows51 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows52(); }
    @Override public int getSize() { return 52; }
    @SuppressWarnings("unused") public volatile long row51 = 0L;
}
class GobanRows54 extends GobanRows52 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows54(); }
    @Override public int getSize() { return 54; }
    @SuppressWarnings("unused") public volatile long row52 = 0L;
    @SuppressWarnings("unused") public volatile long row53= 0L;
}
class GobanRows56 extends GobanRows54 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows56(); }
    @Override public int getSize() { return 56; }
    @SuppressWarnings("unused") public volatile long row54 = 0L;
    @SuppressWarnings("unused") public volatile long row55= 0L;
}
class GobanRows58 extends GobanRows56 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows58(); }
    @Override public int getSize() { return 58; }
    @SuppressWarnings("unused") public volatile long row56 = 0L;
    @SuppressWarnings("unused") public volatile long row57= 0L;
}
class GobanRows60 extends GobanRows58 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows60(); }
    @Override public int getSize() { return 60; }
    @SuppressWarnings("unused") public volatile long row58 = 0L;
    @SuppressWarnings("unused") public volatile long row59= 0L;
}
class GobanRows62 extends GobanRows60 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows62(); }
    @Override public int getSize() { return 62; }
    @SuppressWarnings("unused") public volatile long row60 = 0L;
    @SuppressWarnings("unused") public volatile long row61= 0L;
}
class GobanRows64 extends GobanRows62 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows64(); }
    @Override public int getSize() { return 64; }
    @SuppressWarnings("unused") public volatile long row62 = 0L;
    @SuppressWarnings("unused") public volatile long row63= 0L;
}
class GobanRows66 extends GobanRows64 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows66(); }
    @Override public int getSize() { return 66; }
    @SuppressWarnings("unused") public volatile long row64 = 0L;
    @SuppressWarnings("unused") public volatile long row65= 0L;
}
class GobanRows68 extends GobanRows66 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows68(); }
    @Override public int getSize() { return 68; }
    @SuppressWarnings("unused") public volatile long row66 = 0L;
    @SuppressWarnings("unused") public volatile long row67= 0L;
}
class GobanRows70 extends GobanRows68 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows70(); }
    @Override public int getSize() { return 70; }
    @SuppressWarnings("unused") public volatile long row68 = 0L;
    @SuppressWarnings("unused") public volatile long row69= 0L;
}
class GobanRows72 extends GobanRows70 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows72(); }
    @Override public int getSize() { return 72; }
    @SuppressWarnings("unused") public volatile long row70 = 0L;
    @SuppressWarnings("unused") public volatile long row71= 0L;
}
class GobanRows74 extends GobanRows72 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows74(); }
    @Override public int getSize() { return 74; }
    @SuppressWarnings("unused") public volatile long row72 = 0L;
    @SuppressWarnings("unused") public volatile long row73= 0L;
}
class GobanRows76 extends GobanRows74 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows76(); }
    @Override public int getSize() { return 76; }
    @SuppressWarnings("unused") public volatile long row74 = 0L;
    @SuppressWarnings("unused") public volatile long row75= 0L;
}
class GobanRows78 extends GobanRows76 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows78(); }
    @Override public int getSize() { return 78; }
    @SuppressWarnings("unused") public volatile long row76 = 0L;
    @SuppressWarnings("unused") public volatile long row77= 0L;
}
class GobanRows80 extends GobanRows78 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows80(); }
    @Override public int getSize() { return 80; }
    @SuppressWarnings("unused") public volatile long row78 = 0L;
    @SuppressWarnings("unused") public volatile long row79= 0L;
}
class GobanRows82 extends GobanRows80 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows82(); }
    @Override public int getSize() { return 82; }
    @SuppressWarnings("unused") public volatile long row80 = 0L;
    @SuppressWarnings("unused") public volatile long row81= 0L;
}
class GobanRows84 extends GobanRows82 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows84(); }
    @Override public int getSize() { return 84; }
    @SuppressWarnings("unused") public volatile long row82 = 0L;
    @SuppressWarnings("unused") public volatile long row83= 0L;
}
class GobanRows86 extends GobanRows84 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows86(); }
    @Override public int getSize() { return 86; }
    @SuppressWarnings("unused") public volatile long row84 = 0L;
    @SuppressWarnings("unused") public volatile long row85= 0L;
}
class GobanRows88 extends GobanRows86 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows88(); }
    @Override public int getSize() { return 88; }
    @SuppressWarnings("unused") public volatile long row86 = 0L;
    @SuppressWarnings("unused") public volatile long row87= 0L;
}
class GobanRows90 extends GobanRows88 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows90(); }
    @Override public int getSize() { return 90; }
    @SuppressWarnings("unused") public volatile long row88 = 0L;
    @SuppressWarnings("unused") public volatile long row89= 0L;
}
class GobanRows92 extends GobanRows90 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows92(); }
    @Override public int getSize() { return 92; }
    @SuppressWarnings("unused") public volatile long row90 = 0L;
    @SuppressWarnings("unused") public volatile long row91= 0L;
}
class GobanRows94 extends GobanRows92 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows94(); }
    @Override public int getSize() { return 94; }
    @SuppressWarnings("unused") public volatile long row92 = 0L;
    @SuppressWarnings("unused") public volatile long row93= 0L;
}
class GobanRows96 extends GobanRows94 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows96(); }
    @Override public int getSize() { return 96; }
    @SuppressWarnings("unused") public volatile long row94 = 0L;
    @SuppressWarnings("unused") public volatile long row95= 0L;
}
class GobanRows98 extends GobanRows96 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows98(); }
    @Override public int getSize() { return 98; }
    @SuppressWarnings("unused") public volatile long row96 = 0L;
    @SuppressWarnings("unused") public volatile long row97= 0L;
}
class GobanRows100 extends GobanRows98 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows100(); }
    @Override public int getSize() { return 100; }
    @SuppressWarnings("unused") public volatile long row98 = 0L;
    @SuppressWarnings("unused") public volatile long row99= 0L;
}
class GobanRows102 extends GobanRows100 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows102(); }
    @Override public int getSize() { return 102; }
    @SuppressWarnings("unused") public volatile long row100 = 0L;
    @SuppressWarnings("unused") public volatile long row101= 0L;
}
@SuppressWarnings("unchecked")
final class GobanRows104 extends GobanRows102 {
    @Override @NotNull public GobanRows1 newInstance() { return new GobanRows104(); }
    @Override public int getSize() { return 104; }
    @SuppressWarnings("unused") public volatile long row102 = 0L;
    @SuppressWarnings("unused") public volatile long row103= 0L;
    static final GobanRows1[] EMPTY = {
        new GobanRows1(), new GobanRows2(),
        new GobanRows3(), new GobanRows4(),
        new GobanRows5(), new GobanRows6(),
        new GobanRows7(), new GobanRows8(),
        new GobanRows9(), new GobanRows10(),
        new GobanRows11(), new GobanRows12(),
        new GobanRows13(), new GobanRows14(),
        new GobanRows15(), new GobanRows16(),
        new GobanRows17(), new GobanRows18(),
        new GobanRows19(), new GobanRows20(),
        new GobanRows21(), new GobanRows22(),
        new GobanRows23(), new GobanRows24(),
        new GobanRows25(), new GobanRows26(),
        new GobanRows27(), new GobanRows28(),
        new GobanRows29(), new GobanRows30(),
        new GobanRows31(), new GobanRows32(),
        new GobanRows33(), new GobanRows34(),
        new GobanRows35(), new GobanRows36(),
        new GobanRows37(), new GobanRows38(),
        new GobanRows39(), new GobanRows40(),
        new GobanRows41(), new GobanRows42(),
        new GobanRows43(), new GobanRows44(),
        new GobanRows45(), new GobanRows46(),
        new GobanRows47(), new GobanRows48(),
        new GobanRows49(), new GobanRows50(),
        new GobanRows51(), new GobanRows52(),
        new GobanRows54(), new GobanRows56(),
        new GobanRows58(), new GobanRows60(),
        new GobanRows62(), new GobanRows64(),
        new GobanRows66(), new GobanRows68(),
        new GobanRows70(), new GobanRows72(),
        new GobanRows74(), new GobanRows76(),
        new GobanRows78(), new GobanRows80(),
        new GobanRows82(), new GobanRows84(),
        new GobanRows86(), new GobanRows88(),
        new GobanRows90(), new GobanRows92(),
        new GobanRows94(), new GobanRows96(),
        new GobanRows98(), new GobanRows100(),
        new GobanRows102(), new GobanRows104()};
    static final AtomicLongFieldUpdater<GobanRows1>[] ROWS;
    static {
        @SuppressWarnings("rawtypes") AtomicLongFieldUpdater[] rows = new AtomicLongFieldUpdater[104];
        ROWS = (AtomicLongFieldUpdater<GobanRows1>[])rows;
        char[] buf = new char[6];
        buf[0] = 'r';
        buf[1] = 'o';
        buf[2] = 'w';
        for(int index = 0; index < 104; index++) {
            int nBuf;
            if (index >= 100) { // index is always < 104
                buf[3] = '1';
                buf[4] = '0';
                // buf[5] = (char)('0' + index - 100)
                //        = (char)(48  + index - 100)
                buf[5] = (char)(index - 52);
                nBuf = 6;
            } else if (index >= 10) {
                buf[3] = (char)('0' + index / 10);
                buf[4] = (char)('0' + index % 10);
                nBuf = 5;
            } else {
                buf[3] = (char)('0' + index);
                nBuf = 4;
            }
            int classIndex = index < 52 ? index : (index / 2 + 26); // (index - 52) / 2 + 52
            rows[index] = AtomicLongFieldUpdater.newUpdater(EMPTY[classIndex].getClass(),
                                                            new String(buf, 0, nBuf).intern());
        }
    }
};
