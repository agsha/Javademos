package sha;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by sharath.g on 17/04/16.
 */
public class HelloImpl extends UnicastRemoteObject implements  Hello {
    protected HelloImpl() throws RemoteException {
        super();

    }

    @Override
    public String echo(String what) {
        return "Hello "+what;
    }
}
