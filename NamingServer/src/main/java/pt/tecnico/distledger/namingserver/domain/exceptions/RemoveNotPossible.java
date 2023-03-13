package pt.tecnico.distledger.namingserver.domain.exceptions;

public class RemoveNotPossible extends ServerStateException {
    public RemoveNotPossible() {
        super("Not possible to remove the server");
    }
}
