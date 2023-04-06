package pt.tecnico.distledger.namingserver;

import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceImplBase;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.RegisterRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.RegisterResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.QualifierAdressPair;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.DeleteRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.DeleteResponse;
import pt.tecnico.distledger.namingserver.domain.exceptions.ServerStateException;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;
import static java.util.stream.Collectors.toList;

import pt.tecnico.distledger.namingserver.domain.ServerState;

public class NamingServerServiceImpl extends NamingServerServiceImplBase {
    
    private ServerState state;

    public NamingServerServiceImpl(boolean isDebug) {
        this.state = new ServerState(isDebug);
    }

    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        String serviceName = request.getName();
        String qualifier = request.getQualifier();
        String address = request.getAddress();

        try {
            state.debugPrint(
                String.format(
                    "Received register request with parameters : name=%s qualifier=%s address%s",
                    serviceName, qualifier, address
                )
            );
            state.registerService(serviceName, qualifier, address);
            RegisterResponse response = RegisterResponse.newBuilder().build();
            state.debugPrint("Registered successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ServerStateException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(
                INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        String serviceName = request.getName();
        String qualifier = request.getQualifier();
        state.debugPrint(
            String.format(
                "Received lookup request for service with name %s and qualifier %s"
                , serviceName, qualifier
            )
        );
        LookupResponse response = LookupResponse.newBuilder().addAllServers(
            state.lookupService(serviceName, qualifier).stream().map(entry -> 
                QualifierAdressPair.newBuilder().setAddress(entry.getAddress()).setQualifier(entry.getQualifier()).build()).collect(toList())).
            build();
        state.debugPrint(String.format("Lookup result : %s", response.toString()));
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        String serviceName = request.getName();
        String address = request.getAddress();
        try {
            state.debugPrint(
                String.format(
                    "Received delete entry request for name %s and address %s", 
                    serviceName, address
                )
            );
            state.deleteService(serviceName, address);
            state.debugPrint("Deleted successfully");
            DeleteResponse response = DeleteResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ServerStateException e) {
            state.debugPrint(
                    String.format("Threw exception : %s .", e.getMessage()));
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
   }
}
