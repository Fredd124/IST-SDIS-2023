package pt.tecnico.distledger.server;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateResponse;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Converter;
import pt.tecnico.distledger.server.domain.operation.Operation;

import io.grpc.stub.StreamObserver;

import java.util.List;

public class CrossServerImpl extends DistLedgerCrossServerServiceImplBase {

    private ServerState state;
    
    public CrossServerImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {
        
        DistLedgerCommonDefinitions.LedgerState state = request.getState();
        List<Operation> ops = state.getLedgerList().stream().map(op -> Converter.convertFromGrpc(op)).toList();
        this.state.setLedgerState(ops);
    }
}
