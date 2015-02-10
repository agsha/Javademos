package hello.embeddedjettydemo;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.eclipse.jetty.server.Server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import static org.junit.Assert.*;

/**
 * Created by sgururaj on 2/8/15.
 */
public class EmbeddedJettyTest {
    public static void main(String[] args) throws Exception {
        EmbeddedJettyTest test = new EmbeddedJettyTest();
        test.start();
    }

    private void start() throws Exception {
        Injector in = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {}
        });
        EmbeddedJetty.P embeddedJettyP = in.getInstance(EmbeddedJetty.P.class);
        Server server = embeddedJettyP.get(8080, ImmutableList.of(new EmbeddedJetty.ServletInfo(new HelloServlet(), "/hello")));
        server.start();
        Content content = Request.Get("http://localhost:8080/hello?query=helloworld")
                .execute().returnContent();
        assertEquals("helloworld", content.asString());
    }

    public static class HelloServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().write(req.getParameter("query"));
        }
    }
}
