package pt.tecnico.distledger.server;

import java.io.IOException;
import java.util.Scanner;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.utils.DistLedgerServerCache;
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
        if (args[1].length() != 1) {
            System.err.println("Invalid qualifier. Qualifier needs to have length=1 .");
            System.exit(1);
        }
        final Character qualifier = args[1].charAt(0);
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
            .setQualifier(qualifier.toString()).build();
        try {
            dnsStub.register(request);
        }
        catch (StatusRuntimeException e) {
            dnsChannel.shutdown();
            if (debug == true) {
                System.err.println(String.format("Caugth exception : %s .", e.getMessage()));
                System.err.println("Exitting server...");
            }
            System.exit(1);
        }
        ServerState state = new ServerState(debug, address , qualifier);
        DistLedgerServerCache serverCache = new DistLedgerServerCache();
        final AdminServerImpl adminImpl = new AdminServerImpl(state, serverCache);
        final BindableService userImpl = new UserServerImpl(state);
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
                cleanServer(adminImpl, crossServerImpl, dnsStub, dnsChannel, server, address);
            }
        });
        while(! exit) {
            String line = scanner.nextLine();
            if (line.equals("")) exit = true;
        }
        scanner.close();
        cleanServer(adminImpl, crossServerImpl, dnsStub, dnsChannel, server, address);
    }

    private static void cleanServer(AdminServerImpl adminImpl, CrossServerImpl crossServerImpl,
        NamingServerServiceBlockingStub dnsStub, ManagedChannel dnsChannel, Server server, String address) {
            if (server.isShutdown()) return;
            DeleteRequest deleteRequest = DeleteRequest.newBuilder().
                setAddress(address).setName(SERVICE_NAME).build();
            dnsStub.delete(deleteRequest);
            dnsChannel.shutdown();
            adminImpl.shutdownChannels();
            server.shutdown();
    }

}

