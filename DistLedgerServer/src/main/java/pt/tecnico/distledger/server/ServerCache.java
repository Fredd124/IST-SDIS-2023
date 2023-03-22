package pt.tecnico.distledger.server;

import java.util.HashMap;
import java.util.Map;

public class ServerCache {

    private Map<String, ServerCommunicationEntry> entries = new HashMap<String, ServerCommunicationEntry>();

    public void addEntry(String qualifier, String address) {
        entries.put(qualifier, new ServerCommunicationEntry(address));
    }

    public void invalidateEntry(String qualifier) {
        entries.remove(qualifier);
    }

    public boolean hasEntry(String qualifier) {
        return entries.containsKey(qualifier);
    }

    public ServerCommunicationEntry getEntry(String qualifier) {
        return entries.get(qualifier);
    }

}