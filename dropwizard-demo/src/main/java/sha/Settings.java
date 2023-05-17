package sha;


import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

public class Settings extends BaseSettings {
    public String[] serverArgs = {"server", "src/main/resources/dw.yaml"};

    @Option(name = "--dw", usage = "path of dropwizard settings yaml")
    public String dw = "src/main/resources/dw.yaml";

    @Option(name = "--ip", usage = "ip address of server")
    public String ip = "localhost";

    @Option(name = "--threads", usage = "number of client threads")
    public int threads = 1;

    @Option(name = "--jdbc", usage = "jdbc to store results")
    public String jdbc = "jdbc:postgresql://127.0.0.1:5432/sharath";

    @Option(name = "--jdbc_user", usage = "jdbc to store results")
    String user = "sharath";

    @Option(name = "--jdbc_password", usage = "jdbc to store results")
    String password = "s";


    public Settings() throws CmdLineException {
        super();
    }

    public Settings(String[] args) throws CmdLineException {
        super(args);
    }
}
