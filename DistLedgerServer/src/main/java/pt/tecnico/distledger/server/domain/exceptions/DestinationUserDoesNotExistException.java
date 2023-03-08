package pt.tecnico.distledger.server.domain.exceptions;

public class DestinationUserDoesNotExistException extends ServerStateException {
    public DestinationUserDoesNotExistException() {
        super("The destination user acccount provided does not exist");
    }
}
