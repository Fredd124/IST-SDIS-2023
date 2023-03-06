package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerState {

    private List<Operation> ledger;
    private boolean active;
    private Map<String, Integer> accountMap;

    public ServerState() {
        this.ledger = new ArrayList<>();
        this.active = false;
        this.accountMap = new HashMap<>();
    }


    public boolean getActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void activate() {
        setActive(true);
    }

    public void deactivate() {
        setActive(false);
    }

    public int getBalance(String userId) {
        return accountMap.get(userId);
    }

    public List<Operation> getLedgerState() {
        return this.ledger;
    }

    public boolean containsUser(String userId) {
        return accountMap.containsKey(userId);
    }

    public void createAccount(String userId, int initialBalance) {
        accountMap.put(userId, initialBalance);
        CreateOp createOp = new CreateOp(userId);
        ledger.add(createOp);
    }

    public void createAccount(String userId) {
        createAccount(userId, 0);
        CreateOp createOp = new CreateOp(userId);
        ledger.add(createOp);
    }

    public void deleteAccount(String userId) {
        accountMap.remove(userId);
        DeleteOp deleteOp = new DeleteOp(userId);
        ledger.add(deleteOp);
    }

    public void transfer(String fromAccount, String toAccount, int amount) {
        accountMap.put(fromAccount, accountMap.get(fromAccount) - amount);
        accountMap.put(toAccount, accountMap.get(toAccount) + amount);
        TransferOp transferOp = new TransferOp(fromAccount, toAccount, amount);
        ledger.add(transferOp);
    }
}
