package pt.tecnico.distledger.namingserver.domain;

import pt.tecnico.distledger.namingserver.domain.ServerEntry;
import java.util.ArrayList;
import java.util.List;

public class ServiceEntry {
    
    private String serviceName;
    private List<ServerEntry> servers = new ArrayList<>();

    public ServiceEntry(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public List<ServerEntry> getServers() {
        return servers;
    }

    public void addServer(String address, String qualifier) {
        servers.add(new ServerEntry(address, qualifier));
    }

    public void removeServer(String address) {
        for (int i = servers.size() - 1; i >= 0; i--) {
            ServerEntry server = servers.get(i);
            if (server.getAddress().equals(address)) {
                servers.remove(server);
            }
        }
    }
}
