package pt.tecnico.distledger.utils;

import java.util.HashMap;
import java.util.Map;

public class ServerCache {

    private Map<String, UserServerCommunicationEntry> userEntries = new HashMap<String, UserServerCommunicationEntry>();
    private Map<String, AdminServerCommunicationEntry> adminEntries = new HashMap<String, AdminServerCommunicationEntry>();
    private Map<String, DistLedgerServerCommunicationEntry> distLedgerEntries = new HashMap<String, DistLedgerServerCommunicationEntry>();

    public void addEntry(String qualifier, String address) {
        userEntries.put(qualifier, new UserServerCommunicationEntry(address));
        adminEntries.put(qualifier, new AdminServerCommunicationEntry(address));
        distLedgerEntries.put(qualifier, new DistLedgerServerCommunicationEntry(address));
    }

    public void removeEntry(String qualifier) {
        userEntries.remove(qualifier);
        adminEntries.remove(qualifier);
        distLedgerEntries.remove(qualifier);
    }

    public boolean userHasEntry(String qualifier) {
        return userEntries.containsKey(qualifier);
    }

    public boolean adminHasEntry(String qualifier) {
        return adminEntries.containsKey(qualifier);
    }

    public boolean distLedgerHasEntry(String qualifier) {
        return distLedgerEntries.containsKey(qualifier);
    }

    public UserServerCommunicationEntry userGetEntry(String qualifier) {
        return userEntries.get(qualifier);
    }

    public AdminServerCommunicationEntry adminGetEntry(String qualifier) {
        return adminEntries.get(qualifier);
    }

    public DistLedgerServerCommunicationEntry distLedgerGetEntry(String qualifier) {
        return distLedgerEntries.get(qualifier);
    }

    public void shutdownServers() {
        for (String entry: userEntries.keySet()) {
            UserServerCommunicationEntry userServerCommunicationEntry = userEntries.get(entry);
            userServerCommunicationEntry.shutdown();
            AdminServerCommunicationEntry adminServerCommunicationEntry = adminEntries.get(entry);
            adminServerCommunicationEntry.shutdown();
            DistLedgerServerCommunicationEntry distLedgerServerCommunicationEntry = distLedgerEntries.get(entry);
            distLedgerServerCommunicationEntry.shutdown();
        }
    }
}