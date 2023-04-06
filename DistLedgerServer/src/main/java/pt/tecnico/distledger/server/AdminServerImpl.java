package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.exceptions.AlreadyActiveException;
import pt.tecnico.distledger.server.domain.exceptions.NotActiveException;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc.AdminServiceImplBase;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateOperationRequest;
import pt.tecnico.distledger.utils.Utils;
import pt.tecnico.distledger.utils.DistLedgerServerCache;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.UNAVAILABLE;

public class AdminServerImpl extends AdminServiceImplBase {

    private ServerState state;
    private DistLedgerServerCache serverCache;
    private ManagedChannel dnsChannel;
    private final String NAMING_SERVER_TARGET = "localhost:5001";
    private NamingServerServiceGrpc.NamingServerServiceBlockingStub dnsStub;
    private final String SERVICE_NAME = "DistLedger";
    private final String SECONDARY_SERVER_QUALIFIER = "B";

    public AdminServerImpl(ServerState state, DistLedgerServerCache serverCache) {
        this.state = state;
        this.serverCache = serverCache;
        dnsChannel = ManagedChannelBuilder.forTarget(NAMING_SERVER_TARGET).usePlaintext().build();
        dnsStub = NamingServerServiceGrpc.newBlockingStub(dnsChannel);
    }

    @Override
    public void activate(ActivateRequest request,
            StreamObserver<ActivateResponse> responseObserver) {

        state.debugPrint("Received activate request from admin.");
        try {
            state.activate();
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
        state.debugPrint(String.format("Propagating operation list %s", state.getLedgerState()));
        state.getLedgerState().forEach(op -> propagateToSecondary(op));
        responseObserver.onNext(GossipResponse.getDefaultInstance());
        responseObserver.onCompleted();
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

    private boolean propagateToSecondary(Operation op) {
        try {
            if (! serverCache.distLedgerHasEntry(SECONDARY_SERVER_QUALIFIER)) {
                state.debugPrint(String.format("Sending lookup request to service %s", SERVICE_NAME));
                List<String> result =  Utils.lookupOnDns(dnsStub, SECONDARY_SERVER_QUALIFIER);
                if (result.size() == 0) {
                    state.debugPrint(
                        String.format("No server found for qualifier %s.", SECONDARY_SERVER_QUALIFIER)
                    );
                    return false;
                }
                String address = result.get(0);
                state.debugPrint("Got server address: " + address);
                serverCache.addEntry(SECONDARY_SERVER_QUALIFIER, address);
                state.debugPrint(String.format("Added B qualifier to server cache."));
            }
            DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub
                 = serverCache.distLedgerGetEntry(SECONDARY_SERVER_QUALIFIER).getStub();
            try {
                DistLedgerCommonDefinitions.Operation operation = Converter.convertToGrpc(op);
                PropagateOperationRequest propagateOperationRequest = PropagateOperationRequest.newBuilder()
                    .setOperation(operation).addAllReplicaTS(state.getReplicaVectorClock()).build();
                state.debugPrint("Sending propagate operation request");
                stub.propagateOperation(propagateOperationRequest);
                state.debugPrint("Propagated operation successfully");
            } catch (StatusRuntimeException e) {
                List<Operation> ledger = new ArrayList<>(state.getLedgerState());
                ledger.add(op);
                DistLedgerCommonDefinitions.LedgerState ledgerState 
                = DistLedgerCommonDefinitions.LedgerState.newBuilder()
                .addAllLedger(
                    ledger.stream()
                    .map(operation -> Converter.convertToGrpc(operation)).collect(Collectors.toList())
                ).build();
                PropagateStateRequest propagateRequest = PropagateStateRequest.newBuilder()
                    .setState(ledgerState).build();
                state.debugPrint("Sending propagate request");
                stub.propagateState(propagateRequest);
                state.debugPrint("Propagated successfully");
            }
            return true;
        }         
        catch (StatusRuntimeException e ) {
            serverCache.removeEntry(SECONDARY_SERVER_QUALIFIER);
            state.debugPrint("Propagate failed for server in cache.");
            return false;
        }
    }

    public void shutdownChannels() {
        serverCache.shutdown();
        dnsChannel.shutdown();
    }

}
