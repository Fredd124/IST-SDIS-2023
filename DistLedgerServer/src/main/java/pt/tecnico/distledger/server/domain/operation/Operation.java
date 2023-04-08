package pt.tecnico.distledger.server.domain.operation;

import java.lang.UnsupportedOperationException;
import java.util.List;

public class Operation {
    private String account;
    private boolean stable;
    private List<Integer> timeStamp;

    public Operation(String fromAccount, List<Integer> timeStamp) {
        this.account = fromAccount;
        this.stable = false;
        this.timeStamp = timeStamp;
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
    
    public List<Integer> getTimeStamp() {
        return this.timeStamp;
    }
    
    public void setTimeStamp(List<Integer> timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getType(){
        throw new UnsupportedOperationException();
    }

}
