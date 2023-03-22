package pt.tecnico.distledger.namingserver.domain;

import pt.tecnico.distledger.namingserver.domain.exceptions.*;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class ServerState {
    
    private HashMap<String, ServiceEntry> services = new HashMap<>();
    private boolean isDebug;

    public ServerState(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public ServiceEntry getService(String serviceName) {
        return services.get(serviceName);
    }   

    public void addService(String serviceName) {
        services.put(serviceName, new ServiceEntry(serviceName));
    }

    public void removeService(String serviceName) {
        services.remove(serviceName);
    }

    public void registerService(String serviceName, String qualifier, String address) 
            throws RegisterNotPossible {
        if (!services.containsKey(serviceName)) {
            addService(serviceName);
        } 

        ServiceEntry serviceEntry = getService(serviceName);
        for (ServerEntry server: serviceEntry.getServers()) {
            if (server.getQualifier().equals(qualifier) || server.getAddress().equals(address)) {
                throw new RegisterNotPossible();
            }
        }
        System.out.println(services.size());
        serviceEntry.addServer(address, qualifier);
    }

    public List<ServerEntry> lookupService(String serviceName, String qualifier) {
        List<ServerEntry> serversForClient = new ArrayList<>();
        if(!services.containsKey(serviceName)) {
            return serversForClient;
        }
        ServiceEntry serviceEntry = getService(serviceName);
        List<ServerEntry> serversList = serviceEntry.getServers();
        if (qualifier == null) {
            return serversList;
        }
        for (ServerEntry server : serversList) {
            if (server.getQualifier().equals(qualifier)) {
                serversForClient.add(server);
            }
        }
        return serversForClient;
    }

    public void deleteService(String serviceName, String address) 
            throws RemoveNotPossible {
        if (!services.containsKey(serviceName)) {
            throw new RemoveNotPossible();
        }
        ServiceEntry serviceEntry = getService(serviceName);
        serviceEntry.removeServer(address);
    }

    public void debugPrint(String outpString) {
        if (isDebug) System.err.println(outpString);
    }
}
