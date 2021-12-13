package hello.springhhtpinvokerdemo.client;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import hello.springhhtpinvokerdemo.shared.MyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import static org.junit.Assert.*;

public class Client {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) {
        Client c = new Client();
        c.start();
    }

    private void start() {
        Injector in = Guice.createInjector(new ClientModule());
        MyService service = in.getInstance(MyService.class);
        assertEquals(Integer.MAX_VALUE- 40, service.echo(Integer.MAX_VALUE-40));
    }

}

class ClientModule extends AbstractModule {
    private static final Logger log = LogManager.getLogger();

    @Provides
    MyService getHttpInvokerProxyFactoryBean() {
        HttpInvokerProxyFactoryBean o = new HttpInvokerProxyFactoryBean();
        o.setServiceUrl("http://localhost:8080/remoting/MyService");
        o.setServiceInterface(MyService.class);
        o.afterPropertiesSet();
        return (MyService)o.getObject();
    }

    @Override
    protected void configure() {

    }
}
