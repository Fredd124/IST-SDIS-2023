package pt.tecnico.distledger.userclient.grpc;

import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceBlockingStub;
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
    private ManagedChannel dnsChannel;
    private UserServiceGrpc.UserServiceBlockingStub stub;
    private NamingServerServiceBlockingStub dnsStub;
    private boolean debug;

    /*TODO: The gRPC client-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service. */

    public UserService(String target, boolean debug) {
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.dnsChannel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();
        this.stub = UserServiceGrpc.newBlockingStub(channel);
        this.dnsStub = NamingServerServiceGrpc.newBlockingStub(dnsChannel);
        this.debug = debug;
        debugPrint("Created user service.");
    }

    public void lookupService(String serviceName, String qualifier) {
        LookupRequest request = LookupRequest.newBuilder().setName(serviceName).setQualifier(qualifier).build();
        LookupResponse response = dnsStub.lookup(request);
        System.out.println(response.getServersCount());
    }

    public void userServiceChannelShutdown() {
        debugPrint("Shut down client channel.");
        channel.shutdownNow();
    }

    public void createAccount(String server, String username) {
        CreateAccountRequest request = CreateAccountRequest.newBuilder().setUserId(username).build(); 

        try {
            debugPrint("Send create account request to server.");
            stub.createAccount(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void deleteAccount(String server, String username) {
        DeleteAccountRequest request = DeleteAccountRequest.newBuilder().setUserId(username).build();

        try {
            debugPrint("Send delete account request to server.");
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
            debugPrint("Send get balance request to server.");
            response = stub.balance(request);
            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void transferTo(String server, String from, String dest, Integer amount) {
        TransferToRequest request = TransferToRequest.newBuilder().setAccountFrom(from).setAccountTo(dest).setAmount(amount).build();
        
        try {
            debugPrint("Send transfer to account request to server.");
            stub.transferTo(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void debugPrint(String message) {
        if (debug) System.err.println(message);
    }
}
