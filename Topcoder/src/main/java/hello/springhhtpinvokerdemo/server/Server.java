package hello.springhhtpinvokerdemo.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import hello.embeddedjettydemo.EmbeddedJetty;
import hello.springhhtpinvokerdemo.shared.MyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by sgururaj on 1/26/15.
 */

public class Server {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.start();
    }

    private void start() throws Exception {
        Injector in = Guice.createInjector(new ServerModule());
        EmbeddedJetty jetty = new EmbeddedJetty.P().get(8080, "/remoting/MyService", new RmiHttpServlet(in.getInstance(HttpInvokerServiceExporter.class)));
        jetty.start();
    }
}

class RmiHttpServlet extends HttpServlet {
    private HttpInvokerServiceExporter e;

    public RmiHttpServlet(HttpInvokerServiceExporter e) {
        this.e = e;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        e.handleRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        e.handleRequest(req, resp);
    }
}
class ServerModule extends AbstractModule {
    @Override
    protected void configure() {

    }

    @Provides
    HttpInvokerServiceExporter getHttpInvokerServiceExporter() {
        HttpInvokerServiceExporter invoker = new HttpInvokerServiceExporter();
        invoker.setService(new MyServiceImpl());
        invoker.setServiceInterface(MyService.class);
        invoker.afterPropertiesSet();
        return invoker;
    }
}

class MyServiceImpl implements MyService {
    private static final Logger log = LogManager.getLogger();

    @Override
    public int echo(int num) {
        log.debug("RMI request at server: {}", num);
        return num;
    }
}