package sha;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ClientMain {
    private static final Logger log = LogManager.getLogger();
    private static final long runid = System.currentTimeMillis();
    private final Settings settings;

    @Inject
    public ClientMain(Settings settings) {
        this.settings = settings;
    }

    public static void main(String[] args) {

        try {
            Injector injector = Guice.createInjector(new GuiceModule(new Settings()));
            injector.getInstance(ClientMain.class).run(args);
        } catch (Throwable t) {
            log.error(t);
        }
    }

    private void run(String[] args) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            ClassicHttpRequest httpGet = ClassicRequestBuilder.get("http://localhost:8080/hello-world?name=sharath")
                    .build();
            httpclient.execute(httpGet, response -> {
                System.out.println(response.getCode() + " " + response.getReasonPhrase());
                final HttpEntity entity1 = response.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String s = IOUtils.toString(entity1.getContent(), StandardCharsets.UTF_8);
                log.info("message is {}", s);
                EntityUtils.consume(entity1);
                return null;
            });

        } catch (IOException e) {
            log.error("", e);
        }
    }
}
