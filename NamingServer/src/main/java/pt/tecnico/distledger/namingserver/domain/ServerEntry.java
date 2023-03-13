package pt.tecnico.distledger.namingserver.domain;

public class ServerEntry {

    private String host;
    private String qualifier;

    public ServerEntry(String addressAndPort, String qual) {
        this.host = addressAndPort;
        this.qualifier = qual;        
    }
}
