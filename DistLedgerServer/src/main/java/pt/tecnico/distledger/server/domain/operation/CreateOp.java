package pt.tecnico.distledger.server.domain.operation;

public class CreateOp extends Operation {

    public CreateOp(String account) {
        super(account);
    }

    @Override
    public String getType(){
        return "OP_CREATE_ACCOUNT";
    }
}
