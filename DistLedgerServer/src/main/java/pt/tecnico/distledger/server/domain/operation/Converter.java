package pt.tecnico.distledger.server.domain.operation;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

public class Converter {
    private DistLedgerCommonDefinitions.Operation messageOp;

    public Converter(Operation operation){
        String type = operation.getType();
        switch (type){
            case ("OP_TRANSFER_TO"):
                this.messageOp = DistLedgerCommonDefinitions.Operation.newBuilder()
                        .setType(DistLedgerCommonDefinitions.OperationType.OP_TRANSFER_TO)
                        .setUserId(operation.getAccount())
                        .setDestUserId(((TransferOp) operation).getDestAccount())
                        .setAmount(((TransferOp) operation).getAmount()).build();
                break;
            case ("OP_CREATE_ACCOUNT"):
                this.messageOp = DistLedgerCommonDefinitions.Operation.newBuilder()
                        .setType(DistLedgerCommonDefinitions.OperationType.OP_CREATE_ACCOUNT)
                        .setUserId(operation.getAccount()).build();
                break;
            case ("OP_DELETE_ACCOUNT"):
                this.messageOp = DistLedgerCommonDefinitions.Operation.newBuilder()
                        .setType(DistLedgerCommonDefinitions.OperationType.OP_DELETE_ACCOUNT)
                        .setUserId(operation.getAccount()).build();
                break;
        }

    }

    public DistLedgerCommonDefinitions.Operation getMessageOp(){
        return this.messageOp;
    }
}
