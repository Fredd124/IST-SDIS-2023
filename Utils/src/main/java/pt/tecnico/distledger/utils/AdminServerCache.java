package pt.tecnico.distledger.utils;

import java.util.HashMap;
import java.util.Map;

public class AdminServerCache {

    private Map<String, AdminServerCommunicationEntry> adminEntries = new HashMap<String, AdminServerCommunicationEntry>();

    public void addEntry(String qualifier, String address) {
        adminEntries.put(qualifier, new AdminServerCommunicationEntry(address));
    }

    public void removeEntry(String qualifier) {
        adminEntries.get(qualifier).shutdown();
        adminEntries.remove(qualifier);
    }

    public boolean adminHasEntry(String qualifier) {
        return adminEntries.containsKey(qualifier);
    }

    public AdminServerCommunicationEntry adminGetEntry(String qualifier) {
        return adminEntries.get(qualifier);
    }

    public void shutdownServers() {
        for (String entry: adminEntries.keySet()) {
            AdminServerCommunicationEntry adminServerCommunicationEntry = adminEntries.get(entry);
            adminServerCommunicationEntry.shutdown();
        }
    }
}
