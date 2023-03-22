package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.grpc.UserService;


public class UserClientMain {
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
        UserService service = new UserService(NAMING_SERVER_TARGET, debug);
        CommandParser parser = new CommandParser(service);
        parser.parseInput();
    }
}
