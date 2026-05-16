package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.persistence.nuevo.UserDao;
import ar.edu.itba.paw.models.entity.UsersOrm;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class UserHibernateDao implements UserDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<UsersOrm> findById(final int id) {
        return Optional.ofNullable(entityManager.find(UsersOrm.class, id));
    }

    @Override
    public Optional<UsersOrm> findByEmail(final String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return entityManager
                .createQuery("FROM UsersOrm u WHERE LOWER(u.email) = LOWER(:email)", UsersOrm.class)
                .setParameter("email", email.trim())
                .getResultStream()
                .findFirst();
    }

    @Override
    public UsersOrm createUser(final UsersOrm user) {
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    @Override
    public Optional<UsersOrm> claimUser(final UsersOrm user) {
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
    public Optional<UsersOrm> updateProfile(final UsersOrm user) {
        if (user.getId() == null) {
            return Optional.empty();
        }
        final UsersOrm existing = entityManager.find(UsersOrm.class, user.getId());
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
    public Optional<UsersOrm> updatePasswordRecoveryToken(final int userId, final String mailToken) {
        final UsersOrm existing = entityManager.find(UsersOrm.class, userId);
        if (existing == null || !Boolean.TRUE.equals(existing.getVerified())) {
            return Optional.empty();
        }
        existing.setMailToken(mailToken);
        existing.setMailTokenEmittedAt(null);
        entityManager.flush();
        return Optional.of(existing);
    }

    @Override
    public Optional<UsersOrm> findByPasswordRecoveryToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return entityManager
                .createQuery("FROM UsersOrm u WHERE u.mailToken = :token AND u.verified = true", UsersOrm.class)
                .setParameter("token", token.trim())
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean resetPasswordByRecoveryToken(final String token, final String passwordHash, final LocalDateTime usedAt) {
        final int updatedRows = entityManager
                .createQuery("UPDATE UsersOrm u"
                        + " SET u.passwordHash = :passwordHash, u.mailTokenEmittedAt = :usedAt"
                        + " WHERE u.mailToken = :token AND u.mailTokenEmittedAt IS NULL AND u.verified = true")
                .setParameter("passwordHash", passwordHash)
                .setParameter("usedAt", usedAt)
                .setParameter("token", token)
                .executeUpdate();
        return updatedRows > 0;
    }

    @Override
    public Optional<UsersOrm> findByEmailVerificationToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return entityManager
                .createQuery("FROM UsersOrm u WHERE u.mailToken = :token AND u.verified = false", UsersOrm.class)
                .setParameter("token", token.trim())
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<UsersOrm> verifyEmailByToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        final Optional<UsersOrm> user = entityManager
                .createQuery("FROM UsersOrm u WHERE u.mailToken = :token AND u.verified = false", UsersOrm.class)
                .setParameter("token", token.trim())
                .getResultStream()
                .findFirst();
        if (user.isEmpty()) {
            return Optional.empty();
        }
        final UsersOrm existing = user.get();
        existing.setVerified(true);
        existing.setMailToken(null);
        existing.setMailTokenEmittedAt(null);
        entityManager.flush();
        return Optional.of(existing);
    }

    @Override
    public List<UsersOrm> findUsersByIds(final Collection<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return entityManager
                .createQuery("FROM UsersOrm u WHERE u.id IN :ids", UsersOrm.class)
                .setParameter("ids", userIds)
                .getResultStream()
                .toList();
    }
}
