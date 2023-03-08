package pt.tecnico.distledger.server.domain.exceptions;

public class UserAlreadyExistsEception extends ServerStateException {
    public UserAlreadyExistsEception() {
        super("The user account provided already exists");
    }
}
