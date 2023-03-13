package pt.tecnico.distledger.namingserver.domain.exceptions;

public class RegisterNotPossible extends ServerStateException {
    public RegisterNotPossible() {
        super("Not possible to register the server");
    }
}
