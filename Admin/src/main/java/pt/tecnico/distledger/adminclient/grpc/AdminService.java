package pt.tecnico.distledger.adminclient.grpc;

import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class AdminService {

    /* TODO: The gRPC client-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service. */

    private AdminServiceGrpc.AdminServiceBlockingStub stub;
    private ManagedChannel channel;

    public AdminService(String host, int port) {
        String target = host + ":" + port;
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = AdminServiceGrpc.newBlockingStub(channel);
    }

    public void activate() {
        try {
            AdminDistLedger.ActivateRequest request = AdminDistLedger.ActivateRequest.newBuilder().build();
            stub.activate(request);
            System.out.println("OK");
            System.out.println("Server activated");
        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        }
    }

    public void deactivate() {
        try {
            AdminDistLedger.DeactivateRequest request = AdminDistLedger.DeactivateRequest.newBuilder().build();
            stub.deactivate(request);
            System.out.println("OK");
            System.out.println("Server deactivated");
        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        }
    }

    public void gossip() {
        try {
            AdminDistLedger.GossipRequest request = AdminDistLedger.GossipRequest.newBuilder().build();
            stub.gossip(request);
            System.out.println("OK");
            System.out.println("Gossip done");
        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        }
    }

    public void getLedgerState() {
        try {
            String res = "";
            AdminDistLedger.getLedgerStateRequest request = AdminDistLedger.getLedgerStateRequest.newBuilder().build();
            AdminDistLedger.getLedgerStateResponse response = stub.getLedgerState(request);

            res += "LedgerState {\n";
            
            for (DistLedgerCommonDefinitions.Operation op : response.getLedgerState().getLedgerList()) {
                res += op.toString() + "\n";
            }

            res += "}";

            System.out.println("OK");
            System.out.println(res);
        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        }

        return;
    }

    public void shutdown() {
        channel.shutdownNow();
    }
}
