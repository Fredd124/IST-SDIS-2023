package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.grpc.UserService;


public class UserClientMain {
    public static void main(String[] args) {

        System.out.println(UserClientMain.class.getSimpleName());

        boolean debug = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-debug")) {
                debug = true;
            }
            else {
                System.err.println(String.format("Invalid argument : %s .", args[i]));
            }
        }
    
        final String namingServerTarget = "localhost:5001";
        UserService service = new UserService(namingServerTarget, debug);
        CommandParser parser = new CommandParser(service);
        parser.parseInput();
    }
}
