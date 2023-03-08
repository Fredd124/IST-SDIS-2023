package pt.tecnico.distledger.server.domain.exceptions;

public class ServerStateException extends Exception{

    public ServerStateException (String errorMessage) {
        super(errorMessage);
    }

}
