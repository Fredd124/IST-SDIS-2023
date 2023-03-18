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

    private ManagedChannel channel, dnsChannel;
    private UserServiceGrpc.UserServiceBlockingStub stub;
    private NamingServerServiceBlockingStub dnsStub;
    private boolean debug;

    public UserService(String target, boolean debug) {
        this.dnsChannel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.dnsStub = NamingServerServiceGrpc.newBlockingStub(dnsChannel);
        this.debug = debug;
        debugPrint("Created user service.");
    }

    public void lookupService(String qualifier) {
        try {
            LookupRequest request = LookupRequest.newBuilder().setName("DistLedger").setQualifier(qualifier).build();
            LookupResponse response = dnsStub.lookup(request);
            String address = response.getServers(0);
            this.channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
            this.stub = UserServiceGrpc.newBlockingStub(channel);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("No server found for qualifier " + qualifier);
        }
    }

    public void namingServerServiceChannelShutdown() {
        debugPrint("Shut down client channel.");
        dnsChannel.shutdownNow();
    }

    public void createAccount(String qualifier, String username) {
        try {
            lookupService(qualifier);
            CreateAccountRequest request = CreateAccountRequest.newBuilder().setUserId(username).build(); 

            debugPrint("Send create account request to server.");
            stub.createAccount(request);
            System.out.println("OK");
            channel.shutdownNow();
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void deleteAccount(String qualifier, String username) {
        try {
            lookupService(qualifier);
            DeleteAccountRequest request = DeleteAccountRequest.newBuilder().setUserId(username).build();

            debugPrint("Send delete account request to server.");
            stub.deleteAccount(request);
            System.out.println("OK");
            channel.shutdownNow();
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void balance(String qualifier, String username) {
        try {
            lookupService(qualifier);
            BalanceRequest request = BalanceRequest.newBuilder().setUserId(username).build();
            BalanceResponse response;

            debugPrint("Send get balance request to server.");
            response = stub.balance(request);
            System.out.println("OK");
            System.out.println(response);
            channel.shutdownNow();
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void transferTo(String qualifier, String from, String dest, Integer amount) {        
        try {
            lookupService(qualifier);
            TransferToRequest request = TransferToRequest.newBuilder().setAccountFrom(from).setAccountTo(dest).setAmount(amount).build();

            debugPrint("Send transfer to account request to server.");
            stub.transferTo(request);
            System.out.println("OK");
            channel.shutdownNow();
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void debugPrint(String message) {
        if (debug) System.err.println(message);
    }
}
