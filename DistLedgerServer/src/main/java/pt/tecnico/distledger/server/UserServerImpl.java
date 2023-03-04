package pt.tecnico.distledger.server;

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

public class UserServerImpl extends UserServiceImplBase{
    
    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> streamObserver) {

    } 

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> streamObserver) {

    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> streamObserver) {

    }

    @Override
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> streamObserver) {
        
    }

}
