@SuppressWarnings("JavaModuleNaming")
module computernerd1101.goban.desktop {

    exports com.computernerd1101.goban.desktop;

    requires java.desktop;
    requires computernerd1101.goban;

    requires kotlin.stdlib;
    requires kotlin.reflect;
    requires kotlinx.coroutines.core;
    requires kotlinx.coroutines.swing;
    requires annotations;

}