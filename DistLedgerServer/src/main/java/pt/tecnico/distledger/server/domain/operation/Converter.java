package pt.tecnico.distledger.server.domain.operation;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

public class Converter {
    public static DistLedgerCommonDefinitions.Operation convert(Operation operation){
        String type = operation.getType();
        switch (type){
            case ("OP_TRANSFER_TO"):
                return DistLedgerCommonDefinitions.Operation.newBuilder()
                        .setType(DistLedgerCommonDefinitions.OperationType.OP_TRANSFER_TO)
                        .setUserId(operation.getAccount())
                        .setDestUserId(((TransferOp) operation).getDestAccount())
                        .setAmount(((TransferOp) operation).getAmount()).build();
            case ("OP_CREATE_ACCOUNT"):
                return DistLedgerCommonDefinitions.Operation.newBuilder()
                        .setType(DistLedgerCommonDefinitions.OperationType.OP_CREATE_ACCOUNT)
                        .setUserId(operation.getAccount()).build();
            case ("OP_DELETE_ACCOUNT"):
                return DistLedgerCommonDefinitions.Operation.newBuilder()
                        .setType(DistLedgerCommonDefinitions.OperationType.OP_DELETE_ACCOUNT)
                        .setUserId(operation.getAccount()).build();
            default:
                return null;
        }

    }
}
