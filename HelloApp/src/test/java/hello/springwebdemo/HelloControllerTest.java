package hello.springwebdemo;

import hello.BaseTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.Assert.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

public class HelloControllerTest extends BaseTest {
    private static final Logger log = LogManager.getLogger();

    MockMvc mockMvc;

    @Autowired
    HelloController helloController;

    @Before
    public void setUp() throws Exception {
        mockMvc = mockMvcFor(helloController);
    }

    @Test
    public void testHandle() throws Exception {


        HelloController.Data body  = mapper.readValue(mockMvc.perform(post("/hello").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(new HelloController.Data("sharath", "1750 sutter st")))).andReturn().getResponse().getContentAsString(), HelloController.Data.class);
        assertEquals("sharath", body.name);
        assertEquals("1750 sutter st", body.address);


    }
}