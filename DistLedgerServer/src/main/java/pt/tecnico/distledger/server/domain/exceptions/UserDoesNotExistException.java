package pt.tecnico.distledger.server.domain.exceptions;

public class UserDoesNotExistException extends ServerStateException {
    public UserDoesNotExistException() {
        super("The user account provided does not exist");
    }
}

