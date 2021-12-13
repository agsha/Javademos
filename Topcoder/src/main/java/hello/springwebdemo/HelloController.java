package hello.springwebdemo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * @author sgururaj
 */
@RestController
public class HelloController {
    private static final Logger log = LogManager.getLogger();
    @RequestMapping(value = "/hello", method = RequestMethod.POST)
    public Data handle(@RequestBody Data body) throws IOException {
        log.error("3333");
        return body;
    }
    public static class Data {
                public String name;
                public String address;
                Data(String name, String address) {
                    this.name = name;
                    this.address = address;
                }
        Data() {

        }
            }
}
