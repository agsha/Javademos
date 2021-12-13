package sha;

import com.flipkart.specter.api.AdminApi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RemoteUpgradeSpecter
{
    public static void main( String[] args ) throws Exception {
        RemoteUpgradeSpecter obj = new RemoteUpgradeSpecter();
        obj.client();
    }

    private void client() throws Exception {
        Registry registry = LocateRegistry.getRegistry("10.33.34.36", 19838);

        AdminApi specter = (AdminApi) registry.lookup(AdminApi.class.getName());
        specter.upgrade("http://10.47.4.220/repos/specter-stage/12", "1.20181011202454");
//        specter.upgrade("http://10.47.4.220/repos/specter-stage/5", "1.20181011191918");
        System.out.println((specter.getCurrentVersion()));
    }
}
