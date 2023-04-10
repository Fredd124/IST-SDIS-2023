package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.utils.Utils;
import pt.tecnico.distledger.server.domain.exceptions.*;

import java.util.ArrayList;
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
    private Map<String, Integer> accountMap;
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
            throws NotActiveException, UserDoesNotExistException, NotUpToDateException {
        if (!this.active) {
            throw new NotActiveException();
        }
        else if (!this.containsUser(userId)) {
            throw new UserDoesNotExistException();
        }
        else if (isBiggerTimeStampValue(clientVectorClock)) {
            throw new NotUpToDateException(); // THIS?
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

    public boolean verifyIfCanExecuteOp(Operation op) { // TODO : add verification to add if op is more recent then timeStamp (prevent double operations)
        if (isRepeatedOp(op)) {
            debugPrint(String.format("Operation %s is repeated", op.getType()));
            return false;
        }
        debugPrint(String.format("Received operation %s with clock %s", op.getType(), op.getPrev()));
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

    public void addOp(Operation op, List<Integer> clientVectorClock) {
        ledger.add(op);
        int i = Utils.getIndexFromQualifier(qualifier);
        replicaVectorClock.set(i, replicaVectorClock.get(i) + 1);
        clientVectorClock.set(i, replicaVectorClock.get(i));
        op.setTimeStamp(clientVectorClock);
        debugPrint(String.format("New clock for server %s : %s", this.qualifier,
                this.replicaVectorClock.toString()));
    }

    public void doOp(Operation op, List<Integer> clientVectorClock) { /** TODO: remove vector clock if not used */
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
           valueVectorClock.set(i, Math.max(valueVectorClock.get(i), replicaVectorClock.get(i)));
        }
    }

    public void frontendRequest(Operation op, List<Integer> clientVectorClock) {
        addOp(op, clientVectorClock);
        if (canExecute(op)) {
            op.setStable(true);
            doOp(op, clientVectorClock);
        }
    }

    public boolean canExecute(Operation op) {
        debugPrint(op.getPrev() + " " + valueVectorClock);
        return !op.isStable() && isBiggerTimeStampValue(op.getPrev());
    }

    public boolean canBeStable(Operation op) { /** TODO : maybe not necessary */
        switch(op.getType()) {
            case("OP_CREATE_ACCOUNT"):
                return true;
            case("OP_TRANSFER_TO"):
            TransferOp transferOp = (TransferOp) op;
                return  this.containsUser(transferOp.getAccount()) && this.containsUser(transferOp.getDestAccount());
            default:
                return false;
        }
    }

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
                if (o1.getType().equals("OP_CREATE_ACCOUNT") && o2.getType().equals("OP_Create_ACCOUNT")) return 0;
                else if (o1.getType().equals("OP_CREATE_ACCOUNT")) return -1;
                else if (o2.getType().equals("OP_CREATE_ACCOUNT")) return 1;
                else return 0;
            }
        });
        return executableOps;
    }

    public void updateStableOps() {
        List<Operation> executableOps = getExecutableOpsSorted();
        debugPrint("Sorted ops: ");
        for (Operation op : executableOps) {
            debugPrint(String.format("Operation %s with clock %s", op.getType(), op.getPrev()));
        }
        for (Operation op : executableOps) {
            op.setStable(true);
            doOp(op, op.getTimeStamp());
        }
    }

    public boolean estimatedGossip(Operation op, String qualifierString) {
        Character qualifierCharacter = qualifierString.charAt(0);
        int index = Utils.getIndexFromQualifier(qualifierCharacter);
        debugPrint(String.format("Comparing %s with %s", op.getPrev().get(index), replicaVectorClock.get(index)));
        return op.getPrev().get(index) >= replicaVectorClock.get(index);
    }

    public boolean isRepeatedOp(Operation op) {
        debugPrint(String.format("Checking if operation %s is repeated %s", op.getType(), op.getPrev()));
        return ledger.stream().anyMatch(operation -> op.getPrev().equals(operation.getTimeStamp()));
    }

    public boolean isBiggerTimeStampValue(List<Integer> requestTimeStamp) {
        return Utils.compareVectorClocks(requestTimeStamp, valueVectorClock) == 1;
    }

    private boolean isSmallerTimeStamp(List<Integer> requestTimeStamp) {
        return Utils.compareVectorClocks(requestTimeStamp, valueVectorClock) == -1;
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
