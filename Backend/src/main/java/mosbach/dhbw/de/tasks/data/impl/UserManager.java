package mosbach.dhbw.de.tasks.data.impl;

import mosbach.dhbw.de.tasks.data.api.UserIF;
import mosbach.dhbw.de.tasks.model.TokenConv;
import mosbach.dhbw.de.tasks.model.UserConv;
import mosbach.dhbw.de.tasks.persistence.entity.UserEntity;
import mosbach.dhbw.de.tasks.persistence.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Service
public class UserManager {

    private final UserRepository userRepo;

    // Token bleibt erstmal wie im alten Projekt (token.properties).
    // Hinweis: In einem echten Setup gehört das in DB/JWT – aber wir bleiben minimal-invasiv.
    private final String tokendata = "token.properties";

    public UserManager(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Transactional
    public void addUser(UserIF user) {
        // Optionaler Guard (wie vorher gab's keinen)
        if (userRepo.existsByEmail(user.getEmail())) {
            return; // oder throw new IllegalArgumentException("Email already used");
        }

        UserEntity e = new UserEntity();
        e.setUserName(user.getUserName());
        e.setEmail(user.getEmail());

        // Minimal-Drift: erstmal "plain" in passwordHash speichern.
        // Später unbedingt BCrypt.
        e.setPasswordHash(user.getPassword());

        userRepo.save(e);
    }

    @Transactional(readOnly = true)
    public boolean checkUser(UserConv user) {
        return userRepo.findFirstByUserName(user.getUserName())
                .map(u -> passwordsMatch(user.getPassword(), u.getPasswordHash()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public UserConv searchUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .map(u -> new UserConv(u.getUserName(), u.getEmail(), u.getPasswordHash()))
                .orElse(new UserConv("User", "not", "found"));
    }

    // ==== Token-Teil (wie vorher, liest token.properties aus resources) ====

    public boolean checkToken(TokenConv token) {
        Properties properties = new Properties();
        try (InputStream resourceStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(tokendata)) {

            if (resourceStream == null) return false;

            properties.load(resourceStream);
            String Token = token.getToken();

            for (String key : properties.stringPropertyNames()) {
                if (key.matches("Auth\\.\\d+\\.Token")) {
                    String id = key.split("\\.")[1];
                    String storedToken = properties.getProperty("Auth." + id + ".Token");
                    if (Token.equals(storedToken)) {
                        String storedEmail = properties.getProperty("Auth." + id + ".Email");
                        // Token is only valid if mapped user still exists in DB
                        return storedEmail != null && userRepo.existsByEmail(storedEmail);
                    }
                }
            }
        } catch (IOException ignored) { }
        return false;
    }

    public UserConv TokenToUser(String token) {
        Properties properties = new Properties();
        try (InputStream resourceStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(tokendata)) {

            if (resourceStream == null) return null;

            properties.load(resourceStream);

            for (String key : properties.stringPropertyNames()) {
                if (key.matches("Auth\\.\\d+\\.Token")) {
                    String id = key.split("\\.")[1];
                    String storedToken = properties.getProperty("Auth." + id + ".Token");
                    if (token.equals(storedToken)) {
                        String storedEmail = properties.getProperty("Auth." + id + ".Email");
                        return searchUserByEmail(storedEmail);
                    }
                }
            }
        } catch (IOException ignored) { }
        return null;
    }

    @Transactional
    public boolean updateUserForTokenOwner(UserConv tokenOwner, UserConv update) {
        if (tokenOwner == null || tokenOwner.getEmail() == null) return false;
        UserEntity e = userRepo.findByEmail(tokenOwner.getEmail()).orElse(null);
        if (e == null) return false;

        if (update != null) {
            if (update.getUserName() != null && !update.getUserName().isBlank()) {
                e.setUserName(update.getUserName());
            }

            // IMPORTANT: email changes are not supported until we have real tokens/JWT,
            // because token.properties would still point to the old email.
            if (update.getEmail() != null && !update.getEmail().isBlank() && !update.getEmail().equals(e.getEmail())) {
                throw new IllegalArgumentException("Email change is not supported yet");
            }

            if (update.getPassword() != null && !update.getPassword().isBlank()) {
                // Minimal-Drift: still plain. Later: BCrypt.
                e.setPasswordHash(update.getPassword());
            }
        }

        userRepo.save(e);
        return true;
    }

    @Transactional
    public boolean deleteUserByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        UserEntity e = userRepo.findByEmail(email).orElse(null);
        if (e == null) return false;
        userRepo.delete(e);
        return true;
    }

    private boolean passwordsMatch(String raw, String stored) {
        // Minimal-Drift: plain equals.
        // Später: BCryptPasswordEncoder.matches(raw, storedHash)
        return raw != null && raw.equals(stored);
    }
}
