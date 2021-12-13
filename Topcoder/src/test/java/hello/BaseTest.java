package hello;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author sgururaj
 */
@WebAppConfiguration
@ContextConfiguration(locations = {"classpath:helloapp-servlet.xml"})
public abstract class BaseTest extends AbstractJUnit4SpringContextTests {

    protected ObjectMapper mapper = new ObjectMapper();
    protected MockMvc mockMvcFor(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .defaultRequest(get("/")
                        .accept(MediaType.APPLICATION_JSON))
                        .alwaysExpect(status().isOk())
                        .build();
    }
}
