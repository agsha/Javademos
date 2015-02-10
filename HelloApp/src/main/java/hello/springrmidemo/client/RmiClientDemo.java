package hello.springrmidemo.client;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import hello.springrmidemo.shared.AccountService;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import static org.junit.Assert.*;

/**
 * Created by sgururaj on 2/8/15.
 */
public class RmiClientDemo {
    public static void main(String[] args) {
        RmiClientDemo demo = new RmiClientDemo();
        demo.start();
    }

    public void start() {
        Injector in = Guice.createInjector(new ClientModule());
        AccountService service = (AccountService)in.getInstance(RmiProxyFactoryBean.class).getObject();
        assertEquals(Integer.MAX_VALUE - 100, service.echo(Integer.MAX_VALUE - 100));

    }
}


class ClientModule extends AbstractModule {

    @Override
    protected void configure() {

    }
    @Provides
    RmiProxyFactoryBean getRmiProxyFactoryBean() {
        RmiProxyFactoryBean pfb = new RmiProxyFactoryBean();
        pfb.setServiceUrl("rmi://localhost:1199/AccountService");
        pfb.setServiceInterface(AccountService.class);
        pfb.afterPropertiesSet();
        return pfb;
    }
}
