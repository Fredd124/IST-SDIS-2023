package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;

public class AdminClientMain {
    public static void main(String[] args) {
        boolean debug = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-debug")) {
                debug = true;
            }
            else {
                System.err.println(String.format("Invalid argument : %s .", args[i]));
            }
        }

        final String NAMING_SERVER_TARGET = "localhost:5001";
        CommandParser parser = new CommandParser(new AdminService(NAMING_SERVER_TARGET, debug));
        parser.parseInput();

    }
}
