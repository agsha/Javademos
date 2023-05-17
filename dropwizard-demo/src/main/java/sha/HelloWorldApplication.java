package sha;


import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HelloWorldApplication extends Application<HelloWorldConfiguration> {
    private static final Logger log = LogManager.getLogger();
    private final HelloWorldResource resource;
    private final TemplateHealthCheck healthCheck;

    public HelloWorldApplication(HelloWorldResource resource, TemplateHealthCheck healthCheck) {
        this.resource = resource;
        this.healthCheck = healthCheck;
    }

    public static void main(String[] args) {

        try {
            Injector injector = Guice.createInjector(new GuiceModule(new Settings(args)));
            Settings settings = injector.getInstance(Settings.class);
            String[] serverArgs = settings.serverArgs;
            serverArgs[1] = settings.dw;
            var app = new HelloWorldApplication(injector.getInstance(HelloWorldResource.class), injector.getInstance(TemplateHealthCheck.class));
            app.run(settings.serverArgs);
        } catch (Throwable t) {
            log.error("", t);
        }
    }

    @Override
    public String getName() {
        return "hello-world";
    }

    @Override
    public void initialize(Bootstrap<HelloWorldConfiguration> bootstrap) {
        // nothing to do yet
    }

    @Override
    protected void bootstrapLogging() {
    }

    @Override
    public void run(HelloWorldConfiguration configuration,
                    Environment environment) {
        environment.healthChecks().register("template", healthCheck);
        environment.jersey().register(resource);
    }
}