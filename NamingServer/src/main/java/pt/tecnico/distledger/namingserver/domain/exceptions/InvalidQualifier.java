package pt.tecnico.distledger.namingserver.domain.exceptions;

public class InvalidQualifier extends ServerStateException {
    public InvalidQualifier() {
        super("Invalid qualifier");
    }
}
