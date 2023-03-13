package pt.tecnico.distledger.namingserver.domain;

public class ServerEntry {
    private String hostPort;
    private String hostAddress;
    private String qualifier;

    public ServerEntry(String port, String address, String qual) {
        this.hostPort = port;
        this.hostAddress = address;
        this.qualifier = qual;        
    }
}
