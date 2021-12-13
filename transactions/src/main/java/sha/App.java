package sha;


import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.aspectj.AnnotationTransactionAspect;

public class App
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) {
        try {
            App obj = new App();
            obj.go();
        } catch (Exception e) {
            log.error("", e);
            log.info("a {}", "b");
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
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl("jdbc:mysql://127.0.0.1/pentos?user=root&&password=");
        DataSourceTransactionManager txManager = new DataSourceTransactionManager(ds);
        AnnotationTransactionAspect.aspectOf().setTransactionManager(txManager);
//        TransactionDefinition def = new DefaultTransactionDefinition();
//
//        TransactionStatus status = txManager.getTransaction(def);
        doquery(ds);
//        Connection connection = ds.getConnection();
//        ResultSet rs = connection.createStatement().executeQuery("show tables;");
//        while(rs.next()) {
//            System.out.println(rs.getString(1));
////            log.info("{}", rs.getString(1));
//        }
//        txManager.commit(status);
    }

    @Transactional
    public void doquery(BasicDataSource ds) throws Exception {
//                Connection connection = ds.getConnection();
//        connection.createStatement().executeUpdate("insert into dummy (val) values ('goooo')");

        JdbcTemplate template = new JdbcTemplate(ds);
       template.update("insert into dummy (val) values ('goooo')");
        throw new RuntimeException("gooo");
    }
}
