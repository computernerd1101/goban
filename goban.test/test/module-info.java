
@SuppressWarnings("JavaModuleNaming")
module computernerd1101.goban.test {

    exports com.computernerd1101.goban.test;
    exports com.computernerd1101.goban.test.desktop;
    exports com.computernerd1101.goban.test.players;
    exports com.computernerd1101.goban.test.sandbox;

    requires computernerd1101.goban;
    requires computernerd1101.goban.desktop;
    requires java.desktop;
    requires kotlin.stdlib;
    requires kotlin.reflect;
    requires kotlinx.coroutines.core.jvm;
    requires kotlinx.coroutines.swing;

    requires junit;

    requires annotations;
    requires kotlin.test;


    uses com.computernerd1101.goban.test.sandbox.MyServiceLoader;

}