package ui;

import exceptions.PaymentException;
import models.Card;
import models.Transaction;
import models.User;
import services.PaymentService;
import services.UserService;

import java.util.List;
import java.util.Scanner;

/**
 * Console-based UI for the Online Payment Simulator.
 * Handles all user interactions through a text menu.
 */
public class PaymentApp {

    private final Scanner        sc             = new Scanner(System.in);
    private final UserService    userService    = new UserService();
    private final PaymentService paymentService = new PaymentService(userService);

    private User currentUser = null;

    // ─── Colour / formatting helpers ─────────────────────────
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String GREEN  = "\u001B[32m";
    private static final String CYAN   = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED    = "\u001B[31m";

    private static String green(String s)  { return GREEN  + s + RESET; }
    private static String cyan(String s)   { return CYAN   + s + RESET; }
    private static String yellow(String s) { return YELLOW + s + RESET; }
    private static String red(String s)    { return RED    + s + RESET; }
    private static String bold(String s)   { return BOLD   + s + RESET; }

    // ──────────────────────────────────────────────────────────
    public void run() {
        printBanner();
        while (true) {
            if (currentUser == null) {
                showAuthMenu();
            } else {
                showMainMenu();
            }
        }
    }

    // ─── Banner ───────────────────────────────────────────────
    private void printBanner() {
        System.out.println();

        System.out.println(bold("       ONLINE PAYMENT SIMULATOR         "));
        System.out.println( "      2nd Semester IT Workshop Project              " );
        System.out.println(yellow("  Demo accounts:  alice@gmail.com / PIN: 1234"));
        System.out.println(yellow("                  bob@gmail.com   / PIN: 5678"));
        System.out.println();
    }

    // ─── Auth Menu ────────────────────────────────────────────
    private void showAuthMenu() {
        System.out.println(bold("\n── Authentication ──────────────────────────────"));
        System.out.println("  1. Login");
        System.out.println("  2. Register");
        System.out.println("  0. Exit");
        System.out.print(cyan("Choice: "));
        String choice = sc.nextLine().trim();

        switch (choice) {
            case "1" -> doLogin();
            case "2" -> doRegister();
            case "0" -> { System.out.println(green("Goodbye! 👋")); System.exit(0); }
            default  -> System.out.println(red("Invalid option."));
        }
    }

    private void doLogin() {
        System.out.print("Email: ");
        String email = sc.nextLine().trim();
        System.out.print("PIN  : ");
        String pin = sc.nextLine().trim();
        try {
            currentUser = userService.login(email, pin);
            System.out.println(green("✔  Welcome back, " + currentUser.getName() + "!"));
        } catch (PaymentException e) {
            System.out.println(red("Login failed: " + e.getMessage()));
        }
    }

    private void doRegister() {
        System.out.print("Full Name : ");
        String name = sc.nextLine().trim();
        System.out.print("Email     : ");
        String email = sc.nextLine().trim();
        System.out.print("Phone     : ");
        String phone = sc.nextLine().trim();
        System.out.print("Set PIN   : ");
        String pin = sc.nextLine().trim();
        try {
            User u = userService.register(name, email, phone, pin);
            System.out.println(green("✔  Account created! ID: " + u.getUserId()));
        } catch (PaymentException e) {
            System.out.println(red("Registration failed: " + e.getMessage()));
        }
    }

    // ─── Main Menu ────────────────────────────────────────────
    private void showMainMenu() {
        System.out.printf("%n" + bold("── Dashboard (%s) ───────────────────────────%n"),
                currentUser.getName());
        System.out.printf("  Wallet Balance: " + green("₹%.2f%n"), currentUser.getWalletBalance());
        System.out.println("────────────────────────────────────────────────");
        System.out.println("  1. Add Money to Wallet");
        System.out.println("  2. Send Money (Wallet)");
        System.out.println("  3. Pay via UPI");
        System.out.println("  4. Pay via Card");
        System.out.println("  5. Pay via Net Banking");
        System.out.println("  6. Transaction History");
        System.out.println("  7. Request Refund");
        System.out.println("  8. View All Users");
        System.out.println("  9. Logout");
        System.out.print(cyan("Choice: "));
        String choice = sc.nextLine().trim();

        switch (choice) {
            case "1" -> doAddMoney();
            case "2" -> doWalletTransfer();
            case "3" -> doUpiPayment();
            case "4" -> doCardPayment();
            case "5" -> doNetBanking();
            case "6" -> showHistory();
            case "7" -> doRefund();
            case "8" -> listUsers();
            case "9" -> { currentUser = null; System.out.println(yellow("Logged out.")); }
            default  -> System.out.println(red("Invalid option."));
        }
    }

    // ─── Feature implementations ──────────────────────────────

    private void doAddMoney() {
        double amount = promptAmount();
        System.out.print("Bank Reference # : ");
        String ref = sc.nextLine().trim();
        try {
            paymentService.addMoneyToWallet(currentUser, amount, ref);
        } catch (PaymentException e) {
            System.out.println(red("Error: " + e.getMessage()));
        }
    }

    private void doWalletTransfer() {
        System.out.print("Receiver Email : ");
        String email = sc.nextLine().trim();
        double amount = promptAmount();
        System.out.print("Remarks        : ");
        String remarks = sc.nextLine().trim();
        try {
            Transaction t = paymentService.walletTransfer(currentUser, email, amount, remarks);
            printTxnReceipt(t);
        } catch (PaymentException e) {
            System.out.println(red("Error: " + e.getMessage()));
        }
    }

    private void doUpiPayment() {
        System.out.println(yellow("Format: username@paysim  e.g.  alice@paysim"));
        System.out.print("UPI ID  : ");
        String upiId = sc.nextLine().trim();
        double amount = promptAmount();
        System.out.print("Remarks : ");
        String remarks = sc.nextLine().trim();
        try {
            Transaction t = paymentService.upiPayment(currentUser, upiId, amount, remarks);
            printTxnReceipt(t);
        } catch (PaymentException e) {
            System.out.println(red("Error: " + e.getMessage()));
        }
    }

    private void doCardPayment() {
        System.out.print("Card Number (16 digits) : ");
        String cardNum = sc.nextLine().trim().replaceAll("\\s+", "");
        System.out.print("Card Holder Name        : ");
        String holder = sc.nextLine().trim();
        System.out.print("Expiry Month (MM)       : ");
        String month = sc.nextLine().trim();
        System.out.print("Expiry Year  (YY)       : ");
        String year = sc.nextLine().trim();
        System.out.print("Card Type (1=Debit 2=Credit): ");
        String typeInput = sc.nextLine().trim();
        Card.CardType type = typeInput.equals("2") ? Card.CardType.CREDIT : Card.CardType.DEBIT;

        Card card = new Card(cardNum, holder, month, year, type);
        System.out.println(cyan("Card linked: " + card));

        System.out.print("Merchant / Payee : ");
        String merchant = sc.nextLine().trim();
        double amount = promptAmount();

        try {
            Transaction t = paymentService.cardPayment(currentUser, card, amount, merchant);
            printTxnReceipt(t);
        } catch (PaymentException e) {
            System.out.println(red("Error: " + e.getMessage()));
        }
    }

    private void doNetBanking() {
        System.out.println("Banks: 1.SBI  2.HDFC  3.ICICI  4.Axis  5.Other");
        System.out.print("Choose bank: ");
        String b = sc.nextLine().trim();
        String bank = switch (b) {
            case "1" -> "SBI";
            case "2" -> "HDFC";
            case "3" -> "ICICI";
            case "4" -> "Axis";
            default  -> "Other Bank";
        };
        double amount = promptAmount();
        System.out.print("Purpose : ");
        String purpose = sc.nextLine().trim();
        try {
            Transaction t = paymentService.netBankingPayment(currentUser, bank, amount, purpose);
            printTxnReceipt(t);
        } catch (PaymentException e) {
            System.out.println(red("Error: " + e.getMessage()));
        }
    }

    private void showHistory() {
        List<Transaction> history = paymentService.getHistory(currentUser.getUserId());
        if (history.isEmpty()) {
            System.out.println(yellow("No transactions found."));
            return;
        }
        System.out.println(bold("\n── Transaction History ──────────────────────────────────────────────────────────────────────────────────"));
        System.out.printf("%-12s | %-8s → %-8s | %11s | %-11s | %-9s | %s%n",
                "TXN ID", "FROM", "TO", "AMOUNT", "METHOD", "STATUS", "TIMESTAMP");
        System.out.println("-".repeat(105));
        history.forEach(t -> System.out.println(t));
    }

    private void doRefund() {
        System.out.print("Transaction ID to refund : ");
        String txnId = sc.nextLine().trim();
        try {
            Transaction t = paymentService.refund(txnId, currentUser);
            System.out.println(green("Refund TXN: " + t.getTransactionId()));
        } catch (PaymentException e) {
            System.out.println(red("Error: " + e.getMessage()));
        }
    }

    private void listUsers() {
        System.out.println(bold("\n── Registered Users ─────────────────────────────"));
        userService.getAllUsers().forEach(u -> System.out.println("  " + u));
    }

    // ─── Helpers ──────────────────────────────────────────────
    private double promptAmount() {
        while (true) {
            System.out.print("Amount (₹) : ");
            try {
                return Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(red("Please enter a valid number."));
            }
        }
    }

    private void printTxnReceipt(Transaction t) {
        System.out.println(green("\n┌─────────── Payment Receipt ─────────────┐"));
        System.out.printf(green("│") + "  TXN ID  : %-29s" + green("│%n"), t.getTransactionId());
        System.out.printf(green("│") + "  Amount  : ₹%-28.2f" + green("│%n"), t.getAmount());
        System.out.printf(green("│") + "  Method  : %-29s" + green("│%n"), t.getMethod());
        System.out.printf(green("│") + "  Status  : %-29s" + green("│%n"), t.getStatus());
        System.out.println(green("└─────────────────────────────────────────┘"));
    }
}
