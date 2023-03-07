package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc.AdminServiceImplBase;
import pt.tecnico.distledger.server.domain.operation.Converter;

import java.io.Console;
import java.util.ArrayList;

import io.grpc.stub.StreamObserver;

public class AdminServerImpl extends AdminServiceImplBase {
    
    ServerState state;

    public AdminServerImpl(ServerState state) {
        this.state = state;
    }
    
    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {
        state.activate();
        ActivateResponse response = ActivateResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {
        state.deactivate();
        DeactivateResponse response = DeactivateResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver) {
        // Only for 3rd delivery
    }

    @Override
    public void getLedgerState(getLedgerStateRequest request, StreamObserver<getLedgerStateResponse> responseObserver) {
        ArrayList<DistLedgerCommonDefinitions.Operation> ledgerState = new ArrayList<DistLedgerCommonDefinitions.Operation>();
        System.out.println(state.getLedgerState().size());
        for (Operation op: state.getLedgerState()) {
            Converter converter = new Converter(op);
            DistLedgerCommonDefinitions.Operation operation = converter.getMessageOp();
            System.out.println(operation);
            ledgerState.add(operation);
        }
        DistLedgerCommonDefinitions.LedgerState ledgerStateGrpc = DistLedgerCommonDefinitions.LedgerState.newBuilder()
                                                                    .addAllLedger(ledgerState).build();
        
        getLedgerStateResponse response = getLedgerStateResponse.newBuilder()
                                            .setLedgerState(ledgerStateGrpc).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
