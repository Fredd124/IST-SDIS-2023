package pt.tecnico.distledger.server;

import java.io.IOException;
import java.util.Scanner;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.DeleteRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.RegisterRequest;
public class ServerMain {

    private final static String SERVICE_NAME = "DistLedger";
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

        final String NAMING_SERVER_TARGET = "localhost:5001";
        ManagedChannel dnsChannel = ManagedChannelBuilder.forTarget(NAMING_SERVER_TARGET).usePlaintext().build();
        NamingServerServiceBlockingStub dnsStub = NamingServerServiceGrpc.newBlockingStub(dnsChannel);
        final String address = "localhost:" + port;
        RegisterRequest request = RegisterRequest.newBuilder()
            .setAddress(address)
            .setName(SERVICE_NAME)
            .setQualifier(qualifier).build();
        dnsStub.register(request);
        ServerState state = new ServerState(debug, address , qualifier);
        ServerCache serverCache = new ServerCache();
        final BindableService adminImpl = new AdminServerImpl(state);
        final UserServerImpl userImpl = new UserServerImpl(state, serverCache);
        final CrossServerImpl crossServerImpl = new CrossServerImpl(state, serverCache);
        

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
		System.out.println("Server started, press Enter to exit server");

		// Do not exit the main thread. Wait until server is terminated.
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                cleanServer(userImpl, crossServerImpl, dnsStub, dnsChannel, server, address);
            }
        });
        while(! exit) {
            String line = scanner.nextLine();
            if (line.equals("")) exit = true;
        }
        scanner.close();
        cleanServer(userImpl, crossServerImpl, dnsStub, dnsChannel, server, address);
    }

    private static void cleanServer(UserServerImpl userImpl, CrossServerImpl crossServerImpl,
        NamingServerServiceBlockingStub dnsStub, ManagedChannel dnsChannel, Server server, String address) {
            DeleteRequest deleteRequest = DeleteRequest.newBuilder().
                setAddress(address).setName(SERVICE_NAME).build();
            dnsStub.delete(deleteRequest);
            dnsChannel.shutdown();
            userImpl.shutdownChannels();
            crossServerImpl.shutdownChannel();
            server.shutdown();
    }

}

