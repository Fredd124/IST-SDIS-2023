package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.exceptions.AlreadyActiveException;
import pt.tecnico.distledger.server.domain.exceptions.NotActiveException;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc.AdminServiceImplBase;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.DeleteRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.RegisterRequest;

import java.util.ArrayList;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.UNAVAILABLE;

public class AdminServerImpl extends AdminServiceImplBase {

    ServerState state;

    public AdminServerImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void activate(ActivateRequest request,
            StreamObserver<ActivateResponse> responseObserver) {

        state.debugPrint("Received activate request from admin.");
        try {
            state.activate();
            ManagedChannel dnsChannel = ManagedChannelBuilder.forTarget("localhost:5001")
                .usePlaintext().build();
            NamingServerServiceGrpc.NamingServerServiceBlockingStub dnsStub = 
                NamingServerServiceGrpc.newBlockingStub(dnsChannel);
            RegisterRequest registerRequest = RegisterRequest.newBuilder().setName("DistLedger")
                .setAddress(state.getAddress()).setQualifier(state.getQualifier()).build();
            dnsStub.register(registerRequest);
            state.debugPrint(String.format("Activated server ."));
            ActivateResponse response = ActivateResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (AlreadyActiveException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void deactivate(DeactivateRequest request,
            StreamObserver<DeactivateResponse> responseObserver) {

        state.debugPrint("Received deactivate request from admin.");
        try {
            state.deactivate();
            ManagedChannel dnsChannel = ManagedChannelBuilder.forTarget("localhost:5001")
                .usePlaintext().build();
            NamingServerServiceGrpc.NamingServerServiceBlockingStub dnsStub = 
                NamingServerServiceGrpc.newBlockingStub(dnsChannel);
            DeleteRequest deleteRequest = DeleteRequest.newBuilder().setName("DistLedger")
                .setAddress(state.getAddress()).build();
            dnsStub.delete(deleteRequest);
            state.debugPrint(String.format("Deactivated server ."));
            DeactivateResponse response = DeactivateResponse.newBuilder()
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (NotActiveException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void gossip(GossipRequest request,
            StreamObserver<GossipResponse> responseObserver) {
        // Only for 3rd delivery
    }

    @Override
    public void getLedgerState(getLedgerStateRequest request,
            StreamObserver<getLedgerStateResponse> responseObserver) {

        state.debugPrint("Received get ledger state request from admin.");
        ArrayList<DistLedgerCommonDefinitions.Operation> ledgerState = new ArrayList<DistLedgerCommonDefinitions.Operation>();
        for (Operation op : state.getLedgerState()) {
            DistLedgerCommonDefinitions.Operation operation = Converter
                    .convertToGrpc(op);
            ledgerState.add(operation);
        }
        DistLedgerCommonDefinitions.LedgerState ledgerStateGrpc = DistLedgerCommonDefinitions.LedgerState
                .newBuilder().addAllLedger(ledgerState).build();

        state.debugPrint(String.format("Got ledger state ."));
        getLedgerStateResponse response = getLedgerStateResponse.newBuilder()
                .setLedgerState(ledgerStateGrpc).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
