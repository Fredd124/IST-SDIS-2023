package pt.tecnico.distledger.server.domain.exceptions;

public class BrokerCantBeDeletedException extends ServerStateException {
    public BrokerCantBeDeletedException() {
        super("The broker account can not be deleted");
    }
}
