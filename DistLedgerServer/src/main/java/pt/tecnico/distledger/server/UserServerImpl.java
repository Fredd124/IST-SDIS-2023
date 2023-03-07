package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc.UserServiceImplBase;
import pt.tecnico.distledger.server.domain.ErrorMessage;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;

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
        if (state.getActive() == false) {
            state.debugPrint(String.format("Threw exception : %s .",
                    ErrorMessage.SERVER_NOT_ACTIVE));
            responseObserver.onError(
                    INVALID_ARGUMENT.withDescription("Server is not active")
                            .asRuntimeException());
        }
        // check if userId exists
        else if (!state.containsUser(userId)) {
            state.debugPrint(String.format("Threw exception : %s .",
                    ErrorMessage.USER_DOES_NOT_EXIST));
            responseObserver.onError(
                    INVALID_ARGUMENT.withDescription("User does not exist")
                            .asRuntimeException());
        }
        else {
            int balance = state.getBalance(userId);
            state.debugPrint(String.format(
                    "Returning balance for user %s : %d .", userId, balance));
            BalanceResponse response = BalanceResponse.newBuilder()
                    .setValue(balance).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void createAccount(CreateAccountRequest request,
            StreamObserver<CreateAccountResponse> responseObserver) {
        String userId = request.getUserId();
        state.debugPrint(String.format(
                "Received create account request from userId : %s .", userId));
        if (state.getActive() == false) {
            state.debugPrint(String.format("Threw exception : %s .",
                    ErrorMessage.SERVER_NOT_ACTIVE));
            responseObserver.onError(
                    INVALID_ARGUMENT.withDescription("Server is not active")
                            .asRuntimeException());
        }
        // check if userId exists
        else if (state.containsUser(userId)) {
            state.debugPrint(String.format("Threw exception : %s .",
                    ErrorMessage.USER_ALREADY_EXISTS));
            responseObserver.onError(
                    INVALID_ARGUMENT.withDescription("User already exists")
                            .asRuntimeException());
        }
        else {
            state.createAccount(userId);
            state.debugPrint(
                    String.format("Created account for user %s .", userId));
            CreateAccountResponse response = CreateAccountResponse.newBuilder()
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request,
            StreamObserver<DeleteAccountResponse> responseObserver) {
        String userId = request.getUserId();
        state.debugPrint(String.format(
                "Received delete account request from userId : %s .", userId));
        if (state.getActive() == false) {
            state.debugPrint(String.format("Threw exception : %s .",
                    ErrorMessage.SERVER_NOT_ACTIVE));
            responseObserver.onError(
                    INVALID_ARGUMENT.withDescription("Server is not active")
                            .asRuntimeException());
        }
        // check if userId exists
        else if (!state.containsUser(userId)) {
            state.debugPrint(String.format("Threw exception : %s .",
                    ErrorMessage.USER_DOES_NOT_EXIST));
            responseObserver.onError(
                    INVALID_ARGUMENT.withDescription("User does not exist")
                            .asRuntimeException());
        }
        else {
            state.deleteAccount(userId);
            state.debugPrint(
                    String.format("Deleted account for user %s .", userId));
            DeleteAccountResponse response = DeleteAccountResponse.newBuilder()
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void transferTo(TransferToRequest request,
            StreamObserver<TransferToResponse> responseObserver) {
        String fromUserId = request.getAccountFrom();
        String toUserId = request.getAccountTo();
        int value = request.getAmount();

        if (state.getActive() == false) {
            state.debugPrint(String.format("Threw exception : %s .",
                    ErrorMessage.SERVER_NOT_ACTIVE));
            responseObserver.onError(
                    INVALID_ARGUMENT.withDescription("Server is not active")
                            .asRuntimeException());
        }
        // check if fromUserId exists
        else if (!state.containsUser(fromUserId)) {
            state.debugPrint(String.format("Threw exception : %s .",
                    ErrorMessage.SOURCE_USER_DOES_NOT_EXIST));
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription("Origin user does not exist")
                    .asRuntimeException());
        }
        // check if toUserId exists
        else if (!state.containsUser(toUserId)) {
            state.debugPrint(String.format("Threw exception : %s .",
                    ErrorMessage.DESTINATION_USER_DOES_NOT_EXIST));
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription("Destination user does not exist")
                    .asRuntimeException());
        }
        // check if fromUserId has enough balance
        else if (state.getBalance(fromUserId) < value) {
            state.debugPrint(String.format("Threw exception : %s .",
                    ErrorMessage.INVALID_BALANCE_AMOUNT));
            responseObserver.onError(INVALID_ARGUMENT.withDescription(
                    "The owner of the account does not have enough balance")
                    .asRuntimeException());
        }
        else {
            state.transfer(fromUserId, toUserId, value);
            state.debugPrint(String.format(
                    "Transfered %d from account of user %s to account of user %s .",
                    value, fromUserId, toUserId));
            TransferToResponse response = TransferToResponse.newBuilder()
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

}
