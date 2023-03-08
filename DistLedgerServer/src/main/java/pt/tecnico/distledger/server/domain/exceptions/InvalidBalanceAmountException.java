package pt.tecnico.distledger.server.domain.exceptions;

public class InvalidBalanceAmountException extends ServerStateException {
    public InvalidBalanceAmountException() {
        super("The balance amount provided must greater than 0");
    }
}
