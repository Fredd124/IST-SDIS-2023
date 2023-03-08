package pt.tecnico.distledger.server.domain.exceptions;

public class BalanceNotZeroException extends ServerStateException {
    public BalanceNotZeroException() {
        super("In order to delete an account, the account's balance must be zero");
    }
}
