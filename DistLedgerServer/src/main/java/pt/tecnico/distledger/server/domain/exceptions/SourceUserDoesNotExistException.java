package pt.tecnico.distledger.server.domain.exceptions;

public class SourceUserDoesNotExistException extends ServerStateException {
    public SourceUserDoesNotExistException() {
        super("The source user account provided does not exist");
    }
}
