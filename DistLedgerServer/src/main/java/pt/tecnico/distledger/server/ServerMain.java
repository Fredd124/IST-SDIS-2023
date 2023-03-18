package pt.tecnico.distledger.server;

import java.io.IOException;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.RegisterRequest;
public class ServerMain {

    public static void main(String[] args) {

        // receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments
		if (args.length < 2) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", ServerMain.class.getName());
			return;
		}

		final int port = Integer.parseInt(args[0]);
        final String qualifier = args[1];
        boolean debug = false;
        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("-debug")) {
                debug = true;
            }
            else {
                System.err.println(String.format("Invalid argument : %s .", args[i]));
            }
        }

        ManagedChannel dnsChannel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();
        NamingServerServiceBlockingStub dnsStub = NamingServerServiceGrpc.newBlockingStub(dnsChannel);
        RegisterRequest request = RegisterRequest.newBuilder()
            .setAddress("localhost:" + port)
            .setName("DistLedger")
            .setQualifier(qualifier).build();
        dnsStub.register(request);
        ServerState state = new ServerState(debug);
        final BindableService adminImpl = new AdminServerImpl(state);
        final BindableService userImpl = new UserServerImpl(state);
        final BindableService crossServerImpl = new CrossServerImpl(state);
        

        // Create a new server to listen on port
		Server server = ServerBuilder.forPort(port).addService(adminImpl).addService(userImpl)
        .addService(crossServerImpl).build();

        // Start the server
        try {
            server.start();
        }
        catch (IOException e) {
            System.out.println("Exception on bind");
        }
		// Server threads are running in the background.
		System.out.println("Server started");

		// Do not exit the main thread. Wait until server is terminated.
        try {
            server.awaitTermination();
        }
        catch (InterruptedException e) {
            System.out.println("Exception on close");
        }
    }

}

