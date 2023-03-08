package pt.tecnico.distledger.server.domain.exceptions;

public class NotActiveException extends ServerStateException {
    public NotActiveException() {
        super("The server provided is not active");
    }
}

