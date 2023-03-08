package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.domain.exceptions.*;

import java.io.NotActiveException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerState {

    private List<Operation> ledger;
    private boolean active;
    private boolean debug;
    private Map<String, Integer> accountMap;

    public ServerState(boolean debug) {
        this.ledger = new ArrayList<>();
        this.active = true;
        this.debug = debug;
        this.accountMap = new HashMap<>();
        createBroker();
    }

    public boolean getActive() {
        return this.active;
    }

    private void setActive(boolean active) {
        this.active = active;
    }

    public void activate() throws AlreadyActiveException {
        if (this.active == true) {
            throw new AlreadyActiveException();
        }
        setActive(true);
    }

    public void deactivate() throws NotActiveException {
        if (this.active == false) {
            throw new NotActiveException();
        }
        setActive(false);
    }

    public void debugPrint(String message) {
        if (debug) System.err.println(message);
    }

    public int getBalance(String userId) throws UserDoesNotExistException {
        if (!this.containsUser(userId)) {
            throw new UserDoesNotExistException();
        }
        return accountMap.get(userId);
    }

    public List<Operation> getLedgerState() {
        return this.ledger;
    }

    public boolean containsUser(String userId) {
        return accountMap.containsKey(userId);
    }

    public void createBroker() { // broker
        accountMap.put("broker", 1000);
    }

    public void createAccount(String userId) throws UserAlreadyExistsEception {
        if (this.containsUser(userId)) {
            throw new UserAlreadyExistsEception();
        }
        accountMap.put(userId, 0);
        CreateOp createOp = new CreateOp(userId);
        ledger.add(createOp);
    }

    public void deleteAccount(String userId) throws BrokerCantBeDeletedException, BalanceNotZeroException, UserDoesNotExistException {
        if (userId.equals("broker")) {
            throw new BrokerCantBeDeletedException();
        }
        else if (!this.containsUser(userId)) {
            throw new UserDoesNotExistException();
        }
        else if (this.getBalance(userId) == 0) {
            throw new BalanceNotZeroException();
        }
        accountMap.remove(userId);
        DeleteOp deleteOp = new DeleteOp(userId);
        ledger.add(deleteOp);
    }

    public void transfer(String fromAccount, String toAccount, int amount) throws SourceUserDoesNotExistException, 
            DestinationUserDoesNotExistException, SourceEqualsDestinationUserException,
                InvalidUserBalanceException, InvalidBalanceAmountException, UserDoesNotExistException {
        if (!this.containsUser(fromAccount)) {
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
        accountMap.put(fromAccount, accountMap.get(fromAccount) - amount);
        accountMap.put(toAccount, accountMap.get(toAccount) + amount);
        TransferOp transferOp = new TransferOp(fromAccount, toAccount, amount);
        ledger.add(transferOp);
    }
}
