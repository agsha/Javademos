package hello;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MyWebServiceTest {
    private static final Logger log = LogManager.getLogger();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testEcho() throws Exception {
        log.debug("working?");
    }
}