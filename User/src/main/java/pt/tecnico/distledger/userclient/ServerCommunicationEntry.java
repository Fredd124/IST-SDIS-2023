package pt.tecnico.distledger.userclient;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

public class ServerCommunicationEntry {
    
    private ManagedChannel channel;
    private UserServiceGrpc.UserServiceBlockingStub stub;

    public ServerCommunicationEntry(String address) {
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        stub = UserServiceGrpc.newBlockingStub(channel);
    }

    public UserServiceGrpc.UserServiceBlockingStub getStub() {
        return stub;
    }

    public void shutdown() {
        channel.shutdownNow();
    }
}
