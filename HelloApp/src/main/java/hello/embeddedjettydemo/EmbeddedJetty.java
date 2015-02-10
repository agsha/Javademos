package hello.embeddedjettydemo;

import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.http.HttpServlet;
import java.util.List;

/**
 * Created by sgururaj on 2/8/15.
 */
public class EmbeddedJetty extends Server{
    private static final Logger log = LogManager.getLogger();

    private EmbeddedJetty(int port) {
        super(port);
    }

    @Singleton
    public static class P {
        private static final Logger log = LogManager.getLogger();

        public EmbeddedJetty get(int port, List<ServletInfo> servletInfo) {
            return get(port, "/", servletInfo);
        }

        public EmbeddedJetty get(int port, String context, List<ServletInfo> servletInfo) {
            EmbeddedJetty server = new EmbeddedJetty(port);

            ServletContextHandler context0 = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context0.setContextPath(context);
            for (ServletInfo info : servletInfo) {
                context0.addServlet(new ServletHolder(info.servlet),info.urlPattern);
            }

            ContextHandlerCollection contexts = new ContextHandlerCollection();
            contexts.setHandlers(new Handler[] { context0 });

            server.setHandler(contexts);
            return server;

        }

        public EmbeddedJetty get(int port, String urlPattern, HttpServlet servlet) {
            return get(port, ImmutableList.of(new ServletInfo(servlet, urlPattern)));
        }
    }

    public static class ServletInfo {
        public HttpServlet servlet;
        public String urlPattern;
        public ServletInfo(HttpServlet servlet, String urlPattern) {
            this.servlet = servlet;
            this.urlPattern = urlPattern;
        }
    }
}
