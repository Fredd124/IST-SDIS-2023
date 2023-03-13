package pt.tecnico.distledger.namingserver;

import java.util.HashMap;
import java.util.Map;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class NamingServer {

    public static void main(String[] args) {

        final int PORT = 5001;
        Map<String, ServiceEntry> services = new HashMap<>();
        boolean debug = false;
        /* for (int i = 2; i < args.length; i++) {
            if (args[i].equals("-debug")) {
                debug = true;
            }
            else {
                System.err.println(String.format("Invalid argument : %s .", args[i]));
            }
        } */
        // Create a new server to listen on port
		Server server = ServerBuilder.forPort(PORT).build();

    }

}
