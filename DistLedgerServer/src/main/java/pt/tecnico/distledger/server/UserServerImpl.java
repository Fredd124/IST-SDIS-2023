package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
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


public class UserServerImpl extends UserServiceImplBase{
    
    private ServerState state;

    public UserServerImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        //TODO : add logic to this operation
        String userId = request.getUserId();

        //check if userId exists
        if(!state.containsUser(userId)){
            responseObserver.onError(INVALID_ARGUMENT.withDescription("User does not exist").asRuntimeException());
        }

        else{
            int balance = state.getBalance(userId);
            BalanceResponse response = BalanceResponse.newBuilder().setValue(balance).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    } 

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        //TODO : add logic to this operation
        String userId = request.getUserId();

        //check if userId exists
        if(state.containsUser(userId)){
            responseObserver.onError(INVALID_ARGUMENT.withDescription("User already exists").asRuntimeException());
        }
        else{
            state.createAccount(userId);
            CreateAccountResponse response = CreateAccountResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {
        //TODO : add logic to this operation
        String userId = request.getUserId();

        //check if userId exists
        if(!state.containsUser(userId)){
            responseObserver.onError(INVALID_ARGUMENT.withDescription("User does not exist").asRuntimeException());
        }
        else{
            state.deleteAccount(userId);
            DeleteAccountResponse response = DeleteAccountResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
        //TODO : add logic to this operation
        String fromUserId = request.getAccountFrom();
        String toUserId = request.getAccountTo();
        int value = request.getAmount();

        //check if fromUserId exists
        if(!state.containsUser(fromUserId)){
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Origin user does not exist").asRuntimeException());
        }
        //check if toUserId exists
        else if(!state.containsUser(toUserId)){
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Destination user does not exist").asRuntimeException());
        }
        //check if fromUserId has enough balance
        else if(state.getBalance(fromUserId) < value){
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Origin user does not have enough balance").asRuntimeException());
        }
        else{
            state.transfer(fromUserId, toUserId, value);
            TransferToResponse response = TransferToResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

}
