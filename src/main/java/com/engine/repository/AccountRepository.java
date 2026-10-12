package com.engine.repository;

import com.engine.exception.DuplicateEntityException;
import com.engine.model.Account;
import com.engine.model.AccountType;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Specialized repository for Account entities with email uniqueness indexing
 * and polymorphic query capabilities.
 */
public class AccountRepository extends InMemoryRepository<Account, String> {

    private final ConcurrentMap<String, Account> emailIndex = new ConcurrentHashMap<>();

    @Override
    public synchronized Account save(Account account) {
        if (account == null) {
            return super.save(null);
        }

        Account existingByEmail = emailIndex.get(account.getEmail().toLowerCase());
        if (existingByEmail != null && !existingByEmail.getId().equals(account.getId())) {
            throw new DuplicateEntityException("Account", "email", account.getEmail());
        }

        Account saved = super.save(account);
        emailIndex.put(account.getEmail().toLowerCase(), saved);
        return saved;
    }

    public Optional<Account> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(emailIndex.get(email.toLowerCase()));
    }

    public List<Account> findByType(AccountType type) {
        if (type == null) {
            return List.of();
        }
        return storage.values().stream()
                .filter(a -> a.getAccountType() == type)
                .toList();
    }

    @Override
    public synchronized boolean deleteById(String id) {
        Optional<Account> existing = findById(id);
        if (existing.isPresent()) {
            emailIndex.remove(existing.get().getEmail().toLowerCase());
            return super.deleteById(id);
        }
        return false;
    }

    @Override
    public synchronized void clear() {
        super.clear();
        emailIndex.clear();
    }
}
