package pt.tecnico.distledger.server.domain.operation;

import java.util.List;

public class CreateOp extends Operation {

    public CreateOp(String account, List<Integer> timeStamp) {
        super(account, timeStamp);
    }

    @Override
    public String getType(){
        return "OP_CREATE_ACCOUNT";
    }
}
