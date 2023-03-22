package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.domain.exceptions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Iterator;

public class ServerState {

    private List<Operation> ledger;
    private String address;
    private String qualifier;
    private boolean active;
    private boolean debug;
    private Map<String, Integer> accountMap;

    public ServerState(boolean debug, String address, String qualifier) {
        this.ledger = Collections.synchronizedList(new ArrayList<>());
        this.active = true;
        this.debug = debug;
        this.address = address;
        this.qualifier = qualifier;
        this.accountMap = new HashMap<>();
        createBroker();
    }

    public boolean getActive() {
        return this.active;
    }

    public boolean isActive() {
        return this.active;
    }

    public void canWrite() throws NotWritableException {
        if (!this.qualifier.equals("A")) {
            throw new NotWritableException();
        } 
    }

    private void setActive(boolean active) {
        this.active = active;
    }

    public String getAddress() {
        return this.address;
    }

    public String getQualifier() {
        return this.qualifier;
    }

    public synchronized void activate() throws AlreadyActiveException {
        if (this.active) {
            throw new AlreadyActiveException();
        }
        setActive(true);
    }

    public synchronized void deactivate() throws NotActiveException {
        if (!this.active) {
            throw new NotActiveException();
        }
        setActive(false);
    }

    public void debugPrint(String message) {
        if (debug) System.err.println(message);
    }

    public int getBalance(String userId) throws NotActiveException, UserDoesNotExistException {
        if (!this.active) {
            throw new NotActiveException();
        } 
        else if (!this.containsUser(userId)) {
            throw new UserDoesNotExistException();
        }
        return accountMap.get(userId);
    }

    public List<Operation> getLedgerState() {
        List<Operation> copy = new ArrayList<>();
        synchronized(this.ledger) {
            Iterator<Operation> i = this.ledger.iterator(); 
            while (i.hasNext())
                copy.add(i.next());
        }
        return copy;
    }

    public synchronized void setLedgerState(List<Operation> ops) {
        this.ledger = ops;
    }

    public boolean containsUser(String userId) {
        return accountMap.containsKey(userId);
    }

    public synchronized void createBroker() { // broker
        accountMap.put("broker", 1000);
    }

    public synchronized Operation getLastOperation() {
        return ledger.get(ledger.size() - 1);
    }

    public synchronized void removeLastOp() {
        ledger.remove(ledger.size() - 1);
    }

    public synchronized Operation createAccount(String userId) throws NotActiveException, UserAlreadyExistsEception {
        if (!this.active) {
            throw new NotActiveException();
        } 
        else if (this.containsUser(userId)) {
            throw new UserAlreadyExistsEception();
        }
        CreateOp op = new CreateOp(userId);
        ledger.add(op);
        return op;
    }

    public synchronized Operation deleteAccount(String userId) throws NotActiveException, BrokerCantBeDeletedException, 
            BalanceNotZeroException, UserDoesNotExistException {
        if (!this.active) {
            throw new NotActiveException();
        } 
        else if (userId.equals("broker")) {
            throw new BrokerCantBeDeletedException();
        }
        else if (!this.containsUser(userId)) {
            throw new UserDoesNotExistException();
        }
        else if (this.getBalance(userId) != 0) {
            throw new BalanceNotZeroException();
        }
        DeleteOp op = new DeleteOp(userId);
        ledger.add(op);
        return op;
    }

    public synchronized Operation transfer(String fromAccount, String toAccount, int amount) throws NotActiveException, 
            SourceUserDoesNotExistException, DestinationUserDoesNotExistException, 
                SourceEqualsDestinationUserException, InvalidUserBalanceException, InvalidBalanceAmountException, 
                    UserDoesNotExistException {
        if (!this.active) {
            throw new NotActiveException();
        } 
        else if (!this.containsUser(fromAccount)) {
            throw new SourceUserDoesNotExistException();
        }
        else if (!this.containsUser(toAccount)) {
            throw new DestinationUserDoesNotExistException();
        }
        else if (fromAccount.equals(toAccount)) {
            throw new SourceEqualsDestinationUserException();
        }
        else if (this.getBalance(fromAccount) < amount) {
            throw new InvalidUserBalanceException();
        }
        else if (amount <= 0) {
            throw new InvalidBalanceAmountException();
        }
        TransferOp op = new TransferOp(fromAccount, toAccount, amount);
        ledger.add(op);
        return op;
    }

    public void doOp(Operation op) {
        if (op.getType().equals("OP_CREATE_ACCOUNT")) {
            CreateOp createOp = (CreateOp) op;
            accountMap.put(createOp.getAccount(), 0);
            synchronized(this.ledger) {
                ledger.add(createOp);
            }
            debugPrint("Created account: " + createOp.getAccount());
        } 
        else if (op.getType().equals("OP_DELETE_ACCOUNT")) {
            DeleteOp deleteOp = (DeleteOp) op;
            accountMap.remove(deleteOp.getAccount());
            synchronized(this.ledger) {
                ledger.add(deleteOp);
            }
            debugPrint("Deleted account: " + deleteOp.getAccount());
        } 
        else if (op.getType().equals("OP_TRANSFER_TO")) {
            TransferOp transferOp = (TransferOp) op;
            accountMap.put(transferOp.getAccount(), 
                        accountMap.get(transferOp.getAccount()) - transferOp.getAmount());
            accountMap.put(transferOp.getDestAccount(), 
                        accountMap.get(transferOp.getDestAccount()) + transferOp.getAmount());
            synchronized(this.ledger) {
                ledger.add(transferOp);
            }
            debugPrint("Transfered " + transferOp.getAmount() + " from " + transferOp.getAccount() + " to " + transferOp.getDestAccount());
        }
    }

    public void doOpList(List<Operation> missingOps) {
        //debugPrint("got here");
        for (Operation op : missingOps) {
            this.doOp(op);
        }
    }
}
