package pt.tecnico.distledger.utils;

import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.QualifierAdressPair;
import java.util.List;

public class Utils {
    
    public static List<QualifierAdressPair> lookupOnDns(NamingServerServiceGrpc.NamingServerServiceBlockingStub dnsStub, String qualifier) {
        final String SERVICE_NAME = "DistLedger";
        LookupRequest request = LookupRequest.newBuilder().setName(SERVICE_NAME)
            .setQualifier(qualifier).build();
        LookupResponse response = dnsStub.lookup(request);
        return response.getServersList();
    }
    
    public static int getIndexFromQualifier(Character qualifier) {
        return qualifier - 'A';
    }

    public static int compareVectorClocks(List<Integer> a, List<Integer> b) {
        int i;
        for (i = 0; i < a.size(); i++) {
            if (a.get(i) > b.get(i)) {
                break;
            }
        }
        if (i == a.size()) return 1; // b is bigger
        for (i = 0; i < a.size(); i++) {
            if (a.get(i) < b.get(i)) {
                break;
            }
        }
        if (i == a.size()) return -1; // a is bigger
        return 0; //  no one is bigger
    } 
    
    public static boolean isSmallerVectorClock(List<Integer> a, List<Integer> b) {
        boolean equal = true;
        int i;
        for (i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) {
                equal = false;
            }
        }
        return equal || (compareVectorClocks(a, b) != 1);
    }

}
