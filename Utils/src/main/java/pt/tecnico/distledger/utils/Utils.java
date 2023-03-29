package pt.tecnico.distledger.utils;

import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import java.util.List;

public class Utils {
    
    public static List<String> lookupOnDns(NamingServerServiceGrpc.NamingServerServiceBlockingStub dnsStub, String qualifier) {
        final String SERVICE_NAME = "DistLedger";
        LookupRequest request = LookupRequest.newBuilder().setName(SERVICE_NAME)
            .setQualifier(qualifier).build();
        LookupResponse response = dnsStub.lookup(request);
        return response.getServersList();
    }
    
    public static int getIndexFromQualifier(Character qualifier) {
        return qualifier - 'A';
    }
}
