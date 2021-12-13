package com.flipkart.specter.api;

import java.io.IOException;
import java.rmi.Remote;

public interface AdminApi extends Remote {
    void upgrade(String repoUrl, String debVersion) throws IOException;
    String getCurrentVersion() throws IOException;
}
