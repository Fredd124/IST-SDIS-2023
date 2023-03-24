package pt.tecnico.distledger.namingserver.domain;

import pt.tecnico.distledger.namingserver.domain.exceptions.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class ServerState {
    
    private Map<String, ServiceEntry> services = Collections.synchronizedMap(new HashMap<>());
    private boolean isDebug;
    private final String MAIN_SERVER_QUALIFIER = "A";
    private final String SECONDARY_SERVER_QUALIFIER = "B";

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
            throws RegisterNotPossible, InvalidQualifier {
        if (!services.containsKey(serviceName)) {
            addService(serviceName);
        } 
        if (!qualifier.equals(MAIN_SERVER_QUALIFIER) && !qualifier.equals(SECONDARY_SERVER_QUALIFIER)) {
            throw new InvalidQualifier();
        }
        ServiceEntry serviceEntry = getService(serviceName);
        for (ServerEntry server: serviceEntry.getServers()) {
            if (server.getQualifier().equals(qualifier) || server.getAddress().equals(address)) {
                throw new RegisterNotPossible();
            }
        }
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
