package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a payment transaction in the system.
 */
public class Transaction {

    public enum PaymentMethod { WALLET, UPI, CARD, NET_BANKING }
    public enum Status        { PENDING, SUCCESS, FAILED, REFUNDED }

    private static int txnCounter = 100001;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final String        transactionId;
    private final String        senderId;
    private final String        receiverId;
    private final double        amount;
    private final PaymentMethod method;
    private       Status        status;
    private final LocalDateTime timestamp;
    private       String        remarks;

    public Transaction(String senderId, String receiverId,
                       double amount, PaymentMethod method, String remarks) {
        this.transactionId = "TXN" + (txnCounter++);
        this.senderId      = senderId;
        this.receiverId    = receiverId;
        this.amount        = amount;
        this.method        = method;
        this.status        = Status.PENDING;
        this.timestamp     = LocalDateTime.now();
        this.remarks       = remarks;
    }

    // ── Getters ──────────────────────────────────────────────
    public String        getTransactionId() { return transactionId; }
    public String        getSenderId()      { return senderId; }
    public String        getReceiverId()    { return receiverId; }
    public double        getAmount()        { return amount; }
    public PaymentMethod getMethod()        { return method; }
    public Status        getStatus()        { return status; }
    public LocalDateTime getTimestamp()     { return timestamp; }
    public String        getRemarks()       { return remarks; }

    public void setStatus(Status status) { this.status = status; }

    @Override
    public String toString() {
        return String.format(
            "%-12s | %-8s → %-8s | ₹%9.2f | %-11s | %-9s | %s",
            transactionId, senderId, receiverId, amount,
            method, status, timestamp.format(FMT));
    }
}
