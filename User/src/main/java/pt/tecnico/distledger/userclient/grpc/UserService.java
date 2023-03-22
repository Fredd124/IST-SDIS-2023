package pt.tecnico.distledger.userclient.grpc;

import pt.tecnico.distledger.userclient.ServerCache;
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
import static io.grpc.Status.UNAVAILABLE;


public class UserService {

    private ManagedChannel dnsChannel;
    private ServerCache serverCache;
    private NamingServerServiceBlockingStub dnsStub;
    private boolean debug;
    private final String DNS_SERVER = "DistLedger";

    public UserService(String target, boolean debug) {
        this.dnsChannel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.dnsStub = NamingServerServiceGrpc.newBlockingStub(dnsChannel);
        serverCache = new ServerCache();
        this.debug = debug; 
        debugPrint("Created user service.");
    }

    public void lookupService(String qualifier) {
        try {
            LookupRequest request = LookupRequest.newBuilder().setName(DNS_SERVER).setQualifier(qualifier).build();
            debugPrint(String.format("Sent lookup request to server DistLedger, to lookup for server with qualifier %s .", qualifier));
            LookupResponse response = dnsStub.lookup(request);
            debugPrint(String.format("Received lookup response from server with servers list %s .", response.getServersList().toString()));
            String address = response.getServers(0);
            serverCache.addEntry(qualifier, address);
        } catch (IndexOutOfBoundsException e) {
            debugPrint(
                    String.format("Caught exception : %s .", e.getMessage()));
            System.out.println("No server found for qualifier " + qualifier);
        } 
    }

    public void namingServerServiceChannelShutdown() {
        debugPrint("Shut down client channel.");
        dnsChannel.shutdownNow();
        serverCache.shutdownServers();
    }

    public void createAccount(String qualifier, String username) {
        try {
            if (!serverCache.hasEntry(qualifier)) {
                lookupService(qualifier);
            } 
            UserServiceGrpc.UserServiceBlockingStub stub = serverCache.getEntry(qualifier).getStub();            
            CreateAccountRequest request = CreateAccountRequest.newBuilder().setUserId(username).build(); 
            debugPrint(String.format("Sent create account request to server %s with username %s as argument.",qualifier, username));
            stub.createAccount(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            debugPrint(
                    String.format("Caugth exception : %s .", e.getMessage()));
            System.out.println(e.getStatus().getDescription());
            if (e.getStatus().equals(UNAVAILABLE)) {
                serverCache.removeEntry(qualifier);
            }
        }   
    }

    public void deleteAccount(String qualifier, String username) {
        try {
            if (!serverCache.hasEntry(qualifier)) {
                lookupService(qualifier);
            } 
            UserServiceGrpc.UserServiceBlockingStub stub = serverCache.getEntry(qualifier).getStub();            
            DeleteAccountRequest request = DeleteAccountRequest.newBuilder().setUserId(username).build();
            debugPrint(String.format("Sent delete account request to server %s with username %s as argument.",qualifier, username));
            stub.deleteAccount(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            debugPrint(
                    String.format("Caught exception : %s .", e.getMessage()));
            System.out.println(e.getStatus().getDescription());
            if (e.getStatus().equals(UNAVAILABLE)) {
                serverCache.removeEntry(qualifier);
            }
        }   
    }

    public void balance(String qualifier, String username) {
        try {
            if (!serverCache.hasEntry(qualifier)) {
                lookupService(qualifier);
            } 
            UserServiceGrpc.UserServiceBlockingStub stub = serverCache.getEntry(qualifier).getStub();            
            BalanceRequest request = BalanceRequest.newBuilder().setUserId(username).build();
            BalanceResponse response;    
            debugPrint(String.format("Sent balance request to server  %s with username %s as argument.",qualifier, username));
            response = stub.balance(request);
            debugPrint(String.format("Received balance response from server with balance %d .", response.getValue()));
            System.out.println("OK");
            System.out.print(response);
        } catch (StatusRuntimeException e) {
            debugPrint(
                    String.format("Caught exception : %s .", e.getMessage()));
            System.out.println(e.getStatus().getDescription());
            if (e.getStatus().equals(UNAVAILABLE)) {
                serverCache.removeEntry(qualifier);
            }
        }   
    }

    public void transferTo(String qualifier, String from, String dest, Integer amount) {        
        try {
            if (!serverCache.hasEntry(qualifier)) {
                lookupService(qualifier);
            } 
            UserServiceGrpc.UserServiceBlockingStub stub = serverCache.getEntry(qualifier).getStub();            
            TransferToRequest request = TransferToRequest.newBuilder().setAccountFrom(from).setAccountTo(dest).setAmount(amount).build();
            debugPrint(String.format("Sent transferTo request to server %s with from %s, dest %s and amount %d as arguments.",qualifier, from, dest, amount));
            stub.transferTo(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            debugPrint(
                    String.format("Caught exception : %s .", e.getMessage()));
            System.out.println(e.getStatus().getDescription());
            if (e.getStatus().equals(UNAVAILABLE)) {
                serverCache.removeEntry(qualifier);
            }
        }   
    }

    public void debugPrint(String message) {
        if (debug) System.err.println(message);
    }
}
