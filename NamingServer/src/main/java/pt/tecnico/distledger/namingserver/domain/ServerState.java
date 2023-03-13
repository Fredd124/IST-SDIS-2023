package pt.tecnico.distledger.namingserver.domain;

import java.util.HashMap;
import java.util.Map;

public class ServerState {
    
    private HashMap<String, ServiceEntry> services = new HashMap<>();

    public ServerState() {
    
    }

    public registerService(String serviceName, String qualifier, String address) {
        if (! services.containsKey(serviceName)) {
            ServiceEntry serviceEntry = new ServiceEntry(serviceName);
        }
        serviceEntry.put(serviceName, new ServerEntry(address, qualifier));
    }

    public void addService(String serviceName) {
        services.put(serviceName, new ServiceEntry(serviceName));
    }

    public void removeService(String serviceName) {
        services.remove(serviceName);
    }

    public List<String> lookupService(String serviceName, String qualifier) {
        
    }
    
}
