package sha;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;

import java.util.List;
import java.util.Map;

import static sha.Utils.readJsonFromClasspath;

public class App 
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            App obj = new App();
            try {
                s = readJsonFromClasspath("settings.json", Settings.class);
            } catch (Exception e) {
                log.warn("settings.json not found on classpath");
                s = new Settings();
            }
//            log.info("Using settings:{}", dumps(s));
            obj.go();
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public static class Settings {
        public String dummy = "";
        // required for jackson
        public Settings() {
        }
    }


    /**
     * All teh code from here:
     */
    private void go() throws Exception {
        Jdbi jdbi = Jdbi.create("jdbc:postgresql://indodanaplayground.rds.indodana:3433/athena", "athenaread", "a779d4f4787f3e5e8153653f1cad0d0a")
                .installPlugin(new PostgresPlugin());
        String query = "SELECT\n" +
                " *\n" +
                "FROM\n" +
                " pg_catalog.pg_tables\n" +
                "WHERE\n" +
                " schemaname != 'pg_catalog'\n" +
                "AND schemaname != 'information_schema';";
        jdbi.withHandle(handle -> {
            List<Map<String, Object>> list = handle.createQuery(query).mapToMap().list();
            for (Map<String, Object> map : list) {
                String tableName = (String)map.get("tablename");
//                log.debug("{}", tableName);
                List<Map<String, Object>> list2 = handle.createQuery("select count(*) from "+tableName+";").mapToMap().list();
                for (Map.Entry<String, Object> entry : list2.get(0).entrySet()) {
                    log.debug("{},{}", tableName, entry.getValue());
                }
            }
            return null;
        });

    }

}
