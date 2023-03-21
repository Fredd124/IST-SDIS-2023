package pt.tecnico.distledger.namingserver;

import java.io.IOException;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class NamingServer {

    public static void main(String[] args) {

        final int PORT = 5001;
        boolean debug = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-debug")) {
                debug = true;
            }
            else {
                System.err.println(String.format("Invalid argument : %s .", args[i]));
            }
        }
        // Create a new server to listen on port
        final BindableService dnsImpl = new NamingServerServiceImpl(debug);
		Server server = ServerBuilder.forPort(PORT).addService(dnsImpl).build();
        
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
