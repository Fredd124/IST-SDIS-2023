package pt.tecnico.distledger.server;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateResponse;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateOperationRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateOperationResponse;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase;
import pt.tecnico.distledger.utils.DistLedgerServerCache;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;

import io.grpc.stub.StreamObserver;

import static io.grpc.Status.UNAVAILABLE;

import java.util.List;
import java.util.stream.Collectors;

public class CrossServerImpl extends DistLedgerCrossServerServiceImplBase {

    private ServerState state;

    public CrossServerImpl(ServerState state, DistLedgerServerCache serverCache) {
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
        if (ops.size() > this.state.getLedgerState().size()) {
            List<Operation> missingOps = ops.subList(this.state.getLedgerState().size(), ops.size());
            this.state.debugPrint("Missing ops: " + missingOps.size());
            this.state.doOpList(missingOps, null);
        }
        PropagateStateResponse response = PropagateStateResponse.getDefaultInstance();
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
        /* if (state.getLedgerState().size() != request.getLedgerSize()) {
            state.debugPrint("Ledger size is different.");
            responseObserver.onError(CANCELLED.withDescription("Ledger size is different.").asRuntimeException());
            return;
        } */
        Operation op = Converter.convertFromGrpc(request.getOperation());
        this.state.debugPrint("Received operation: " + op.toString());
        this.state.verifyOp(op, null);
        this.state.updateReplicaClocks(request.getReplicaTSList());
        PropagateOperationResponse response = PropagateOperationResponse.getDefaultInstance();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
