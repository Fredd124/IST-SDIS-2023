package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.exceptions.AlreadyActiveException;
import pt.tecnico.distledger.server.domain.exceptions.NotActiveException;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.QualifierAdressPair;
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
        List<String> targetQualifiers = Utils.lookupOnDns(dnsStub, "").stream()
            .map(response -> response.getQualifier())
            .collect(Collectors.toList());
        targetQualifiers.stream()
            .filter(qualifier -> !qualifier.equals(state.getQualifier().toString()))
            .forEach(qualifier -> 
                propagateToSecondary(state.getLedgerState()
                        .stream()
                        .collect(Collectors.toList()), 
                    qualifier));
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

    private void propagateToSecondary(List<Operation> ops, String qualifier) {
        if (ops.size() == 0) {
            this.state.debugPrint("No operations to propagate.");
            return;
        }
        try {
            if (! serverCache.distLedgerHasEntry(qualifier)) {
                state.debugPrint(String.format("Sending lookup request to service %s", SERVICE_NAME));
                List<QualifierAdressPair> result =  Utils.lookupOnDns(dnsStub, qualifier);
                if (result.size() == 0) {
                    state.debugPrint(
                        String.format("No server found for qualifier %s.", qualifier)
                    );
                }
                String address = result.get(0).getAddress();
                state.debugPrint("Got server address: " + address);
                serverCache.addEntry(qualifier, address);
                state.debugPrint(String.format("Added B qualifier to server cache."));
            }
            List<Integer> replicaTS = this.state.getReplicaVectorClock();
            DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub
                 = serverCache.distLedgerGetEntry(qualifier).getStub();
            DistLedgerCommonDefinitions.LedgerState ledgerState 
                = DistLedgerCommonDefinitions.LedgerState.newBuilder()
                .addAllLedger(
                    ops.stream()
                    .filter(op -> !Utils.isSmallerVectorClock(op.getTimeStamp(), replicaTS))
                    .map(operation -> Converter.convertToGrpc(operation)).collect(Collectors.toList())
                ).build();
                PropagateStateRequest propagateRequest = PropagateStateRequest.newBuilder()
                    .setState(ledgerState).addAllReplicaTS(this.state.getReplicaVectorClock()).build();
                state.debugPrint("Sending propagate request");
                stub.propagateState(propagateRequest);
                state.debugPrint(
                    String.format("Propagated successfully %d operations.", ops.size())
                );
        }         
        catch (StatusRuntimeException e ) {
            serverCache.removeEntry(qualifier);
            state.debugPrint("Propagate failed for server in cache.");
        }
    }

    public void shutdownChannels() {
        serverCache.shutdown();
        dnsChannel.shutdown();
    }

}
