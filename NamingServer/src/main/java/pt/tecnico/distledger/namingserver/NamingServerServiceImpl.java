package pt.tecnico.distledger.namingserver;

import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceImplBase;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.RegisterRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.RegisterResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.DeleteRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.DeleteResponse;
import pt.tecnico.distledger.namingserver.domain.exceptions.ServerStateException;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;

import pt.tecnico.distledger.namingserver.domain.ServerState;

public class NamingServerServiceImpl extends NamingServerServiceImplBase {
    
    private ServerState state;

    public NamingServerServiceImpl() {
        this.state = new ServerState();
    }

    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        String serviceName = request.getName();
        String qualifier = request.getQualifier();
        String address = request.getAddress();

        try {
            state.registerService(serviceName, qualifier, address);
            RegisterResponse response = RegisterResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ServerStateException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        String serviceName = request.getName();
        String qualifier = request.getQualifier();
        LookupResponse response = LookupResponse.newBuilder().addAllServers(
            state.lookupService(serviceName, qualifier).stream().map(entry -> entry.getQualifier()).toList()).
            build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        String serviceName = request.getName();
        String address = request.getAddress();

        try {
            state.deleteService(serviceName, address);
            DeleteResponse response = DeleteResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ServerStateException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
        
    }

}
