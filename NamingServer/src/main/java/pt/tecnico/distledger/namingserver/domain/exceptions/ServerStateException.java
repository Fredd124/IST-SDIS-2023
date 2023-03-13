package pt.tecnico.distledger.namingserver.domain.exceptions;

public class ServerStateException extends Exception{

    public ServerStateException (String errorMessage) {
        super(errorMessage);
    }

}
