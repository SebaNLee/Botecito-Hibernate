package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Users;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class UserJpaDao implements UserDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Users> findById(final int id) {
        return Optional.ofNullable(entityManager.find(Users.class, id));
    }

    @Override
    public Optional<Users> findByEmail(final String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return entityManager
                .createQuery("FROM Users u WHERE LOWER(u.email) = LOWER(:email)", Users.class)
                .setParameter("email", email.trim())
                .getResultStream()
                .findFirst();
    }

    @Override
    public Users createUser(final Users user) {
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    @Override
    public Optional<Users> claimUser(final Users user) {
        return findByEmail(user.getEmail())
                .filter(existing -> existing.getPasswordHash() == null)
                .map(existing -> {
                    existing.setFirstName(user.getFirstName());
                    existing.setLastName(user.getLastName());
                    existing.setLanguage(user.getLanguage());
                    existing.setPasswordHash(user.getPasswordHash());
                    existing.setMailToken(user.getMailToken());
                    existing.setMailTokenEmittedAt(user.getMailTokenEmittedAt());
                    existing.setVerified(user.getVerified() != null && user.getVerified());
                    if (user.getAlias() != null) {
                        existing.setAlias(user.getAlias());
                    }
                    entityManager.flush();
                    return existing;
                });
    }

    @Override
    public Optional<Users> updateProfile(final Users user) {
        if (user.getId() == null) {
            return Optional.empty();
        }
        final Users existing = entityManager.find(Users.class, user.getId());
        if (existing == null) {
            return Optional.empty();
        }
        existing.setFirstName(user.getFirstName());
        existing.setLastName(user.getLastName());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        existing.setAlias(user.getAlias());
        existing.setLanguage(user.getLanguage());
        existing.setVerified(user.getVerified() != null && user.getVerified());
        existing.setMailToken(user.getMailToken());
        existing.setMailTokenEmittedAt(user.getMailTokenEmittedAt());
        entityManager.flush();
        return Optional.of(existing);
    }

    @Override
    public Optional<Users> updatePasswordRecoveryToken(final int userId, final String mailToken) {
        final Users existing = entityManager.find(Users.class, userId);
        if (existing == null || !Boolean.TRUE.equals(existing.getVerified())) {
            return Optional.empty();
        }
        existing.setMailToken(mailToken);
        existing.setMailTokenEmittedAt(null);
        entityManager.flush();
        return Optional.of(existing);
    }

    @Override
    public Optional<Users> findByPasswordRecoveryToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return entityManager
                .createQuery("FROM Users u WHERE u.mailToken = :token AND u.verified = true", Users.class)
                .setParameter("token", token.trim())
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean resetPasswordByRecoveryToken(
            final String token, final String passwordHash, final LocalDateTime usedAt) {
        final int updatedRows = entityManager
                .createQuery("UPDATE Users u"
                        + " SET u.passwordHash = :passwordHash, u.mailTokenEmittedAt = :usedAt"
                        + " WHERE u.mailToken = :token AND u.mailTokenEmittedAt IS NULL AND u.verified = true")
                .setParameter("passwordHash", passwordHash)
                .setParameter("usedAt", usedAt)
                .setParameter("token", token)
                .executeUpdate();
        return updatedRows > 0;
    }

    @Override
    public Optional<Users> findByEmailVerificationToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return entityManager
                .createQuery("FROM Users u WHERE u.mailToken = :token AND u.verified = false", Users.class)
                .setParameter("token", token.trim())
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<Users> verifyEmailByToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        final Optional<Users> user = entityManager
                .createQuery("FROM Users u WHERE u.mailToken = :token AND u.verified = false", Users.class)
                .setParameter("token", token.trim())
                .getResultStream()
                .findFirst();
        if (user.isEmpty()) {
            return Optional.empty();
        }
        final Users existing = user.get();
        existing.setVerified(true);
        existing.setMailToken(null);
        existing.setMailTokenEmittedAt(null);
        entityManager.flush();
        return Optional.of(existing);
    }

    @Override
    public List<Users> findUsersByIds(final Collection<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return entityManager
                .createQuery("FROM Users u WHERE u.id IN :ids", Users.class)
                .setParameter("ids", userIds)
                .getResultStream()
                .toList();
    }
}
