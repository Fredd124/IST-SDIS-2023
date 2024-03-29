package pt.tecnico.distledger.userclient.grpc;

import pt.tecnico.distledger.utils.UserServerCache;
import pt.tecnico.distledger.utils.Utils;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.QualifierAdressPair;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import static io.grpc.Status.UNAVAILABLE;

import java.util.List;
import java.util.ArrayList;

public class UserService {

    private ManagedChannel dnsChannel;
    private UserServerCache serverCache;
    private NamingServerServiceBlockingStub dnsStub;
    private boolean debug;
    private List<Integer> vectorClock = new ArrayList<>();

    public UserService(String target, boolean debug) {
        this.dnsChannel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.dnsStub = NamingServerServiceGrpc.newBlockingStub(dnsChannel);
        serverCache = new UserServerCache();
        this.debug = debug; 
        for (int i = 0; i < 3; i++) {
            vectorClock.add(0);
        }
        debugPrint("Created user service.");
    }

    public boolean lookupService(String qualifier) {
        debugPrint(String.format("Sending lookup request to server DistLedger, to lookup for server with qualifier %s .", qualifier));
        List<QualifierAdressPair> result = Utils.lookupOnDns(dnsStub, qualifier);
        if (result.size() == 0) {
            System.out.println("No server found for qualifier " + qualifier);
            return false;
        }
        String address = result.get(0).getAddress();
        debugPrint(String.format("Received lookup response from server with servers list %s .", result.toString()));
        serverCache.addEntry(qualifier, address);
        return true;
    }

    public void namingServerServiceChannelShutdown() {
        debugPrint("Shut down client channel.");
        dnsChannel.shutdown();
        serverCache.shutdownServers();
    }

    public void createAccount(String qualifier, String username) {
        try {
            if (!serverCache.userHasEntry(qualifier) && !lookupService(qualifier)) {
                debugPrint(String.format("No server found on lookup for qualifier %s .", qualifier));
                System.out.println("No server found for qualifier " + qualifier);
                return;
            } 
            UserServiceGrpc.UserServiceBlockingStub stub = serverCache.userGetEntry(qualifier).getStub();            
            CreateAccountRequest request = CreateAccountRequest.newBuilder().setUserId(username).addAllPrevTS(vectorClock).build(); 
            CreateAccountResponse response;
            debugPrint(String.format("Sent create account request to server %s with username %s as argument.",qualifier, username));
            debugPrint(String.format("Sent vector clock %s from server.", vectorClock.toString()));
            response = stub.createAccount(request);
            System.out.println("OK");
            for (int i = 0; i < vectorClock.size(); i++) {
                vectorClock.set(i, Math.max(vectorClock.get(i), response.getTSList().get(i)));
            }
            debugPrint(String.format("Received vector clock %s from server.", vectorClock.toString()));
        } catch (StatusRuntimeException e) {
            debugPrint(
                    String.format("Caugth exception : %s .", e.getMessage()));
            System.out.println(e.getStatus().getDescription());
            if (e.getStatus().equals(UNAVAILABLE)) {
                serverCache.removeEntry(qualifier);
            }
        }   
    }

    public void balance(String qualifier, String username) {
        try {
            if (!serverCache.userHasEntry(qualifier) && !lookupService(qualifier)) {
                debugPrint(String.format("No server found on lookup for qualifier %s .", qualifier));
                System.out.println("No server found for qualifier " + qualifier);
                return;
            } 
            UserServiceGrpc.UserServiceBlockingStub stub = serverCache.userGetEntry(qualifier).getStub();            
            BalanceRequest request = BalanceRequest.newBuilder().setUserId(username).addAllPrevTS(vectorClock).build();
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
            if (!serverCache.userHasEntry(qualifier) && !lookupService(qualifier)) {
                debugPrint(String.format("No server found on lookup for qualifier %s .", qualifier));
                System.out.println("No server found for qualifier " + qualifier);
                return;
            } 
            UserServiceGrpc.UserServiceBlockingStub stub = serverCache.userGetEntry(qualifier).getStub();            
            TransferToRequest request = TransferToRequest.newBuilder().setAccountFrom(from).setAccountTo(dest).setAmount(amount).addAllPrevTS(vectorClock).build();
            TransferToResponse response;
            debugPrint(String.format("Sent transferTo request to server %s with from %s, dest %s and amount %d as arguments.",qualifier, from, dest, amount));
            debugPrint(String.format("Sent vector clock %s from server.", vectorClock.toString()));
            response = stub.transferTo(request);
            System.out.println("OK");   
            for (int i = 0; i < vectorClock.size(); i++) {
                vectorClock.set(i, Math.max(vectorClock.get(i), response.getTSList().get(i)));
            }            
            debugPrint(String.format("Received vector clock %s from server.", vectorClock.toString()));
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
