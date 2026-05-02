package models;

/**
 * Represents a linked debit / credit card.
 */
public class Card {

    public enum CardType { DEBIT, CREDIT }

    private final String   cardNumber;   // last 4 digits stored only
    private final String   cardHolder;
    private final String   expiryMonth;
    private final String   expiryYear;
    private final CardType type;
    private       double   creditLimit;  // relevant for CREDIT cards
    private       double   availableCredit;

    public Card(String fullCardNumber, String cardHolder,
                String expiryMonth, String expiryYear, CardType type) {
        // Store only last 4 digits (security best practice)
        this.cardNumber      = "**** **** **** " + fullCardNumber.substring(fullCardNumber.length() - 4);
        this.cardHolder      = cardHolder;
        this.expiryMonth     = expiryMonth;
        this.expiryYear      = expiryYear;
        this.type            = type;
        this.creditLimit     = (type == CardType.CREDIT) ? 50000.0 : 0.0;
        this.availableCredit = this.creditLimit;
    }

    public boolean isExpired() {
        int month = Integer.parseInt(expiryMonth);
        int year  = Integer.parseInt(expiryYear);
        java.time.LocalDate now = java.time.LocalDate.now();
        return (year < now.getYear() % 100) ||
               (year == now.getYear() % 100 && month < now.getMonthValue());
    }

    public boolean chargeCredit(double amount) {
        if (type != CardType.CREDIT) return false;
        if (amount > availableCredit)  return false;
        availableCredit -= amount;
        return true;
    }

    // ── Getters ──────────────────────────────────────────────
    public String   getCardNumber()      { return cardNumber; }
    public String   getCardHolder()      { return cardHolder; }
    public CardType getType()            { return type; }
    public double   getAvailableCredit() { return availableCredit; }
    public String   getExpiry()          { return expiryMonth + "/" + expiryYear; }

    @Override
    public String toString() {
        return String.format("Card[%s | %s | %s | Exp: %s/%s]",
                cardNumber, cardHolder, type, expiryMonth, expiryYear);
    }
}
