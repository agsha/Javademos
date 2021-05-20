package sha;

import org.junit.jupiter.api.Test;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {
    @Test
    public void testHello() {
        STGroup stg = new STGroupFile("hello.stg");
        String actual = stg.getInstanceOf("hello").add("name", "Sharath!").render();
        assertEquals("Hello, Sharath!", actual);
    }
}