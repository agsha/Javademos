package hello;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hello.gen.Employee;
import hello.gen.Name;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;

/**
 * Hello world!
 *
 */
public class App
{
    private static Logger log = LogManager.getLogger();
    public static void main( String[] args ) throws JsonProcessingException, JAXBException, URISyntaxException {
        App app = new App();
        app.start();
    }

    private void start() throws JAXBException, URISyntaxException, JsonProcessingException {
        JAXBContext jaxbContext;
        jaxbContext = JAXBContext.newInstance(new Class[] {Employee.class});

        //Marshall the employee object to xml.
        Employee emp1 = new Employee();
        emp1.setAge("30");
        emp1.setDept("programmer");
        emp1.setId("1");
        Name name = new Name();
        name.setFname("sha");
        name.setLname("raj");
        emp1.setName(name);
        emp1.setProject("test");

        StringWriter writer = new StringWriter();
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(emp1, writer);
        log.debug(writer.toString());

        //Un-Marshall the employee xml to java object.
        Unmarshaller um = jaxbContext.createUnmarshaller();
        Employee emp2 = (Employee) um.unmarshal(new StringReader(writer.toString()));
        log.debug(new ObjectMapper().writeValueAsString(emp2));
    }

}
