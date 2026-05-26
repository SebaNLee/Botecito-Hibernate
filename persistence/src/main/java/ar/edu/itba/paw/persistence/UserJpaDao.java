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
        return user;
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
