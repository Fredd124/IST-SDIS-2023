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

import io.grpc.stub.StreamObserver;

import static io.grpc.Status.UNAVAILABLE;

import java.util.List;
import java.util.stream.Collectors;

public class CrossServerImpl extends DistLedgerCrossServerServiceImplBase {

    private ServerState state;
    
    public CrossServerImpl(ServerState state) {
        this.state = state;
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
        this.state.debugPrint(ops.size() + "|---|" + this.state.getLedgerState().size());
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
}
