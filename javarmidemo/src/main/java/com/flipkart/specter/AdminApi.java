package com.flipkart.specter;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AdminApi extends Remote {
    void upgrade(String repoUrl, String debVersion) throws RemoteException, IOException;
    String getCurrentVersion() throws RemoteException, IOException;
}
