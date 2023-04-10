package pt.tecnico.distledger.server;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateResponse;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase;
import pt.tecnico.distledger.utils.DistLedgerServerCache;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;

import io.grpc.stub.StreamObserver;

import static io.grpc.Status.UNAVAILABLE;

import java.util.List;
import java.util.ArrayList;
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
        ops.forEach(op -> {
            List<Integer> clientVectorClock = new ArrayList<>(op.getPrev());
            if (! this.state.isRepeatedOp(op)) this.state.addOp(op, clientVectorClock);
        });
        this.state.updateReplicaClocks(request.getReplicaTSList());
        this.state.debugPrint(
            String.format("Updating replica clock for received clock %s", request.getReplicaTSList())
        );
        this.state.updateStableOps();
        PropagateStateResponse response = PropagateStateResponse.getDefaultInstance();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
