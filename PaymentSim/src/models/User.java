package models;

/**
 * Represents a registered user in the payment system.
 */
public class User {
    private static int idCounter = 1000;

    private final String userId;
    private String name;
    private String email;
    private String phone;
    private double walletBalance;
    private String pin; // hashed in real systems

    public User(String name, String email, String phone, String pin) {
        this.userId  = "USR" + (idCounter++);
        this.name    = name;
        this.email   = email;
        this.phone   = phone;
        this.pin     = pin;
        this.walletBalance = 0.0;
    }

    // ── Getters ──────────────────────────────────────────────
    public String getUserId()        { return userId; }
    public String getName()          { return name; }
    public String getEmail()         { return email; }
    public String getPhone()         { return phone; }
    public double getWalletBalance() { return walletBalance; }

    // ── Wallet operations ────────────────────────────────────
    public void credit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Credit amount must be positive.");
        walletBalance += amount;
    }

    public void debit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Debit amount must be positive.");
        if (amount > walletBalance) throw new IllegalStateException("Insufficient wallet balance.");
        walletBalance -= amount;
    }

    // ── Authentication ───────────────────────────────────────
    public boolean verifyPin(String inputPin) {
        return this.pin.equals(inputPin);
    }

    @Override
    public String toString() {
        return String.format("User[%s | %s | %s | Balance: ₹%.2f]",
                userId, name, email, walletBalance);
    }
}
