package pt.tecnico.distledger.server;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub;

public class ServerCommunicationEntry {
    
    private ManagedChannel channel;
    private DistLedgerCrossServerServiceBlockingStub stub;

    public ServerCommunicationEntry(String address) {
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
    }

    public DistLedgerCrossServerServiceBlockingStub getStub() {
        return stub;
    }

    public void shutdown() {
        channel.shutdown();
    }
}
