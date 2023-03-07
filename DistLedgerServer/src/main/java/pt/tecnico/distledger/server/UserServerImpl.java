package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ErrorMessage;
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

        if (!state.getActive()) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(ErrorMessage.SERVER_NOT_ACTIVE.label)
                    .asRuntimeException());
        }
        else if (!state.containsUser(userId)) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(ErrorMessage.USER_DOES_NOT_EXIST.label)
                    .asRuntimeException());
        }
        else {
            int balance = state.getBalance(userId);
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

        if (!state.getActive()) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(ErrorMessage.SERVER_NOT_ACTIVE.label)
                    .asRuntimeException());
        }
        else if (state.containsUser(userId)) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(ErrorMessage.USER_ALREADY_EXISTS.label)
                    .asRuntimeException());
        }
        else {
            state.createAccount(userId);
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

        if (!state.getActive()) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(ErrorMessage.SERVER_NOT_ACTIVE.label)
                    .asRuntimeException());
        }
        else if (userId == "broker") {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(
                            ErrorMessage.BROKER_CAN_NOT_BE_DELETED.label)
                    .asRuntimeException());
        }
        else if (!state.containsUser(userId)) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(ErrorMessage.USER_DOES_NOT_EXIST.label)
                    .asRuntimeException());
        }
        else {
            state.deleteAccount(userId);
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

        if (!state.getActive()) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(ErrorMessage.SERVER_NOT_ACTIVE.label)
                    .asRuntimeException());
        }
        else if (!state.containsUser(fromUserId)) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(
                            ErrorMessage.SOURCE_USER_DOES_NOT_EXIST.label)
                    .asRuntimeException());
        }
        else if (!state.containsUser(toUserId)) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(
                            ErrorMessage.DESTINATION_USER_DOES_NOT_EXIST.label)
                    .asRuntimeException());
        }
        else if (state.getBalance(fromUserId) < value) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(ErrorMessage.INVALID_USER_BALANCE.label)
                    .asRuntimeException());
        }
        else if (value <= 0) {
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(ErrorMessage.INVALID_BALANCE_AMOUNT.label)
                    .asRuntimeException());
        }
        else {
            state.transfer(fromUserId, toUserId, value);
            TransferToResponse response = TransferToResponse.newBuilder()
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

}
