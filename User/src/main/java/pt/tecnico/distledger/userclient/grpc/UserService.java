package pt.tecnico.distledger.userclient.grpc;

import pt.ulisboa.tecnico.distledger.contract.user.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import io.grpc.StatusRuntimeException;

//One method for both stub and channel creation?

public class UserService {

    private ManagedChannel channel;
    private UserServiceGrpc.UserServiceBlockingStub stub;

    /*TODO: The gRPC client-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service. */

    public UserService(String target) {
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = UserServiceGrpc.newBlockingStub(channel);
    }

    public void userServiceChannelShutdown() {
        channel.shutdownNow();
    }

    /* 
        public ManagedChannel creatChannel(String target) {
            // Channel is the abstraction to connect to a service endpoint.
            // Let us use plaintext communication because we do not have certificates.
            final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    
            return channel;
        }
    
        public UserServiceGrpc.UserServiceBlockingStub createStub(ManagedChannel channel) {
            // It is up to the client to determine whether to block the call.
            // Here we create a blocking stub, but an async stub,
            // or an async stub with Future are always possible.
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        
            return stub;
        }        
    */

    public void createAccount(String server, String username) {
        CreateAccountRequest request = CreateAccountRequest.newBuilder().setUserId(username).build(); 

        try {
            stub.createAccount(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void deleteAccount(String server, String username) {
        DeleteAccountRequest request = DeleteAccountRequest.newBuilder().setUserId(username).build();

        try {
            stub.deleteAccount(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void balance(String server, String username) {
        BalanceRequest request = BalanceRequest.newBuilder().setUserId(username).build();
        BalanceResponse response;

        try {
            response = stub.balance(request);
            System.out.println("OK");
            System.out.println(response.getValue());
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void transferTo(String server, String from, String dest, Integer amount) {
        TransferToRequest request = TransferToRequest.newBuilder().setAccountFrom(from).setAccountTo(dest).setAmount(amount).build();
        
        try {
            stub.transferTo(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

}
