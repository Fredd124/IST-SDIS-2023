package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.exceptions.NotActiveException;
import pt.tecnico.distledger.server.domain.exceptions.NotUpToDateException;
import pt.tecnico.distledger.server.domain.exceptions.ServerStateException;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc.UserServiceImplBase;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.UNAVAILABLE;
import static io.grpc.Status.CANCELLED;

import java.util.List;
import java.util.ArrayList;

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
            int balance = state.balance(userId, request.getPrevTSList());
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
        catch (NotUpToDateException e) {
                state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(CANCELLED.withDescription(e.getMessage())
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
        Operation done = null;
        try {
            done = state.createAccount(userId, null);
            state.debugPrint(
                    String.format("Created operation to create account for user %s .", userId));
            state.debugPrint("Adding operation to ledger.");
            List<Integer> copyClientVectorClock = new ArrayList<Integer>();
            request.getPrevTSList().forEach(value -> copyClientVectorClock.add(value));
            state.frontendRequest(done, copyClientVectorClock);
            CreateAccountResponse response = CreateAccountResponse.newBuilder()
                    .addAllTS(copyClientVectorClock)
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
    public void transferTo(TransferToRequest request,
            StreamObserver<TransferToResponse> responseObserver) {
        String fromUserId = request.getAccountFrom();
        String toUserId = request.getAccountTo();
        int value = request.getAmount();
        state.debugPrint(String.format(
                "Received transfer request from %s to %s, with amout of %d .",
                fromUserId, toUserId, value));
        Operation done = null;
        try {
            done = state.transfer(fromUserId, toUserId, value, null);
            state.debugPrint(String.format(
                    "Transfered %d from account of user %s to account of user %s .",
                    value, fromUserId, toUserId));
            state.debugPrint("Adding operation to ledger.");
            List<Integer> copyClientVectorClock = new ArrayList<Integer>();
            request.getPrevTSList().forEach(clock -> copyClientVectorClock.add(clock));
            state.frontendRequest(done, copyClientVectorClock);
            TransferToResponse response = TransferToResponse.newBuilder()
                    .addAllTS(copyClientVectorClock).build();
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

}
