package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.grpc.UserService;
import java.util.Scanner;


public class CommandParser {

    private static final String SPACE = " ";
    private static final String CREATE_ACCOUNT = "createAccount";
    private static final String DELETE_ACCOUNT = "deleteAccount";
    private static final String TRANSFER_TO = "transferTo";
    private static final String BALANCE = "balance";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

    private final UserService userService;

    public CommandParser(UserService userService) {
        this.userService = userService;
    }

    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String cmd = line.split(SPACE)[0];

            try{
                switch (cmd) {
                    case CREATE_ACCOUNT:
                        userService.debugPrint("Received from input createAccount command.");
                        this.createAccount(line);
                        break;

                    case DELETE_ACCOUNT:
                        userService.debugPrint("Received from input deleteAccount command.");
                        this.deleteAccount(line);
                        break;

                    case TRANSFER_TO:
                        userService.debugPrint("Received from input transferTo command.");
                        this.transferTo(line);
                        break;

                    case BALANCE:
                        userService.debugPrint("Received from input balance command.");
                        this.balance(line);
                        break;

                    case HELP:
                        userService.debugPrint("Received from input help command.");
                        this.printUsage();
                        break;

                    case EXIT:
                        userService.debugPrint("Received from input exit command.");
                        exit = true;
                        userService.userServiceChannelShutdown();
                        break;

                    default:
                        this.printUsage();
                        break;
                }
            }
            catch (Exception e){
                System.err.println(e.getMessage());
            }
        }
    }

    private void createAccount(String line){
        String[] split = line.split(SPACE);

        if (split.length != 3){
            this.printUsage();
            return;
        }

        String server = split[1];
        String username = split[2];

        userService.debugPrint("Called userService createAccount method.");
        userService.createAccount(server, username);
    }

    private void deleteAccount(String line){
        String[] split = line.split(SPACE);

        if (split.length != 3){
            this.printUsage();
            return;
        }
        String server = split[1];
        String username = split[2];

        userService.debugPrint("Called userService deleteAccount method.");
        userService.deleteAccount(server, username);
    }


    private void balance(String line){
        String[] split = line.split(SPACE);

        if (split.length != 3){
            this.printUsage();
            return;
        }
        String server = split[1];
        String username = split[2];

        userService.debugPrint("Called userService balance method.");
        userService.balance(server, username);
    }

    private void transferTo(String line){
        String[] split = line.split(SPACE);

        if (split.length != 5){
            this.printUsage();
            return;
        }
        String server = split[1];
        String from = split[2];
        String dest = split[3];
        Integer amount = Integer.valueOf(split[4]);

        userService.debugPrint("Called userService transferTo method.");
        userService.transferTo(server, from, dest, amount);
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                        "- createAccount <server> <username>\n" +
                        "- deleteAccount <server> <username>\n" +
                        "- balance <server> <username>\n" +
                        "- transferTo <server> <username_from> <username_to> <amount>\n" +
                        "- exit\n");
    }
}
