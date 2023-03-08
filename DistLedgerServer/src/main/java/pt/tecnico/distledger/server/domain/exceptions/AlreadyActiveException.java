package pt.tecnico.distledger.server.domain.exceptions;

public class AlreadyActiveException  extends ServerStateException {
    public AlreadyActiveException() {
        super("The server provided is not active");
    }  
}
