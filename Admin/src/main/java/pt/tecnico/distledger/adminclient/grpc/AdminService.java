package pt.tecnico.distledger.adminclient.grpc;

import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.adminclient.ServerCache;
import static io.grpc.Status.UNAVAILABLE;

public class AdminService {

    private NamingServerServiceGrpc.NamingServerServiceBlockingStub namingServerStub;
    private ManagedChannel namingServerChannel;
    private ServerCache serverCache;
    private boolean debug;

    public AdminService(String target, boolean debug) {
        this.debug = debug;
        this.namingServerChannel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.namingServerStub = NamingServerServiceGrpc.newBlockingStub(namingServerChannel);
        this.serverCache = new ServerCache();
    }

    private void lookup(String qualifier) {
        try {
            NamingServer.LookupRequest request = NamingServer.LookupRequest.newBuilder().
                                                    setName("DistLedger").setQualifier(qualifier).build();
            debugPrint(String.format("Sent lookup request to server DistLedger %s .", qualifier));
            NamingServer.LookupResponse response = namingServerStub.lookup(request);
            debugPrint(String.format("Received lookup response from server with servers list %s .", 
                response.getServersList().toString()));
            String address = response.getServers(0);
            serverCache.addEntry(qualifier, address);
        } catch (IndexOutOfBoundsException e) {
            debugPrint(
                    String.format("Caught exception : %s .", e.getMessage()));
            System.out.println("No server found for qualifier " + qualifier);
        }
    }

    public void activate(String qualifier) {
        try {
            if (!serverCache.hasEntry(qualifier)) {
                lookup(qualifier);
            } 
            AdminServiceGrpc.AdminServiceBlockingStub stub = serverCache.getEntry(qualifier).getStub();            
            AdminDistLedger.ActivateRequest request = AdminDistLedger.ActivateRequest.newBuilder().build();
            debugPrint(String.format("Sending activate request to server %s ...", qualifier));
            stub.activate(request);
            System.out.println("OK");
            debugPrint("Server activated");
        } catch (StatusRuntimeException e) {
            debugPrint(
                    String.format("Caugth exception : %s .", e.getMessage()));
            System.out.println(e.getStatus().getDescription());
            if (e.getStatus().equals(UNAVAILABLE)) {
                serverCache.removeEntry(qualifier);
            }
        }
    }

    public void deactivate(String qualifier) {
        try {
            if (!serverCache.hasEntry(qualifier)) {
                lookup(qualifier);
            } 
            AdminServiceGrpc.AdminServiceBlockingStub stub = serverCache.getEntry(qualifier).getStub();            
            AdminDistLedger.DeactivateRequest request = AdminDistLedger.DeactivateRequest.newBuilder().build();
            debugPrint(String.format("Sending deactivate request to server %s ...", qualifier));
            stub.deactivate(request);
            System.out.println("OK");
            debugPrint("Server deactivated");
        } catch (StatusRuntimeException e) {
            debugPrint(
                    String.format("Caugth exception : %s .", e.getMessage()));
            System.out.println(e.getStatus().getDescription());
            if (e.getStatus().equals(UNAVAILABLE)) {
                serverCache.removeEntry(qualifier);
            }
        }
    }

    public void gossip(String qualifier) {
        try {
            if (!serverCache.hasEntry(qualifier)) {
                lookup(qualifier);
            } 
            AdminServiceGrpc.AdminServiceBlockingStub stub = serverCache.getEntry(qualifier).getStub();            
            AdminDistLedger.GossipRequest request = AdminDistLedger.GossipRequest.newBuilder().build();
            debugPrint(String.format("Sending gossip request to server %s ...", qualifier));
            stub.gossip(request);
            System.out.println("OK");
            debugPrint("Server gossiped");
        } catch (StatusRuntimeException e) {
            debugPrint(
                    String.format("Caugth exception : %s .", e.getMessage()));
            System.out.println(e.getStatus().getDescription());
            if (e.getStatus().equals(UNAVAILABLE)) {
                serverCache.removeEntry(qualifier);
            }
        }
    }

    public void getLedgerState(String qualifier) {
        try {
            if (!serverCache.hasEntry(qualifier)) {
                lookup(qualifier);
            } 
            AdminServiceGrpc.AdminServiceBlockingStub stub = serverCache.getEntry(qualifier).getStub();            
            AdminDistLedger.getLedgerStateRequest request = AdminDistLedger.getLedgerStateRequest.newBuilder().build();
            debugPrint(String.format("Sending getLedgerState request to server %s ...", qualifier));
            AdminDistLedger.getLedgerStateResponse response = stub.getLedgerState(request);
            debugPrint(String.format("Received getLedgerState response from server with ledger state %s .", 
                    response.getLedgerState().toString()));
            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            debugPrint(
                    String.format("Caugth exception : %s .", e.getMessage()));
            System.out.println(e.getStatus().getDescription());
            if (e.getStatus().equals(UNAVAILABLE)) {
                serverCache.removeEntry(qualifier);
            }
        }
    }

    public void shutdown() {
        debugPrint("Shutting down namingServerChannel...");
        namingServerChannel.shutdownNow();
        serverCache.shutdownServers();
        debugPrint("namingServerChannel shut down");
    }

    public void debugPrint(String message) {
        if (debug) {
            System.out.println(message);
        }
    }
}
