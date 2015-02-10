package hello.springrmidemo.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import hello.springrmidemo.shared.AccountService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.remoting.rmi.RmiServiceExporter;

import java.rmi.RemoteException;

/**
* Created by sgururaj on 2/8/15.
*/

public class RmiServerDemo {
    public static void main(String[] args) {
        RmiServerDemo demo = new RmiServerDemo();
        demo.start();
    }

    private void start() {
        Injector in = Guice.createInjector(new ServerModule());
        // this line starts the rmi server and never terminates.
        in.getInstance(RmiServiceExporter.class);
    }
}

class AccountServiceImpl implements AccountService {
    private static final Logger log = LogManager.getLogger();
    public int echo (int num) {
        log.debug("echo: {}", num);
        return num;

    }
}


class ServerModule extends AbstractModule {

    @Override
    protected void configure() {

    }
    @Provides
    RmiServiceExporter getRmiServiceExporter() throws RemoteException {
        RmiServiceExporter exporter = new RmiServiceExporter();
        exporter.setServiceName("AccountService");
        exporter.setService(new AccountServiceImpl());
        exporter.setServiceInterface(AccountService.class);
        exporter.setRegistryPort(1199);
        exporter.afterPropertiesSet();
        return exporter;
    }
}