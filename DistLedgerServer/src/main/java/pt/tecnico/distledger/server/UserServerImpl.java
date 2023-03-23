package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.exceptions.NotActiveException;
import pt.tecnico.distledger.server.domain.exceptions.NotWritableException;
import pt.tecnico.distledger.server.domain.exceptions.ServerStateException;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.utils.ServerCache;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateOperationRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupResponse;
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
import static io.grpc.Status.UNAVAILABLE;
import static io.grpc.Status.ABORTED;

import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class UserServerImpl extends UserServiceImplBase {

    private ServerState state;
	private ServerCache serverCache;
    private ManagedChannel dnsChannel;
    private NamingServerServiceGrpc.NamingServerServiceBlockingStub dnsStub;
    private final String NAMING_SERVER_TARGET = "localhost:5001";
    private final String SERVICE_NAME = "DistLedger";
    private final String SECONDARY_SERVER_QUALIFIER = "B";

    public UserServerImpl(ServerState state, ServerCache serverCache) {
        this.state = state;
        this.serverCache = serverCache;
        dnsChannel = ManagedChannelBuilder.forTarget(NAMING_SERVER_TARGET).usePlaintext().build();
        dnsStub = NamingServerServiceGrpc.newBlockingStub(dnsChannel);
    }

    private boolean propagateToSecondary(Operation op) {
        try {
            if (! serverCache.distLedgerHasEntry(SECONDARY_SERVER_QUALIFIER)) {
                state.debugPrint("Doing lookup");
                LookupRequest request = LookupRequest.newBuilder().setName(SERVICE_NAME)
                    .setQualifier(SECONDARY_SERVER_QUALIFIER).build();
                LookupResponse response = dnsStub.lookup(request);
                if (response.getServersCount() == 0) {
                    state.debugPrint("No servers found. Propagate request failed.");
                    return false;
                }
                String address = response.getServers(0);
                state.debugPrint("Got server address: " + address);
                serverCache.addEntry(SECONDARY_SERVER_QUALIFIER, address);
                state.debugPrint(String.format("Added B qualifier to server cache."));
            }
            DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub
                 = serverCache.distLedgerGetEntry(SECONDARY_SERVER_QUALIFIER).getStub();
            try {
                DistLedgerCommonDefinitions.Operation operation = Converter.convertToGrpc(op);
                PropagateOperationRequest propagateOperationRequest = PropagateOperationRequest.newBuilder()
                    .setOperation(operation).build();
                state.debugPrint("Sending propagate operation request");
                stub.propagateOperation(propagateOperationRequest);
                state.debugPrint("Propagated operation successfully");
            } catch (StatusRuntimeException e) {
                List<Operation> ledger = new ArrayList<>(state.getLedgerState());
                ledger.add(op);
                DistLedgerCommonDefinitions.LedgerState ledgerState 
                = DistLedgerCommonDefinitions.LedgerState.newBuilder()
                .addAllLedger(
                    ledger.stream()
                    .map(operation -> Converter.convertToGrpc(operation)).collect(Collectors.toList())
                ).build();
                PropagateStateRequest propagateRequest = PropagateStateRequest.newBuilder()
                    .setState(ledgerState).build();
                state.debugPrint("Sending propagate request");
                stub.propagateState(propagateRequest);
                state.debugPrint("Propagated successfully");
            }
            return true;
        }         
        catch (StatusRuntimeException e ) {
            serverCache.removeEntry(SECONDARY_SERVER_QUALIFIER);
            state.debugPrint("Propagate failed for server in cache.");
            return false;
        }
    }

    @Override
    public void balance(BalanceRequest request,
            StreamObserver<BalanceResponse> responseObserver) {
        String userId = request.getUserId();
        state.debugPrint(String.format(
                "Received get balance request from userId : %s .", userId));
        try {
            int balance = state.getBalance(userId);
            state.debugPrint(String.format(
                    "Returning balance for user %s : %d .", userId, balance));
            BalanceResponse response = BalanceResponse.newBuilder()
                    .setValue(balance).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (NotActiveException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage())
                    .asRuntimeException());
        }
        catch (ServerStateException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void createAccount(CreateAccountRequest request,
            StreamObserver<CreateAccountResponse> responseObserver) {
        String userId = request.getUserId();
        state.debugPrint(String.format(
                "Received create account request from userId : %s .", userId));
        Operation done = null;
        try {
            state.canWrite();
            done = state.createAccount(userId);
            state.debugPrint(
                    String.format("Created operation to create account for user %s .", userId));
			if (!propagateToSecondary(done)) {
                responseObserver.onError(UNAVAILABLE.withDescription("The secondary server is not available.")
                    .asRuntimeException());
                return;
            }
            state.debugPrint("Performing operation.");
            state.doOp(done);
            CreateAccountResponse response = CreateAccountResponse.newBuilder()
                        .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (NotWritableException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(ABORTED.withDescription(e.getMessage())
                    .asRuntimeException());
        }
        catch (NotActiveException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage())
                    .asRuntimeException());
        }
        catch (ServerStateException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request,
            StreamObserver<DeleteAccountResponse> responseObserver) {
        String userId = request.getUserId();
        state.debugPrint(String.format(
                "Received delete account request from userId : %s .", userId));
        Operation done = null;
        try {
            state.canWrite();
            done = state.deleteAccount(userId);
            state.debugPrint(
                    String.format("Created operation to delete account for user %s .", userId));
            if (!propagateToSecondary(done)) {
                responseObserver.onError(UNAVAILABLE.withDescription("The secondary server is not available.")
                    .asRuntimeException());
                return;
            }
            state.debugPrint("Performing operation.");
            state.doOp(done);
            DeleteAccountResponse response = DeleteAccountResponse.newBuilder()
                        .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (NotWritableException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(ABORTED.withDescription(e.getMessage())
                    .asRuntimeException());
        }
        catch (NotActiveException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage())
                    .asRuntimeException());
        }
        catch (ServerStateException e) {
            state.debugPrint(String.format("Threw exception : %s .",
                    e.getMessage()));
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(
                            e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void transferTo(TransferToRequest request,
            StreamObserver<TransferToResponse> responseObserver) {
        String fromUserId = request.getAccountFrom();
        String toUserId = request.getAccountTo();
        int value = request.getAmount();
        state.debugPrint(String.format(
                "Received transfer request from %s to %s, with amout of %d .",
                fromUserId, toUserId, value));
        Operation done = null;
        try {
            state.canWrite();
            done = state.transfer(fromUserId, toUserId, value);
            state.debugPrint(String.format(
                    "Transfered %d from account of user %s to account of user %s .",
                    value, fromUserId, toUserId));
            if (!propagateToSecondary(done)) {
                responseObserver.onError(UNAVAILABLE.withDescription("The secondary server is not available.")
                    .asRuntimeException());
                return;
            }
            state.debugPrint("Performing operation.");
            state.doOp(done);
            TransferToResponse response = TransferToResponse.newBuilder()
                        .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (NotWritableException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(ABORTED.withDescription(e.getMessage())
                    .asRuntimeException());
        }
        catch (NotActiveException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage())
                    .asRuntimeException());
        }
        catch (ServerStateException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription(e.getMessage()).asRuntimeException());
        }
    }

    public void shutdownChannels() {
        dnsChannel.shutdown();
        serverCache.shutdownServers();
    }
}
