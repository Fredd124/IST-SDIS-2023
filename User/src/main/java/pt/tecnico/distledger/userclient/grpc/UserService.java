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

public class UserService {

    private ManagedChannel channel, dnsChannel;
    private UserServiceGrpc.UserServiceBlockingStub stub;
    private NamingServerServiceBlockingStub dnsStub;
    private boolean debug;

    public UserService(String target, boolean debug) {
        this.dnsChannel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.dnsStub = NamingServerServiceGrpc.newBlockingStub(dnsChannel);
        this.debug = true; 
        debugPrint("Created user service.");
    }

    public void lookupService(String qualifier) {
        try {
            LookupRequest request = LookupRequest.newBuilder().setName("DistLedger").setQualifier(qualifier).build();
            debugPrint(String.format("Sent lookup request to server DistLedger %s .", qualifier));
            LookupResponse response = dnsStub.lookup(request);
            debugPrint(String.format("Received lookup response from server with servers list %s .", response.getServersList().toString()));
            String address = response.getServers(0);
            this.channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
            this.stub = UserServiceGrpc.newBlockingStub(channel);
        } catch (IndexOutOfBoundsException e) {
            debugPrint(
                    String.format("Caught exception : %s .", e.getMessage()));
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
            debugPrint(String.format("Sent create account request to server with username %s as argument.", username));
            stub.createAccount(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            debugPrint(
                    String.format("Caugth exception : %s .", e.getMessage()));
            System.out.println(e.getStatus().getDescription());
        }
        finally {
            channel.shutdown();
        }
    }

    public void deleteAccount(String qualifier, String username) {
        try {
            lookupService(qualifier);

            DeleteAccountRequest request = DeleteAccountRequest.newBuilder().setUserId(username).build();
            debugPrint(String.format("Sent delete account request to server with username %s as argument.", username));
            stub.deleteAccount(request);
            System.out.println("OK");
            channel.shutdown();
        } catch (StatusRuntimeException e) {
            debugPrint(
                    String.format("Caught exception : %s .", e.getMessage()));
            System.out.println(e.getStatus().getDescription());
        }
        finally {
            channel.shutdown();
        }
    }

    public void balance(String qualifier, String username) {
        try {
            lookupService(qualifier);
            BalanceRequest request = BalanceRequest.newBuilder().setUserId(username).build();
            BalanceResponse response;

            debugPrint(String.format("Sent balance request to server with username %s as argument.", username));
            response = stub.balance(request);
            debugPrint(String.format("Received balance response from server with balance %d .", response.getValue()));
            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            debugPrint(
                    String.format("Caught exception : %s .", e.getMessage()));
            System.out.println(e.getStatus().getDescription());
        }
        finally {
            channel.shutdown();
        }
    }

    public void transferTo(String qualifier, String from, String dest, Integer amount) {        
        try {
            lookupService(qualifier);
            TransferToRequest request = TransferToRequest.newBuilder().setAccountFrom(from).setAccountTo(dest).setAmount(amount).build();

            debugPrint(String.format("Sent transferTo request to server with from %s, dest %s and amount %d as arguments.", from, dest, amount));
            stub.transferTo(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            debugPrint(
                    String.format("Caught exception : %s .", e.getMessage()));
            System.out.println(e.getStatus().getDescription());
        }
        finally {
            channel.shutdown();
        }
    }

    public void debugPrint(String message) {
        if (debug) System.err.println(message);
    }
}
