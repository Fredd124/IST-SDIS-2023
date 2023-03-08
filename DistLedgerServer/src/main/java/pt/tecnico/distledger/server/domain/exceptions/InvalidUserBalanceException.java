package pt.tecnico.distledger.server.domain.exceptions;

public class InvalidUserBalanceException extends ServerStateException {
    public InvalidUserBalanceException() {
        super("The source user account provided does not have enough balance");
    }
    
}
