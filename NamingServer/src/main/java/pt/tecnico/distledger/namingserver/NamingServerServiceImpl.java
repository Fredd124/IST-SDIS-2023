import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceImplBase;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.RegisterRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.RegisterResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.RemovalRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.RemovalResponse;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;

import pt.tecnico.distledger.namingserver.domain.ServerState;

public class NamingServerServiceImpl {
    
    private ServerState state;

    public NamingServerServiceImpl() {
        this.state = new ServerState();
    }

    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        String serviceName = request.getName();
        String qualifier = request.getQualifier();
        String address = request.getAddress();
        state.registerService(serviceName, qualifier, address);
        RegisterResponse response = RegisterResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        // TODO : create Exceptions (server ja existe com outro qualificador, mesmo qualificador e address diff
    }

    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        String serviceName = request.getName();
        String qualifier = request.getQualifier();
        state.lookupService(serviceName, qualifier);
        RegisterResponse response = RegisterResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void remove(RemovalRequest request, StreamObserver<RemovalResponse> responseObserver) {
        
    }

}
