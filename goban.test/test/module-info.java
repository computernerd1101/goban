
@SuppressWarnings("JavaModuleNaming")
module computernerd1101.goban.test {

    exports com.computernerd1101.goban.test;
    exports com.computernerd1101.goban.test.players;

    requires computernerd1101.goban;
    requires computernerd1101.goban.desktop;
    requires java.desktop;
    requires kotlin.stdlib;
    requires kotlin.reflect;
    requires kotlinx.coroutines.core;
    requires kotlinx.coroutines.swing;

    requires junit;

    requires org.jetbrains.annotations;

    uses com.computernerd1101.goban.test.sandbox.MyServiceLoader;

}