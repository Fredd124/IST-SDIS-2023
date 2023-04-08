package pt.tecnico.distledger.server.domain.exceptions;

public class NotUpToDateException extends Exception {
    public NotUpToDateException() {
        super("The server is not up to date to answer to read");
    }
}
