package pt.tecnico.distledger.utils;

import java.util.HashMap;
import java.util.Map;

public class DistLedgerServerCache {
    
    private Map<String, DistLedgerServerCommunicationEntry> distLedgerEntries = new HashMap<String, DistLedgerServerCommunicationEntry>();

    public void addEntry(String qualifier, String address) {
        distLedgerEntries.put(qualifier, new DistLedgerServerCommunicationEntry(address));
    }

    public void removeEntry(String qualifier) {
        distLedgerEntries.remove(qualifier);
    }

    public boolean distLedgerHasEntry(String qualifier) {
        return distLedgerEntries.containsKey(qualifier);
    }

    public DistLedgerServerCommunicationEntry distLedgerGetEntry(String qualifier) {
        return distLedgerEntries.get(qualifier);
    }

    public void shutdownServers() {
        for (String entry: distLedgerEntries.keySet()) {
            DistLedgerServerCommunicationEntry distLedgerServerCommunicationEntry = distLedgerEntries.get(entry);
            distLedgerServerCommunicationEntry.shutdown();
        }
    }
}

