package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

public class Converter {
    public static DistLedgerCommonDefinitions.Operation convertToGrpc(Operation operation) {
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
                return DistLedgerCommonDefinitions.Operation.newBuilder()
                .setType(OperationType.OP_UNSPECIFIED).build();
        }
    }

    public static Operation convertFromGrpc(DistLedgerCommonDefinitions.Operation operation) {
        DistLedgerCommonDefinitions.OperationType type = operation.getType();
        switch(type) {
            case OP_CREATE_ACCOUNT:
                return new CreateOp(operation.getUserId());
            case OP_DELETE_ACCOUNT:
                return new DeleteOp(operation.getUserId());
            case OP_TRANSFER_TO:
                return new TransferOp(operation.getUserId(), operation.getDestUserId(), operation.getAmount());
            case OP_UNSPECIFIED:
                return null;
            default:
                return null;
        }
    }
}
