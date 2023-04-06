package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.utils.Utils;
import pt.tecnico.distledger.server.domain.exceptions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class ServerState {

    private List<Operation> ledger;
    private List<Integer> replicaVectorClock;
    private String address;
    private Character qualifier;
    private boolean active;
    private boolean debug;
    private Map<String, Integer> accountMap;
    private final String BROKER = "broker";
    private final Integer BROKER_INIT_VALUE = 1000;

    public ServerState(boolean debug, String address, Character qualifier) {
        this.ledger = Collections.synchronizedList(new ArrayList<>());
        this.replicaVectorClock = Collections
                .synchronizedList(new ArrayList<>());
        for (int i = 0; i < 3; i++) {
            this.replicaVectorClock.add(0);
        }
        this.active = true;
        this.debug = debug;
        this.address = address;
        this.qualifier = qualifier;
        this.accountMap = Collections.synchronizedMap(new HashMap<>());
        createBroker();
    }

    public synchronized boolean isActive() {
        return this.active;
    }

    private void setActive(boolean active) {
        this.active = active;
    }

    public String getAddress() {
        return this.address;
    }

    public Character getQualifier() {
        return this.qualifier;
    }

    public List<Integer> getReplicaVectorClock() {
        return this.replicaVectorClock;
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
        if (debug)
            System.err.println(message);
    }

    public int balance(String userId, List<Integer> clientVectorClock)
            throws NotActiveException, UserDoesNotExistException {
        if (!this.active) {
            throw new NotActiveException();
        }
        else if (!this.containsUser(userId)) {
            throw new UserDoesNotExistException();
        }
        return getBalance(userId);
    }

    public int getBalance(String userId) {
        return accountMap.get(userId);
    }

    public synchronized List<Operation> getLedgerState() {
        return this.ledger;
    }

    public synchronized void setLedgerState(List<Operation> ops) {
        this.ledger = ops;
    }

    public boolean containsUser(String userId) {
        return accountMap.containsKey(userId);
    }

    public void createBroker() {
        accountMap.put(BROKER, BROKER_INIT_VALUE);
    }

    public Operation createAccount(String userId, List<Integer> timeStamp)
            throws NotActiveException, UserAlreadyExistsEception {
        if (!this.active) {
            throw new NotActiveException();
        }
        else if (this.containsUser(userId)) {
            throw new UserAlreadyExistsEception();
        }
        return new CreateOp(userId, timeStamp);
    }

    public Operation transfer(String fromAccount, String toAccount, int amount, List<Integer> timeStamp)
            throws NotActiveException, SourceUserDoesNotExistException,
            DestinationUserDoesNotExistException,
            SourceEqualsDestinationUserException, InvalidUserBalanceException,
            InvalidBalanceAmountException, UserDoesNotExistException {
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
        return new TransferOp(fromAccount, toAccount, amount, timeStamp);
    }

    public void verifyOp(Operation op, List<Integer> clientVectorClock) {
        switch(op.getType()) {
            case("OP_CREATE_ACCOUNT"):
                try {
                    Operation newOp = createAccount(op.getAccount());
                    addOp(newOp, clientVectorClock);
                }
                catch (ServerStateException e) {
                    debugPrint("Repeated or invalid operation. Skiping operation...");
                } 
            case("OP_TRANSFER_TO"):
                try {
                    TransferOp transferOp = (TransferOp) op;
                    Operation newOp = transfer(transferOp.getAccount(), transferOp.getDestAccount(), transferOp.getAmount());
                    addOp(newOp, clientVectorClock);
                } catch (ServerStateException e){
                    debugPrint("Repeated or invalid operation. Skiping operation...");
                }           
        }
    }

    public void addOp(Operation op, List<Integer> clientVectorClock) {
        ledger.add(op);
        int i = Utils.getIndexFromQualifier(qualifier);
        replicaVectorClock.set(i, replicaVectorClock.get(i) + 1);
        if (clientVectorClock != null) {
            clientVectorClock.set(i, replicaVectorClock.get(i));
        }
        debugPrint(String.format("New clock for server %s : %s", this.qualifier,
                this.replicaVectorClock.toString()));
        updateStableOps();
    }

    public void doOp(Operation op, List<Integer> clientVectorClock) {
        if (op.getType().equals("OP_CREATE_ACCOUNT")) {
            CreateOp createOp = (CreateOp) op;
            accountMap.put(createOp.getAccount(), 0);
            debugPrint("Created account: " + createOp.getAccount());
        }
        else if (op.getType().equals("OP_TRANSFER_TO")) {
            TransferOp transferOp = (TransferOp) op;
            accountMap.put(transferOp.getAccount(),
                    accountMap.get(transferOp.getAccount())
                            - transferOp.getAmount());
            accountMap.put(transferOp.getDestAccount(),
                    accountMap.get(transferOp.getDestAccount())
                            + transferOp.getAmount());
            debugPrint("Transfered " + transferOp.getAmount() + " from "
                    + transferOp.getAccount() + " to "
                    + transferOp.getDestAccount());
        }
    }

    public void doOpList(List<Operation> missingOps,
            List<Integer> clientVectorClock) {
        for (Operation op : missingOps) {
            this.doOp(op, clientVectorClock);
        }
    }

    public void updateStableOps() {
        ledger.stream().forEach(operation -> {
            if (!operation.isStable() && isBiggerTimeStamp(operation.getTimeStamp()))
                if (operation.getType().equals("OP_CREATE_ACCOUNT")) {
                    operation.setStable(true);
                    doOp(operation, null);
                }
                else if (operation.getType().equals("OP_TRANSFER_TO")) {
                        operation.setStable(true);
                        doOp(operation, null);
                }
        });
    }

    private boolean isBiggerTimeStamp(List<Integer> requestTimeStamp) {
        for (int i = 0; i < requestTimeStamp.size(); i++) {
            if (requestTimeStamp.get(i) > replicaVectorClock.get(i)) {
                return false;
            }
        }
        return true;
    }

    public void updateReplicaClocks(List<Integer> replicaVectorClock) {
        for (int i = 0; i < replicaVectorClock.size(); i++) {
            if (replicaVectorClock.get(i) > this.replicaVectorClock.get(i)) {
                this.replicaVectorClock.set(i, replicaVectorClock.get(i));
            }
        }
        debugPrint(String.format("New clock for server %s : %s", qualifier,
                this.replicaVectorClock.toString()));
    }
}
