package pt.tecnico.distledger.server;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateResponse;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateOperationRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateOperationResponse;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.ProvideStateResponse;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.ProvideStateRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase;
import pt.tecnico.distledger.utils.DistLedgerServerCache;
import pt.tecnico.distledger.utils.Utils;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import io.grpc.stub.StreamObserver;

import static io.grpc.Status.UNAVAILABLE;
import static io.grpc.Status.CANCELLED;

import java.util.List;
import java.util.stream.Collectors;

public class CrossServerImpl extends DistLedgerCrossServerServiceImplBase {

    private ServerState state;
    private DistLedgerServerCache serverCache;
    private ManagedChannel dnsChannel;
    private NamingServerServiceGrpc.NamingServerServiceBlockingStub dnsStub;
    private final String NAMING_SERVER_TARGET = "localhost:5001";
    private final String SERVICE_NAME = "DistLedger";
    private final String MAIN_SERVER_QUALIFIER = "A";
    private final String SECONDARY_SERVER_QUALIFIER = "B";

    public CrossServerImpl(ServerState state, DistLedgerServerCache serverCache) {
        this.state = state;
        this.serverCache = serverCache;
        dnsChannel = ManagedChannelBuilder.forTarget(NAMING_SERVER_TARGET).usePlaintext().build();
        dnsStub = NamingServerServiceGrpc.newBlockingStub(dnsChannel);
        if (state.getLedgerState().size() == 0) {
            this.askForState();
        }
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {
        if (!state.isActive()){
            state.debugPrint("Server is innactive.");
            responseObserver.onError(UNAVAILABLE.withDescription("Server is innactive.").asRuntimeException());
            return;
        }
        DistLedgerCommonDefinitions.LedgerState state = request.getState();
        List<Operation> ops = state.getLedgerList().stream().map(op -> Converter.convertFromGrpc(op))
            .collect(Collectors.toList());
        if (ops.size() > this.state.getLedgerState().size()) {
            List<Operation> missingOps = ops.subList(this.state.getLedgerState().size(), ops.size());
            this.state.debugPrint("Missing ops: " + missingOps.size());
            this.state.doOpList(missingOps);
        }
        PropagateStateResponse response = PropagateStateResponse.getDefaultInstance();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void provideState(ProvideStateRequest request, StreamObserver<ProvideStateResponse> responseObserver) {
        if (!state.isActive()){
            state.debugPrint("Server is innactive.");
            responseObserver.onError(UNAVAILABLE.withDescription("Server is innactive.").asRuntimeException());
            return;
        }
        DistLedgerCommonDefinitions.LedgerState ledgerState 
            = DistLedgerCommonDefinitions.LedgerState.newBuilder()
        .addAllLedger(
            state.getLedgerState().stream()
            .map(op -> Converter.convertToGrpc(op)).collect(Collectors.toList())
        ).build();
        ProvideStateResponse response = ProvideStateResponse.newBuilder().setState(ledgerState).build();
        state.debugPrint("Sending State");
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void propagateOperation(PropagateOperationRequest request, StreamObserver<PropagateOperationResponse> responseObserver) {
        if (!state.isActive()){
            state.debugPrint("Server is innactive.");
            responseObserver.onError(UNAVAILABLE.withDescription("Server is innactive.").asRuntimeException());
            return;
        }
        if (state.getLedgerState().size() != request.getLedgerSize()) {
            state.debugPrint("Ledger size is different.");
            responseObserver.onError(CANCELLED.withDescription("Ledger size is different.").asRuntimeException());
            return;
        }
        Operation op = Converter.convertFromGrpc(request.getOperation());
        this.state.debugPrint("Received operation: " + op.toString());
        this.state.doOp(op);
        PropagateOperationResponse response = PropagateOperationResponse.getDefaultInstance();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void askForState() {
        String qualifier = state.getQualifier();
        String other = qualifier.equals(MAIN_SERVER_QUALIFIER) ? SECONDARY_SERVER_QUALIFIER : MAIN_SERVER_QUALIFIER;
        state.debugPrint("Asking for state from " + other);

        this.lookupAndAskForState(qualifier);
    }

    private void lookupAndAskForState(String qualifier) {
        try {
            if (!serverCache.distLedgerHasEntry(qualifier)) {
                state.debugPrint(String.format("Sending lookup request to service %s", SERVICE_NAME));
                List<String> result = Utils.lookupOnDns(dnsStub, qualifier);
                if (result.size() == 0) {
                    state.debugPrint(String.format("No server found for qualifier %s.", qualifier));
                    return;
                }
                String address = result.get(0);
                state.debugPrint("Got server address: " + address);
                serverCache.addEntry(qualifier, address);
                state.debugPrint(String.format("Added %s qualifier to server cache.", qualifier));
            }
            DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub 
                = serverCache.distLedgerGetEntry(qualifier).getStub();
            ProvideStateRequest request = ProvideStateRequest.newBuilder().build();
            ProvideStateResponse response = stub.provideState(request);
            DistLedgerCommonDefinitions.LedgerState state = response.getState();
            List<Operation> ops = state.getLedgerList().stream().map(op -> Converter.convertFromGrpc(op))
                .collect(Collectors.toList());
            if (ops.size() > this.state.getLedgerState().size()) {
                List<Operation> missingOps = ops.subList(this.state.getLedgerState().size(), ops.size());
                this.state.debugPrint("Missing ops: " + missingOps.size());
                this.state.doOpList(missingOps);
            }
        } 
        catch (StatusRuntimeException e ) {
            serverCache.removeEntry(qualifier);
            state.debugPrint("Propagate failed for server in cache.");
        }
    }

    public void shutdownChannel() {
        dnsChannel.shutdown();
    }
}
