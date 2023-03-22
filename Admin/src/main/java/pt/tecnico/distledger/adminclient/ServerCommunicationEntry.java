package pt.tecnico.distledger.adminclient;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

public class ServerCommunicationEntry {
    
    private ManagedChannel channel;
    private AdminServiceGrpc.AdminServiceBlockingStub stub;

    public ServerCommunicationEntry(String address) {
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        stub = AdminServiceGrpc.newBlockingStub(channel);
    }

    public AdminServiceGrpc.AdminServiceBlockingStub getStub() {
        return stub;
    }

    public void shutdown() {
        channel.shutdownNow();
    }
}
