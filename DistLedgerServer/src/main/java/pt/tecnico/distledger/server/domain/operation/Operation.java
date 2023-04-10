package pt.tecnico.distledger.server.domain.operation;

import java.lang.UnsupportedOperationException;
import java.util.List;

public class Operation {
    private String account;
    private boolean stable;
    private List<Integer> timeStamp;
    List<Integer> prev;

    public Operation(String fromAccount, List<Integer> prev) {
        this.account = fromAccount;
        this.stable = false;
        this.prev = prev;
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
    
    public List<Integer> getPrev() {
        return this.prev;
    }

    public void setPrev(List<Integer> prev) {
        this.prev = prev;
    }

    public String getType(){
        throw new UnsupportedOperationException();
    }

}
