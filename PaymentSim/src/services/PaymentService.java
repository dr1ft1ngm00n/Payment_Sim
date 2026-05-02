package services;

import exceptions.PaymentException;
import models.Card;
import models.Transaction;
import models.Transaction.PaymentMethod;
import models.Transaction.Status;
import models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core payment engine:
 *  - Wallet transfers (peer-to-peer)
 *  - UPI payments
 *  - Card payments
 *  - Net banking (simulated)
 *  - Add money to wallet
 *  - Refunds
 *  - Transaction history
 */
public class PaymentService {

    private static final double MAX_SINGLE_TXN = 100_000.0;  // ₹1 lakh limit
    private static final double SERVICE_FEE_PCT = 0.0;        // 0% for demo

    private final List<Transaction> transactions = new ArrayList<>();
    private final UserService       userService;

    public PaymentService(UserService userService) {
        this.userService = userService;
    }

    // ─────────────────────────────────────────────────────────
    // 1. ADD MONEY TO WALLET (from external bank simulation)
    // ─────────────────────────────────────────────────────────
    public Transaction addMoneyToWallet(User user, double amount,
                                        String bankRef) throws PaymentException {
        validateAmount(amount);
        Transaction txn = new Transaction(
                "BANK", user.getUserId(), amount,
                PaymentMethod.NET_BANKING,
                "Add money via bank | Ref: " + bankRef);
        try {
            user.credit(amount);
            txn.setStatus(Status.SUCCESS);
            System.out.println("✔  ₹" + amount + " added to wallet.");
        } catch (Exception e) {
            txn.setStatus(Status.FAILED);
            throw new PaymentException("Failed to add money: " + e.getMessage(), e);
        } finally {
            transactions.add(txn);
        }
        return txn;
    }

    // ─────────────────────────────────────────────────────────
    // 2. WALLET TRANSFER (P2P)
    // ─────────────────────────────────────────────────────────
    public Transaction walletTransfer(User sender, String receiverEmail,
                                      double amount, String remarks) throws PaymentException {
        validateAmount(amount);
        User receiver = userService.getUserByEmail(receiverEmail);

        if (sender.getEmail().equals(receiver.getEmail())) {
            throw new PaymentException("Cannot transfer to yourself.");
        }

        Transaction txn = new Transaction(
                sender.getUserId(), receiver.getUserId(),
                amount, PaymentMethod.WALLET, remarks);
        try {
            sender.debit(amount);
            receiver.credit(amount);
            txn.setStatus(Status.SUCCESS);
            System.out.printf("✔  ₹%.2f transferred to %s.%n", amount, receiver.getName());
        } catch (IllegalStateException e) {
            txn.setStatus(Status.FAILED);
            throw new PaymentException("Transfer failed: " + e.getMessage(), e);
        } finally {
            transactions.add(txn);
        }
        return txn;
    }

    // ─────────────────────────────────────────────────────────
    // 3. UPI PAYMENT
    // ─────────────────────────────────────────────────────────
    public Transaction upiPayment(User sender, String upiId,
                                  double amount, String remarks) throws PaymentException {
        validateAmount(amount);
        // Simulate UPI lookup: derive userId from upiId (userId@paysim)
        String receiverEmail = upiId.replace("@paysim", "") + "@gmail.com";
        User receiver;
        try {
            receiver = userService.getUserByEmail(receiverEmail);
        } catch (PaymentException e) {
            throw new PaymentException("UPI ID not found: " + upiId);
        }

        Transaction txn = new Transaction(
                sender.getUserId(), receiver.getUserId(),
                amount, PaymentMethod.UPI,
                "UPI to " + upiId + " | " + remarks);
        try {
            sender.debit(amount);
            receiver.credit(amount);
            txn.setStatus(Status.SUCCESS);
            System.out.printf("✔  UPI payment of ₹%.2f to %s successful.%n",
                    amount, upiId);
        } catch (IllegalStateException e) {
            txn.setStatus(Status.FAILED);
            throw new PaymentException("UPI payment failed: " + e.getMessage(), e);
        } finally {
            transactions.add(txn);
        }
        return txn;
    }

    // ─────────────────────────────────────────────────────────
    // 4. CARD PAYMENT
    // ─────────────────────────────────────────────────────────
    public Transaction cardPayment(User sender, Card card,
                                   double amount, String merchant) throws PaymentException {
        validateAmount(amount);
        if (card.isExpired()) {
            throw new PaymentException("Card is expired.");
        }

        Transaction txn = new Transaction(
                sender.getUserId(), "MERCHANT:" + merchant,
                amount, PaymentMethod.CARD,
                "Card purchase at " + merchant);
        try {
            if (card.getType() == Card.CardType.DEBIT) {
                sender.debit(amount);  // deduct from wallet (linked account simulation)
            } else {
                // Credit card: charge credit limit
                if (!card.chargeCredit(amount)) {
                    throw new IllegalStateException("Credit limit exceeded.");
                }
            }
            txn.setStatus(Status.SUCCESS);
            System.out.printf("✔  Card payment of ₹%.2f to %s successful.%n",
                    amount, merchant);
        } catch (IllegalStateException e) {
            txn.setStatus(Status.FAILED);
            throw new PaymentException("Card payment failed: " + e.getMessage(), e);
        } finally {
            transactions.add(txn);
        }
        return txn;
    }

    // ─────────────────────────────────────────────────────────
    // 5. NET BANKING PAYMENT
    // ─────────────────────────────────────────────────────────
    public Transaction netBankingPayment(User sender, String bankName,
                                         double amount, String purpose) throws PaymentException {
        validateAmount(amount);

        // Simulate bank processing delay (just a print)
        System.out.println("⏳ Connecting to " + bankName + " Net Banking...");

        Transaction txn = new Transaction(
                sender.getUserId(), "BANK:" + bankName,
                amount, PaymentMethod.NET_BANKING,
                purpose + " via " + bankName);
        try {
            sender.debit(amount);
            txn.setStatus(Status.SUCCESS);
            System.out.printf("✔  Net Banking payment of ₹%.2f via %s successful.%n",
                    amount, bankName);
        } catch (IllegalStateException e) {
            txn.setStatus(Status.FAILED);
            throw new PaymentException("Net Banking failed: " + e.getMessage(), e);
        } finally {
            transactions.add(txn);
        }
        return txn;
    }

    // ─────────────────────────────────────────────────────────
    // 6. REFUND
    // ─────────────────────────────────────────────────────────
    public Transaction refund(String transactionId, User requestingUser) throws PaymentException {
        Transaction original = transactions.stream()
                .filter(t -> t.getTransactionId().equals(transactionId))
                .findFirst()
                .orElseThrow(() -> new PaymentException("Transaction not found: " + transactionId));

        if (original.getStatus() != Status.SUCCESS) {
            throw new PaymentException("Only successful transactions can be refunded.");
        }
        if (!original.getSenderId().equals(requestingUser.getUserId())) {
            throw new PaymentException("You can only refund your own transactions.");
        }

        // Credit back the sender
        requestingUser.credit(original.getAmount());
        original.setStatus(Status.REFUNDED);

        Transaction refundTxn = new Transaction(
                original.getReceiverId(), original.getSenderId(),
                original.getAmount(), original.getMethod(),
                "REFUND for " + original.getTransactionId());
        refundTxn.setStatus(Status.SUCCESS);
        transactions.add(refundTxn);

        System.out.printf("✔  Refund of ₹%.2f processed for %s.%n",
                original.getAmount(), transactionId);
        return refundTxn;
    }

    // ─────────────────────────────────────────────────────────
    // 7. HISTORY QUERIES
    // ─────────────────────────────────────────────────────────
    public List<Transaction> getHistory(String userId) {
        return transactions.stream()
                .filter(t -> t.getSenderId().equals(userId) ||
                             t.getReceiverId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────
    private void validateAmount(double amount) throws PaymentException {
        if (amount <= 0) {
            throw new PaymentException("Amount must be positive.");
        }
        if (amount > MAX_SINGLE_TXN) {
            throw new PaymentException("Single transaction limit is ₹" + MAX_SINGLE_TXN);
        }
    }
}
