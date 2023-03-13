package pt.tecnico.distledger.namingserver.domain;

import java.util.ArrayList;
import java.util.List;

public class ServiceEntry {
    
    private String serviceName;
    private List<ServerEntry> servers = new ArrayList<>();

    public ServiceEntry(String serviceName) {
        this.serviceName = serviceName;
    }
}
