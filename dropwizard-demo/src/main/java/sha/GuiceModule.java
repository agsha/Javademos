package sha;

import com.google.inject.AbstractModule;

public class GuiceModule extends AbstractModule {


    private final Settings settings;

    public GuiceModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        super.configure();
        bind(Settings.class).toInstance(settings);
    }
}
