import com.computernerd1101.goban.time.*;

@SuppressWarnings("JavaModuleNaming")
module computernerd1101.goban {

    exports com.computernerd1101.goban;
    exports com.computernerd1101.goban.time;
    exports com.computernerd1101.goban.sgf;
    exports com.computernerd1101.goban.markup;
    exports com.computernerd1101.goban.properties;
    exports com.computernerd1101.sgf;
    exports com.computernerd1101.goban.players;

    requires kotlin.stdlib;
    requires kotlin.reflect;
    requires kotlinx.coroutines.core;

    requires annotations;

    uses Overtime;

    provides Overtime with ByoYomi, CanadianOvertime;

}