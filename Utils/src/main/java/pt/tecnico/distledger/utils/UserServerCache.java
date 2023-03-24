package pt.tecnico.distledger.utils;

import java.util.HashMap;
import java.util.Map;

public class UserServerCache {

    private Map<String, UserServerCommunicationEntry> userEntries = new HashMap<String, UserServerCommunicationEntry>();
    
    public void addEntry(String qualifier, String address) {
        userEntries.put(qualifier, new UserServerCommunicationEntry(address));
    }

    public void removeEntry(String qualifier) {
        userEntries.remove(qualifier);
    }

    public boolean userHasEntry(String qualifier) {
        return userEntries.containsKey(qualifier);
    }

    public UserServerCommunicationEntry userGetEntry(String qualifier) {
        return userEntries.get(qualifier);
    }

    public void shutdownServers() {
        for (String entry: userEntries.keySet()) {
            UserServerCommunicationEntry userServerCommunicationEntry = userEntries.get(entry);
            userServerCommunicationEntry.shutdown();
        }
    }
}