package pt.tecnico.distledger.utils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

public class AdminServerCommunicationEntry {
    
    private ManagedChannel channel;
    private AdminServiceGrpc.AdminServiceBlockingStub stub;
    
    public AdminServerCommunicationEntry(String address) {
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        stub = AdminServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        channel.shutdownNow();
    }

    public AdminServiceGrpc.AdminServiceBlockingStub getStub() {
        return stub;
    }
}

