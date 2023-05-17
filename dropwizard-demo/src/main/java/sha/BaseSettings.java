package sha;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;

public abstract class BaseSettings {
    @Option(name = "--settings", usage = "path of settings file")
    public String settings = "";

    public BaseSettings() throws CmdLineException {
        this(new String[]{});
    }

    public BaseSettings(String[] args) throws CmdLineException {
        CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(args);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        ObjectReader objectReader = mapper.readerForUpdating(this);
        try {
            objectReader.readValue(new File(this.settings));
        } catch (IOException e) {
            return;
        }
        parser = new CmdLineParser(this);
        parser.parseArgument(args);
    }
}
