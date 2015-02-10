package hello.guicedemo;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * mvn compile exec:java -Dexec.mainClass="hello.guicedemo.GuiceDemo"
 * @author sgururaj
 */
public class GuiceDemo {
    private static final Logger log = LogManager.getLogger();
    public static void main(String[] args) {
        Injector in = Guice.createInjector(new DemoModule());
        MyInterface myInterface = in.getInstance(MyInterface.class);
        myInterface.sayHello();
    }

    static interface MyInterface {
        public void sayHello();
    }

    static class MyImpl implements MyInterface {
        @Override
        public void sayHello() {
            log.debug("Hello, World!");
        }
    }
    static class DemoModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(MyInterface.class).to(MyImpl.class);
        }
    }
}
