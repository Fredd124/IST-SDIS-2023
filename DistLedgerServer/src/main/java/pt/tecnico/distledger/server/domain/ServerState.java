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
    private Map<String, List<Integer>> timetableMap;
    private final String BROKER = "broker";
    private final Integer BROKER_INIT_VALUE = 1000;

    public ServerState(boolean debug, String address, Character qualifier) {
        this.ledger = Collections.synchronizedList(new ArrayList<>());
        this.replicaVectorClock = Collections
                .synchronizedList(new ArrayList<>());
        this.valueVectorClock = Collections
                .synchronizedList(new ArrayList<>());
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
        this.timetableMap = new HashMap<String, List<Integer>>(); // Improve initialization
        initializeTimeTableMap(replicaVectorClock);
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

    public void initializeTimeTableMap(List<Integer> vector) {
        Integer[] array = new Integer[3];
        Arrays.fill(array, 0);
        List<Integer> zeroList = Arrays.asList(array);
        timetableMap.put("A", zeroList);
        timetableMap.put("B", zeroList);
        timetableMap.put("C", zeroList);
    }

    public List<Integer> getValueVectorClock() {
        return this.valueVectorClock;
    }

    public Map<String, List<Integer>> getTimeTableMap() {
        return this.timetableMap;
    }
    
    public void changeTimeTableMapEntry(String qualifier, List<Integer> newGossipTimeStamp) {
        timetableMap.replace(qualifier, newGossipTimeStamp);
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

    public synchronized int balance(String userId, List<Integer> clientVectorClock)
            throws NotActiveException, UserDoesNotExistException, NotUpToDateException {
        if (!this.active) {
            throw new NotActiveException();
        }
        else if (!isBiggerTimeStampValue(clientVectorClock)) {
            clientOnHold = true;
            while (clientOnHold) {
                try {
                    wait();
                    clientOnHold = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!this.containsUser(userId)) {
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
        else if (fromAccount.equals(toAccount)) {
            throw new SourceEqualsDestinationUserException();
        }
        else if (amount <= 0) {
            throw new InvalidBalanceAmountException();
        }
        return new TransferOp(fromAccount, toAccount, amount, timeStamp);
    }

    public boolean verifyConstraints(Operation op) {
        switch(op.getType()) {
            case("OP_CREATE_ACCOUNT"):
                return ! this.containsUser(op.getAccount());
            case("OP_TRANSFER_TO"):
                TransferOp transferOp = (TransferOp) op;
                return  this.containsUser(transferOp.getAccount()) && this.containsUser(transferOp.getDestAccount())
                    && this.getBalance(transferOp.getAccount()) >= transferOp.getAmount();
            default:
                return false;
        }
    }

    /* Add operation to ledger */
    public void addOp(Operation op) {
        ledger.add(op);   
    }

    /* Increment this replica's replicaTS entry and change client vector clock */
    public void incrementReplicaEntry(Operation op, List<Integer> clientVectorClock) {
        int i = Utils.getIndexFromQualifier(qualifier);
        replicaVectorClock.set(i, replicaVectorClock.get(i) + 1);
        clientVectorClock.set(i, replicaVectorClock.get(i));
        if(op.getTimeStamp() == null) op.setTimeStamp(clientVectorClock);
        debugPrint(String.format("New clock for server %s : %s", this.qualifier,
        this.replicaVectorClock.toString()));
    }

    public void mergeReplicaClock(List<Integer> operationVectorClock) {
        for (int i = 0; i < replicaVectorClock.size(); i++) {
            replicaVectorClock.set(i, Math.max(replicaVectorClock.get(i), operationVectorClock.get(i)));
        }
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
           valueVectorClock.set(i, Math.max(valueVectorClock.get(i), op.getTimeStamp().get(i)));
        }
        debugPrint("ValueVectorClock: " + valueVectorClock);
        if (clientOnHold) {
            notifyAll();
        }
    }

    /* Receive a request from the frontend */
    public void frontendRequest(Operation op, List<Integer> clientVectorClock) {
        addOp(op);
        incrementReplicaEntry(op, clientVectorClock);
        if (canExecute(op)) {
            op.setStable(true);
            doOp(op);
        }
    }

    public boolean canExecute(Operation op) {
        return !op.isStable() && isBiggerTimeStampValue(op.getPrev());
    }

    /* Sorts stable operations in order to have them in the same order in every server */
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
                int compareResult = Utils.compareVectorClocks(o1.getTimeStamp(), o2.getTimeStamp());
                if (compareResult != 0) return - compareResult;
                else return compareEntries(o1.getTimeStamp(), o2.getTimeStamp());
            }
        });
        return executableOps;
    }

    /* Update stable operations if possible */
    public void updateStableOps() {
        List<Operation> executableOps = getExecutableOpsSorted();
        debugPrint("Sorted ops: ");
        for (Operation op : executableOps) {
            debugPrint(String.format("Operation %s with clock %s", op.getType(), op.getPrev()));
        }
        for (Operation op : executableOps) {
            if (isBiggerTimeStampValue(op.getPrev())) {
                if (verifyConstraints(op)) {
                    op.setStable(true);
                    doOp(op);
                }
                else {
                    for (int i = 0; i < replicaVectorClock.size(); i++) {
                        valueVectorClock.set(i, Math.max(valueVectorClock.get(i), op.getTimeStamp().get(i)));
                    }
                }
            }
        }
    }

    public boolean isRepeatedOp(Operation op) {
        debugPrint(String.format("Comparing r.ts %s with replica clock %s", op.getTimeStamp(), replicaVectorClock));
        return isBiggerTimeStampReplica(op.getTimeStamp());
    }

    /* TODO : change to is bigger or equal? */
    public boolean isBiggerTimeStampValue(List<Integer> requestTimeStamp) {
        return Utils.compareVectorClocks(requestTimeStamp, valueVectorClock) == 1;
    }

    public boolean isBiggerTimeStampReplica(List<Integer> requestTimeStamp) {
        return Utils.compareVectorClocks(requestTimeStamp, replicaVectorClock) == 1;
    }

    private int compareEntries(List<Integer> requestTimeStamp, List<Integer> opTimeStamp) {
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

    public void updateReplicaClocks(List<Integer> replicaVectorClock) {
        for (int i = 0; i < replicaVectorClock.size(); i++) {
            if (replicaVectorClock.get(i) > this.replicaVectorClock.get(i)) {
                this.replicaVectorClock.set(i, replicaVectorClock.get(i));
            }
        }
        debugPrint(String.format("Replica clock for server %s : %s", qualifier,
                this.replicaVectorClock.toString()));
    }
}
