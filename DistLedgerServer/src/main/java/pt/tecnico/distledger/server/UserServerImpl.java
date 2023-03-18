package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.exceptions.NotActiveException;
import pt.tecnico.distledger.server.domain.exceptions.ServerStateException;
import pt.tecnico.distledger.server.domain.operation.Converter;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc.UserServiceImplBase;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.UNAVAILABLE;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class UserServerImpl extends UserServiceImplBase {

    private ServerState state;

    public UserServerImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void balance(BalanceRequest request,
            StreamObserver<BalanceResponse> responseObserver) {
        String userId = request.getUserId();
        state.debugPrint(String.format(
                "Received get balance request from userId : %s .", userId));
        try {
            int balance = state.getBalance(userId);
            state.debugPrint(String.format(
                    "Returning balance for user %s : %d .", userId, balance));
            BalanceResponse response = BalanceResponse.newBuilder()
                    .setValue(balance).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (NotActiveException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage())
                    .asRuntimeException());
        }
        catch (ServerStateException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void createAccount(CreateAccountRequest request,
            StreamObserver<CreateAccountResponse> responseObserver) {
        String userId = request.getUserId();
        state.debugPrint(String.format(
                "Received create account request from userId : %s .", userId));
        try {
            state.createAccount(userId);
            state.debugPrint(
                    String.format("Created account for user %s .", userId));
            propagateToSecondary();
            CreateAccountResponse response = CreateAccountResponse.newBuilder()
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
        catch (ServerStateException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request,
            StreamObserver<DeleteAccountResponse> responseObserver) {
        String userId = request.getUserId();
        state.debugPrint(String.format(
                "Received delete account request from userId : %s .", userId));
        try {
            state.deleteAccount(userId);
            state.debugPrint(
                    String.format("Deleted account for user %s .", userId));
            propagateToSecondary();
            DeleteAccountResponse response = DeleteAccountResponse.newBuilder()
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
        catch (ServerStateException e) {
            state.debugPrint(String.format("Threw exception : %s .",
                    e.getMessage()));
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(
                            e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void transferTo(TransferToRequest request,
            StreamObserver<TransferToResponse> responseObserver) {
        String fromUserId = request.getAccountFrom();
        String toUserId = request.getAccountTo();
        int value = request.getAmount();
        state.debugPrint(String.format(
                "Received transfer request from %s to %s, with amout of %d .",
                fromUserId, toUserId, value));
        try {
            state.transfer(fromUserId, toUserId, value);
            state.debugPrint(String.format(
                    "Transfered %d from account of user %s to account of user %s .",
                    value, fromUserId, toUserId));
            propagateToSecondary();
            TransferToResponse response = TransferToResponse.newBuilder()
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
        catch (ServerStateException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(e.getMessage()).asRuntimeException());
        }
    }

    void propagateToSecondary() {
        LookupRequest request = LookupRequest.newBuilder().setName("DistLedger").setQualifier("B").build();
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();
        NamingServerServiceGrpc.NamingServerServiceBlockingStub dnsStub = NamingServerServiceGrpc.newBlockingStub(channel);
        LookupResponse response = dnsStub.lookup(request);
        channel.shutdown();
        String address = response.getServers(0); // TODO : implement for other cases 
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub 
            = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
        DistLedgerCommonDefinitions.LedgerState ledgerState 
            = DistLedgerCommonDefinitions.LedgerState.newBuilder()
            .addAllLedger(
                state.getLedgerState().stream()
                .map(op -> Converter.convertToGrpc(op)).toList()
            ).build();
        PropagateStateRequest propagateRequest = PropagateStateRequest.newBuilder().setState(ledgerState).build();
        stub.propagateState(propagateRequest);

    }

}
