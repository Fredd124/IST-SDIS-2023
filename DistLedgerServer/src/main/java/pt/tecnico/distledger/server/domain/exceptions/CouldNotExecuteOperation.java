package pt.tecnico.distledger.server.domain.exceptions;

public class CouldNotExecuteOperation extends ServerStateException {
    public CouldNotExecuteOperation() {
        super("Could not apply operation to server");
    }
}
