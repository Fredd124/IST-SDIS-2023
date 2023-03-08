package pt.tecnico.distledger.server.domain.exceptions;

public class SourceEqualsDestinationUserException extends ServerStateException {
    public SourceEqualsDestinationUserException() {
        super("The source user account must be diferent from the destination user account");
    }
}
