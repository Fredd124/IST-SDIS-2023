package pt.tecnico.distledger.server.domain.operation;

import java.lang.UnsupportedOperationException;

public class Operation {
    private String account;
    private boolean stable;

    public Operation(String fromAccount) {
        this.account = fromAccount;
        this.stable = false;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public boolean isStable() {
        return stable;
    }

    public void setStable(boolean stable) {
        this.stable = stable;
    }
    
    public String getType(){
        throw new UnsupportedOperationException();
    }

}
