package hello;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hello.gen.*;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import java.util.*;

/**
 * Hello world!
 *
 */
public class App
{
    private static Logger log = LogManager.getLogger();
    public static void main( String[] args ) throws JsonProcessingException {
        App app = new App();
        app.start();
    }

    private void start() throws JsonProcessingException {
        ConfigurationService cfg = new ConfigurationServiceService().getConfigurationServicePort();
        putHeaders((BindingProvider)cfg);

        log.debug(tojson(cfg.getAllRoles()));



        DefectService defectService = new DefectServiceService().getDefectServicePort();
        putHeaders((BindingProvider)defectService);

    }


    private void putHeaders(BindingProvider p) {
        p.getBinding().setHandlerChain(new ArrayList<Handler>(
                        Arrays.asList(new ClientAuthenticationHandlerWSS("xwss-config-client.xml"))));

    }

    private String tojson(Object o) {
        String res = "";
        ObjectMapper map = new ObjectMapper();
        try {
            res = (map.writerWithDefaultPrettyPrinter().writeValueAsString(o));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    private String deepToString(Object o) {
        ReflectionToStringBuilder builder = new ReflectionToStringBuilder(o, new RecursiveToStringStyle());
        return builder.toString();
    }

    static class My extends RecursiveToStringStyle {

        @Override
        public  boolean isArrayContentDetail() {
            return super.isArrayContentDetail();    // NOCOMMIT
        }

        @Override
        public  void setArrayContentDetail(boolean arrayContentDetail) {
            super.setArrayContentDetail(arrayContentDetail);    // NOCOMMIT
        }

        @Override
        public void setDefaultFullDetail(boolean defaultFullDetail) {
            super.setDefaultFullDetail(defaultFullDetail);    // NOCOMMIT
        }

        @Override
        protected boolean isDefaultFullDetail() {
            return super.isDefaultFullDetail();    // NOCOMMIT
        }
    }
}
