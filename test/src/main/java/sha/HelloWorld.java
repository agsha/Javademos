package sha;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HelloWorld extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        String content = IOUtils.toString(request.getReader());
        response.getWriter().println(""+content.length());
        baseRequest.setHandled(true);
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(8000);
        server.setHandler(new HelloWorld());
        server.start();
        server.join();
    }
}
