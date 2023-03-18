package pt.tecnico.distledger.adminclient.grpc.exceptions;

public class NoServerFoundException extends Exception{
    
    public NoServerFoundException(String message) {
        super(message);
    }
}
