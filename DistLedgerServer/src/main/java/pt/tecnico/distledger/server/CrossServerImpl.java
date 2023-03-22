package pt.tecnico.distledger.server;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateResponse;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateOperationRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateOperationResponse;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.ProvideStateResponse;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.ProvideStateRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import io.grpc.stub.StreamObserver;

import static io.grpc.Status.UNAVAILABLE;

import java.util.List;
import java.util.stream.Collectors;

public class CrossServerImpl extends DistLedgerCrossServerServiceImplBase {

    private ServerState state;
    private ServerCache serverCache;
    private ManagedChannel dnsChannel;
    private NamingServerServiceGrpc.NamingServerServiceBlockingStub dnsStub;
    
    public CrossServerImpl(ServerState state, ServerCache serverCache) {
        this.state = state;
        this.serverCache = serverCache;
        dnsChannel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();
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
            responseObserver.onError(UNAVAILABLE.withDescription("Ledger size is different.").asRuntimeException());
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
        String other = qualifier.equals("A") ? "B" : "A";
        state.debugPrint("Asking for state from " + other);

        this.lookupAndAsk(other);
    }

    private void lookupAndAsk(String qualifier) {
        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub; 
        try {
            if (! serverCache.hasEntry(qualifier)) {
                state.debugPrint("Doing lookup");
                LookupRequest request = LookupRequest.newBuilder().setName("DistLedger")
                    .setQualifier(qualifier).build();
                LookupResponse response = dnsStub.lookup(request);
                String address = response.getServers(0);
                state.debugPrint("Got server address: " + address);
                ManagedChannel channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
                stub
                    = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
                serverCache.addEntry(qualifier, address);
                state.debugPrint(String.format("Added B qualifier to server cache."));
            }
            else stub = serverCache.getEntry(qualifier).getStub();

            ProvideStateRequest request = ProvideStateRequest.newBuilder().build();
            ProvideStateResponse response = stub.provideState(request);
            DistLedgerCommonDefinitions.LedgerState state = response.getState();
            List<Operation> ops = state.getLedgerList().stream().map(op -> Converter.convertFromGrpc(op))
                .collect(Collectors.toList());
            this.state.debugPrint(ops.size() + "|---|" + this.state.getLedgerState().size());
            if (ops.size() > this.state.getLedgerState().size()) {
                List<Operation> missingOps = ops.subList(this.state.getLedgerState().size(), ops.size());
                this.state.debugPrint("Missing ops: " + missingOps.size());
                this.state.doOpList(missingOps);
            }

        } 
        catch (IndexOutOfBoundsException e) {
            state.debugPrint("Provide failed");
        }
        catch (StatusRuntimeException e ) {
            serverCache.invalidateEntry(qualifier);
            state.debugPrint("Propagate failed for server in cache.");
            lookupAndAsk(qualifier);
        }
    }

    public void shutdownChannel() {
        dnsChannel.shutdown();
    }
}
