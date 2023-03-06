package pt.tecnico.distledger.server.domain.operation;

import java.lang.UnsupportedOperationException;

public class Operation {
    private String account;

    public Operation(String fromAccount) {
        this.account = fromAccount;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getType(){
        throw new UnsupportedOperationException();
    }

}
