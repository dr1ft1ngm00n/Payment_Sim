package services;

import exceptions.PaymentException;
import models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages user accounts: registration, login, and lookups.
 */
public class UserService {

    // userId → User
    private final Map<String, User> usersById    = new HashMap<>();
    // email  → User  (for login)
    private final Map<String, User> usersByEmail = new HashMap<>();

    public UserService() {
        // Seed two demo users
        seedDemoUsers();
    }

    private void seedDemoUsers() {
        try {
            register("Alice",   "alice@gmail.com",  "9876543210", "1234");
            register("Bob",    "bob@gmail.com",    "9123456780", "5678");
        } catch (PaymentException ignored) {}

        // Give demo users some wallet balance
        try {
            getUserByEmail("alice@gmail.com").credit(5000.0);
            getUserByEmail("bob@gmail.com").credit(3000.0);
        } catch (PaymentException ignored) {}
    }

    /** Registers a new user and returns the created User. */
    public User register(String name, String email,
                         String phone, String pin) throws PaymentException {
        if (usersByEmail.containsKey(email.toLowerCase())) {
            throw new PaymentException("Email already registered: " + email);
        }
        if (pin == null || pin.length() < 4) {
            throw new PaymentException("PIN must be at least 4 digits.");
        }
        User user = new User(name, email.toLowerCase(), phone, pin);
        usersById.put(user.getUserId(), user);
        usersByEmail.put(email.toLowerCase(), user);
        return user;
    }

    /** Authenticates a user by email + PIN. Returns the User if successful. */
    public User login(String email, String pin) throws PaymentException {
        User user = getUserByEmail(email);
        if (!user.verifyPin(pin)) {
            throw new PaymentException("Incorrect PIN.");
        }
        return user;
    }

    /** Looks up a user by email; throws if not found. */
    public User getUserByEmail(String email) throws PaymentException {
        User user = usersByEmail.get(email.toLowerCase());
        if (user == null) {
            throw new PaymentException("No user found with email: " + email);
        }
        return user;
    }

    /** Looks up a user by userId; throws if not found. */
    public User getUserById(String userId) throws PaymentException {
        User user = usersById.get(userId);
        if (user == null) {
            throw new PaymentException("No user found with ID: " + userId);
        }
        return user;
    }

    /** Returns an immutable list of all registered users. */
    public List<User> getAllUsers() {
        return new ArrayList<>(usersById.values());
    }
}
