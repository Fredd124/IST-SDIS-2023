package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;

import java.util.Scanner;

public class CommandParser {

    private static final String SPACE = " ";
    private static final String ACTIVATE = "activate";
    private static final String DEACTIVATE = "deactivate";
    private static final String GET_LEDGER_STATE = "getLedgerState";
    private static final String GOSSIP = "gossip";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

    private final AdminService adminService;
    public CommandParser(AdminService adminService) {
        this.adminService = adminService;
    }
    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String cmd = line.split(SPACE)[0];

            switch (cmd) {
                case ACTIVATE:
                    adminService.debugPrint("Received activate command from input");
                    this.activate(line);
                    break;

                case DEACTIVATE:
                    adminService.debugPrint("Received deactivate command from input");
                    this.deactivate(line);
                    break;

                case GET_LEDGER_STATE:
                    adminService.debugPrint("Received getLedgerState command from input");
                    this.dump(line);
                    break;

                case GOSSIP:
                    adminService.debugPrint("Received gossip command from input");
                    this.gossip(line);
                    break;

                case HELP:
                    adminService.debugPrint("Received help command from input");
                    this.printUsage();
                    break;

                case EXIT:
                    adminService.debugPrint("Received exit command from input");
                    scanner.close();
                    this.shutdown();
                    exit = true;
                    break;

                default:
                    adminService.debugPrint("No command found, printing usage");
                    this.printUsage();
                    break;
            }
        }
    }

    private void activate(String line){
        String[] split = line.split(SPACE);

        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        adminService.debugPrint("Calling adminService adctivate method");
        adminService.activate(server);
    }

    private void deactivate(String line){
        String[] split = line.split(SPACE);

        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        adminService.debugPrint("Calling adminService deactivate method");
        adminService.deactivate(server);
    }

    private void dump(String line){
        String[] split = line.split(SPACE);

        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        adminService.debugPrint("Calling adminService getLedgerState method");
        adminService.getLedgerState(server);
    }

    private void gossip(String line){
        String[] split = line.split(SPACE);

        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        adminService.debugPrint("Calling adminService getLedgerState method");
        adminService.gossip(server);
    }
    
    private void printUsage() {
        System.out.println("Usage:\n" +
                "- activate <server>\n" +
                "- deactivate <server>\n" +
                "- getLedgerState <server>\n" +
                "- gossip <server>\n" +
                "- exit\n");
    }

    private void shutdown() {
        adminService.debugPrint("Calling adminService shutdown method");
        adminService.shutdown();
    }

}
