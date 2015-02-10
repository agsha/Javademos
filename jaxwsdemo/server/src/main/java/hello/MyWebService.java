package hello;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

/**
 * Hello world!
 *
 */
@WebService
public class MyWebService
{
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) {
        Endpoint.publish("http://localhost:8080/myWebService", new MyWebService());
    }
    @WebMethod
    public int echo(int num) {
        log.debug("server: {}", num);
        return num;
    }
}
