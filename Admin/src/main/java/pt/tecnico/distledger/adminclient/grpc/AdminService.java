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
    private boolean debug;

    public AdminService(String target, boolean debug) {
        this.debug = debug;
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = AdminServiceGrpc.newBlockingStub(channel);
    }

    public void activate() {
        try {
            AdminDistLedger.ActivateRequest request = AdminDistLedger.ActivateRequest.newBuilder().build();
            debugPrint("Sending activate request to server...");
            stub.activate(request);
            System.out.println("OK");
            debugPrint("Server activated");
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void deactivate() {
        try {
            AdminDistLedger.DeactivateRequest request = AdminDistLedger.DeactivateRequest.newBuilder().build();
            debugPrint("Sending deactivate request to server...");
            stub.deactivate(request);
            System.out.println("OK");
            debugPrint("Server deactivated");
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void gossip() {
        try {
            AdminDistLedger.GossipRequest request = AdminDistLedger.GossipRequest.newBuilder().build();
            debugPrint("Sending gossip request to server...");
            stub.gossip(request);
            System.out.println("OK");
            debugPrint("Server gossiped");
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void getLedgerState() {
        try {
            String res = "";
            AdminDistLedger.getLedgerStateRequest request = AdminDistLedger.getLedgerStateRequest.newBuilder().build();
            debugPrint("Sending getLedgerState request to server...");
            AdminDistLedger.getLedgerStateResponse response = stub.getLedgerState(request);
            debugPrint("Server responded with ledger state");

            res += "LedgerState {\n";
            
            for (DistLedgerCommonDefinitions.Operation op : response.getLedgerState().getLedgerList()) {
                res += op.toString() + "\n";
            }

            res += "}";
            debugPrint("LedgerState string to print created");

            System.out.println("OK");
            System.out.println(res);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }

        return;
    }

    public void shutdown() {
        debugPrint("Shutting down channel...");
        channel.shutdownNow();
        debugPrint("Channel shut down");
    }

    public void debugPrint(String message) {
        if (debug) {
            System.out.println(message);
        }
    }
}
