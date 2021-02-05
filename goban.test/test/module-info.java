
@SuppressWarnings("JavaModuleNaming")
module computernerd1101.goban.test {

    exports com.computernerd1101.goban.test;
    exports com.computernerd1101.goban.players.test;

    requires computernerd1101.goban;
    requires computernerd1101.goban.desktop;
    requires kotlin.stdlib;
    requires kotlin.reflect;
    requires kotlinx.coroutines.core;

    requires junit;

}