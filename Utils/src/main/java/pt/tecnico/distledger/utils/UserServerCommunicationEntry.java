package pt.tecnico.distledger.utils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

public class UserServerCommunicationEntry {

    private ManagedChannel channel;
    private UserServiceGrpc.UserServiceBlockingStub stub;
    
    public UserServerCommunicationEntry(String address) {
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        stub = UserServiceGrpc.newBlockingStub(channel);
    }

    public UserServiceGrpc.UserServiceBlockingStub getStub() {
        return this.stub;
    }

    public void shutdown() {
        channel.shutdown();
    }
}
