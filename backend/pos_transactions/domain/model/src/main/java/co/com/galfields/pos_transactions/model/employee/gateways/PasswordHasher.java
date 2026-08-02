package co.com.galfields.pos_transactions.model.employee.gateways;

public interface PasswordHasher {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
