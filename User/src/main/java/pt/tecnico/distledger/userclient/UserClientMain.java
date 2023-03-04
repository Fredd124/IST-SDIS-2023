package pt.tecnico.distledger.userclient;

import io.grpc.ManagedChannel;
import pt.tecnico.distledger.userclient.grpc.UserService;
import pt.ulisboa.tecnico.distledger.contract.user.*;

public class UserClientMain {
    public static void main(String[] args) {

        System.out.println(UserClientMain.class.getSimpleName());

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // check arguments
        if (args.length != 2) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<host> <port>");
            return;
        }

        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        final String target = host + ":" + port;

        UserService service = new UserService();
        CommandParser parser = new CommandParser(service);

        ManagedChannel channel = service.creatChannel(target);
        UserServiceGrpc.UserServiceBlockingStub stub = service.createStub(channel);
        parser.parseInput(stub);

        channel.shutdownNow();
    }
}
