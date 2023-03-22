package pt.tecnico.distledger.server.domain.exceptions;

public class NotWritableException extends Exception {
    public NotWritableException() {
        super("The server provided is not writable");
    }
}
