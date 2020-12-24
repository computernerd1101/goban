import com.computernerd1101.goban.time.*;

@SuppressWarnings("JavaModuleNaming")
module computernerd1101.goban {

    exports com.computernerd1101.goban;
    exports com.computernerd1101.goban.time;
    exports com.computernerd1101.goban.sgf;
    exports com.computernerd1101.goban.markup;
    exports com.computernerd1101.goban.annotations;
    exports com.computernerd1101.sgf;

    requires kotlin.stdlib;
    requires kotlin.reflect;

    requires junit;
    //requires org.jetbrains.annotations;

    uses Overtime;

    provides Overtime with ByoYomi, CanadianOvertime;

}