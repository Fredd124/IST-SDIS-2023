package pt.tecnico.distledger.utils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

public class DistLedgerServerCommunicationEntry {
    
    private ManagedChannel channel;
    private DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub;
    
    public DistLedgerServerCommunicationEntry(String address) {
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
    }

    public DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub getStub() {
        return stub;
    }

    public void shutdown() {
        channel.shutdownNow();
    }
}


