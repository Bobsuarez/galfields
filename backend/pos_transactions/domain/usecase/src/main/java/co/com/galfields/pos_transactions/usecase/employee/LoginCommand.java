package co.com.galfields.pos_transactions.usecase.employee;

public record LoginCommand(String username, String password, String terminalCode) {
}
