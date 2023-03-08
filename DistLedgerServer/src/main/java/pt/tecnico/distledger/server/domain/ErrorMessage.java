package pt.tecnico.distledger.server.domain;

public enum ErrorMessage {
    SERVER_NOT_ACTIVE("The server provided is not active"),
    SERVER_ALREADY_ACTIVE("The server provided is already active"),
    
    USER_DOES_NOT_EXIST("The user account provided does not exist"),
    USER_ALREADY_EXISTS("The user account provided already exists"),
    BROKER_CAN_NOT_BE_DELETED("The broker account can not be deleted"),
    SOURCE_USER_DOES_NOT_EXIST("The source user account provided does not exist"),
    DESTINATION_USER_DOES_NOT_EXIST("The destination user acccount provided does not exist"),
    SOURCE_EQUALS_DESTINATION("The source user account must be diferent from the destination user account"),

    INVALID_BALANCE_AMOUNT("The balance amount provided must greater than 0"),
    INVALID_USER_BALANCE("The source user account provided does not have enough balance"),
    BALANCE_NOT_ZERO("In order to delete an account, the account's balance must be zero");

    public final String label;

    ErrorMessage(String label) {
        this.label = label;
    }
}