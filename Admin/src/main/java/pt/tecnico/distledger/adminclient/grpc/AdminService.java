package pt.tecnico.distledger.adminclient.grpc;

import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer;
import pt.tecnico.distledger.adminclient.grpc.exceptions.NoServerFoundException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class AdminService {

    /* TODO: The gRPC client-side logic should be here.
        This should include a method that builds a namingServerChannel and stub,
        as well as individual methods for each remote operation of this service. */

    private NamingServerServiceGrpc.NamingServerServiceBlockingStub namingServerStub;
    private ManagedChannel namingServerChannel, channel;
    private boolean debug;

    public AdminService(String target, boolean debug) {
        this.debug = debug;
        this.namingServerChannel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.namingServerStub = NamingServerServiceGrpc.newBlockingStub(namingServerChannel);
    }

    private AdminServiceGrpc.AdminServiceBlockingStub lookup(String server) 
                                                    throws NoServerFoundException{
        try {
            NamingServer.LookupRequest request = NamingServer.LookupRequest.newBuilder().
                                                    setName("DistLedger").setQualifier(server).build();
            NamingServer.LookupResponse response = namingServerStub.lookup(request);
            String address = response.getServers(0);
            this.channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
            AdminServiceGrpc.AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);
            return stub;
        } catch (IndexOutOfBoundsException e) {
            throw new NoServerFoundException("No server found with name " + server);
        }
    }

    public void activate(String server) {
        try {
            AdminServiceGrpc.AdminServiceBlockingStub stub = lookup(server);

            AdminDistLedger.ActivateRequest request = AdminDistLedger.ActivateRequest.newBuilder().build();
            debugPrint("Sending activate request to server...");
            stub.activate(request);
            System.out.println("OK");
            debugPrint("Server activated");

            channel.shutdownNow();
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        } catch (NoServerFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    public void deactivate(String server) {
        try {
            AdminServiceGrpc.AdminServiceBlockingStub stub = lookup(server);

            AdminDistLedger.DeactivateRequest request = AdminDistLedger.DeactivateRequest.newBuilder().build();
            debugPrint("Sending deactivate request to server...");
            stub.deactivate(request);
            System.out.println("OK");
            debugPrint("Server deactivated");

            channel.shutdownNow();
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        } catch (NoServerFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    public void gossip(String server) {
        try {
            AdminServiceGrpc.AdminServiceBlockingStub stub = lookup(server);

            AdminDistLedger.GossipRequest request = AdminDistLedger.GossipRequest.newBuilder().build();
            debugPrint("Sending gossip request to server...");
            stub.gossip(request);
            System.out.println("OK");
            debugPrint("Server gossiped");

            channel.shutdownNow();
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        } catch (NoServerFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    public void getLedgerState(String server) {
        try {
            AdminServiceGrpc.AdminServiceBlockingStub stub = lookup(server);

            AdminDistLedger.getLedgerStateRequest request = AdminDistLedger.getLedgerStateRequest.newBuilder().build();
            debugPrint("Sending getLedgerState request to server...");
            AdminDistLedger.getLedgerStateResponse response = stub.getLedgerState(request);
            debugPrint("Server responded with ledger state");
            System.out.println("OK");
            System.out.println(response);
            debugPrint("LedgerState string to print created");

            channel.shutdownNow();
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        } catch (NoServerFoundException e) {
            System.out.println(e.getMessage());
        }

        return;
    }

    public void shutdown() {
        debugPrint("Shutting down namingServerChannel...");
        namingServerChannel.shutdownNow();
        debugPrint("namingServerChannel shut down");
    }

    public void debugPrint(String message) {
        if (debug) {
            System.out.println(message);
        }
    }
}
