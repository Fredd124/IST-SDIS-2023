package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.utils.Utils;
import pt.tecnico.distledger.server.domain.exceptions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

public class ServerState {

    private List<Operation> ledger;
    private List<Integer> replicaVectorClock;
    private List<Integer> valueVectorClock;
    private String address;
    private Character qualifier;
    private boolean active;
    private boolean debug;
    private boolean clientOnHold;
    private Map<String, Integer> accountMap;
    private Map<String, List<Integer>> timeTableMap;
    private final String BROKER = "broker";
    private final Integer BROKER_INIT_VALUE = 1000;

    public ServerState(boolean debug, String address, Character qualifier) {
        this.ledger = Collections.synchronizedList(new ArrayList<>());
        this.replicaVectorClock = Collections
                .synchronizedList(new ArrayList<>());
        this.valueVectorClock = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < 3; i++) {
            this.replicaVectorClock.add(0);
            this.valueVectorClock.add(0);
        }
        this.active = true;
        this.debug = debug;
        this.clientOnHold = false;
        this.address = address;
        this.qualifier = qualifier;
        this.accountMap = Collections.synchronizedMap(new HashMap<>());
        this.timeTableMap = new HashMap<String, List<Integer>>(); 
        initializetimeTableMap(replicaVectorClock);
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

    public int getBalance(String userId) {
        return accountMap.get(userId);
    }

    public synchronized List<Operation> getLedgerState() {
        return this.ledger;
    }

    public synchronized void setLedgerState(List<Operation> ops) {
        this.ledger = ops;
    }

    public void initializetimeTableMap(List<Integer> vector) {
        Integer[] array = new Integer[3];
        Arrays.fill(array, 0);
        List<Integer> zeroList = Arrays.asList(array);
        timeTableMap.put("A", zeroList);
        timeTableMap.put("B", zeroList);
        timeTableMap.put("C", zeroList);
        timeTableMap.remove(this.qualifier.toString());
    }

    public Map<String, List<Integer>> getTimeTableMap() {
        return this.timeTableMap;
    }

    public boolean containsUser(String userId) {
        return accountMap.containsKey(userId);
    }

    public void createBroker() {
        accountMap.put(BROKER, BROKER_INIT_VALUE);
    }

    public void changetimeTableMapEntry(String qualifier,
            List<Integer> newGossipTimeStamp) {
        timeTableMap.replace(qualifier, newGossipTimeStamp);
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

    public synchronized int balance(String userId,
            List<Integer> clientVectorClock) throws NotActiveException,
            UserDoesNotExistException, NotUpToDateException,
            InterruptedException {
        if (!this.active) {
            throw new NotActiveException();
        }
        else if (!isNotBiggerThanValueTS(clientVectorClock)) {
            clientOnHold = true;
            while (clientOnHold) {
                try {
                    wait();
                    clientOnHold = false;
                }
                catch (InterruptedException e) {
                    throw new InterruptedException();
                }
            }
        }
        if (!this.containsUser(userId)) {
            throw new UserDoesNotExistException();
        }
        return getBalance(userId);
    }

    public Operation createAccount(String userId, List<Integer> timeStamp)
            throws NotActiveException, UserAlreadyExistsEception {
        if (!this.active) {
            throw new NotActiveException();
        }
        return new CreateOp(userId, timeStamp);
    }

    public Operation transfer(String fromAccount, String toAccount, int amount,
            List<Integer> timeStamp)
            throws NotActiveException, SourceUserDoesNotExistException,
            DestinationUserDoesNotExistException,
            SourceEqualsDestinationUserException, InvalidUserBalanceException,
            InvalidBalanceAmountException, UserDoesNotExistException {
        if (!this.active) {
            throw new NotActiveException();
        }
        else if (fromAccount.equals(toAccount)) {
            throw new SourceEqualsDestinationUserException();
        }
        else if (amount <= 0) {
            throw new InvalidBalanceAmountException();
        }
        return new TransferOp(fromAccount, toAccount, amount, timeStamp);
    }

    /* Used to check if executable operation can be applied to value or not */
    public void verifyConstraints(Operation op) throws UserDoesNotExistException, 
        InvalidUserBalanceException, SourceUserDoesNotExistException,
        DestinationUserDoesNotExistException {
        switch(op.getType()) {
            case("OP_CREATE_ACCOUNT"):
                if(this.containsUser(op.getAccount())) {
                    throw new UserDoesNotExistException();
                }
                break;
            case("OP_TRANSFER_TO"):
                TransferOp transferOp = (TransferOp) op;
                if (!this.containsUser(transferOp.getAccount())) {
                    throw new SourceUserDoesNotExistException();
                }
                else if (!this.containsUser(transferOp.getDestAccount())) {
                    throw new DestinationUserDoesNotExistException();
                }
                else if (this.getBalance(transferOp.getAccount()) < transferOp.getAmount()) {
                    throw new InvalidUserBalanceException();
                }
                break;
            default:
                break;
        }
    }

    public void addOpToLedger(Operation op) {
        ledger.add(op);
    }

    /*
     * Increment this replica's replicaTS entry and change client vector clock
     */
    public void incrementReplicaEntry(Operation op,
            List<Integer> clientVectorClock) {
        int i = Utils.getIndexFromQualifier(qualifier);
        replicaVectorClock.set(i, replicaVectorClock.get(i) + 1);
        clientVectorClock.set(i, replicaVectorClock.get(i));
        if (op.getTimeStamp() == null)
            op.setTimeStamp(clientVectorClock);
        debugPrint(String.format("New replicaTS for server %s : %s", this.qualifier,
                this.replicaVectorClock.toString()));
    }

    /* Execute operation and change server state */
    public synchronized void doOp(Operation op) {
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
        for (int i = 0; i < replicaVectorClock.size(); i++) {
            valueVectorClock.set(i, Math.max(valueVectorClock.get(i),
                    op.getTimeStamp().get(i)));
        }
        debugPrint("ValueTS: " + valueVectorClock);
        if (clientOnHold) {
            notifyAll();
        }
    }

    /* Receive a request from the frontend */
    public void frontendRequest(Operation op, List<Integer> clientVectorClock) {
        addOpToLedger(op);
        incrementReplicaEntry(op, clientVectorClock);
        if (canExecute(op)) {
            op.setStable(true);
            doOp(op);
        }
    }

    public boolean canExecute(Operation op) {
        return !op.isStable() && isNotBiggerThanValueTS(op.getPrev());
    }

    /*
     * Sorts stable operations in order to have them in the same order in every
     * server
     */
    public List<Operation> getExecutableOpsSorted() {
        List<Operation> executableOps = new ArrayList<Operation>();
        for (Operation op : ledger) {
            if (!op.isStable()) {
                executableOps.add(op);
            }
        }
        Collections.sort(executableOps, new Comparator<Operation>() {
            @Override
            public int compare(Operation o1, Operation o2) {
                int compareResult = Utils.compareVectorClocks(o1.getTimeStamp(),
                        o2.getTimeStamp());
                if (compareResult != 0)
                    return -compareResult;
                else
                    return compareEntries(o1.getTimeStamp(), o2.getTimeStamp());
            }
        });
        return executableOps;
    }

    /* Used to compare timeStamps without a > or < relationship */
    private int compareEntries(List<Integer> requestTimeStamp,
        List<Integer> opTimeStamp) {
        for (int i = 0; i < requestTimeStamp.size(); i++) {
            if (requestTimeStamp.get(i) == opTimeStamp.get(i)) {
                continue;
            }
            else if (requestTimeStamp.get(i) > opTimeStamp.get(i)) {
                return 1;
            }
            else {
                return -1;
            }
        }
        return 0;
    }

    /* Update stable operations if possible */
    public void updateStableOps() {
        List<Operation> executableOps = getExecutableOpsSorted();
        for (Operation op : executableOps) {
            if (isNotBiggerThanValueTS(op.getPrev())) {
                op.setStable(true);
                try {
                    verifyConstraints(op);
                    doOp(op);
                } catch (ServerStateException e) {
                    debugPrint(String.format("Threw exception : %s .", e.getMessage()));
                    for (int i = 0; i < replicaVectorClock.size(); i++) {
                        valueVectorClock.set(i, Math.max(valueVectorClock.get(i), op.getTimeStamp().get(i)));
                    }
                }
            }
        }
    }

    public boolean isRepeatedOp(Operation op) {
        debugPrint(String.format("Comparing r.ts %s with replicaTS %s",
                op.getTimeStamp(), replicaVectorClock));
        return isNotBiggerThanReplicaTS(op.getTimeStamp());
    }

    /* op.ts <= valueTS */
    public boolean isNotBiggerThanValueTS(List<Integer> requestTimeStamp) {
        return Utils.compareVectorClocks(requestTimeStamp,
                valueVectorClock) == 1;
    }
    /* op.ts <=  replicaTS*/
    public boolean isNotBiggerThanReplicaTS(List<Integer> requestTimeStamp) {
        return Utils.compareVectorClocks(requestTimeStamp,
                replicaVectorClock) == 1;
    }

    /* Merge this replicaTS for received replicaTS */
    public void updateReplicaClocks(List<Integer> replicaVectorClock) {
        for (int i = 0; i < replicaVectorClock.size(); i++) {
            if (replicaVectorClock.get(i) > this.replicaVectorClock.get(i)) {
                this.replicaVectorClock.set(i, replicaVectorClock.get(i));
            }
        }
        debugPrint(String.format("ReplicaTS of server %s : %s", qualifier,
                this.replicaVectorClock.toString()));
    }

    public void debugPrint(String message) {
        if (debug)
            System.err.println(message);
    }
}
