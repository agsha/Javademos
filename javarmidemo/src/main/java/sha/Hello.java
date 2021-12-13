package sha;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by sharath.g on 17/04/16.
 */
public interface Hello extends Remote {
    String echo(String what) throws RemoteException;
    String exceptionExample() throws RemoteException, MyException;
}
