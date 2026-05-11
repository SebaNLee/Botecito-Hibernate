package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.persistence.nuevo.UserDao;
import ar.edu.itba.paw.persistence.orm.entities.UsersOrm;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class UserHibernateDao implements UserDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserModel> findById(final int id) {
        return Optional.ofNullable(entityManager.find(UsersOrm.class, id)).map(UserHibernateDao::toUserModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserModel> findByEmail(final String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return entityManager
                .createQuery("FROM UsersOrm u WHERE LOWER(u.email) = LOWER(:email)", UsersOrm.class)
                .setParameter("email", email.trim())
                .getResultStream()
                .findFirst()
                .map(UserHibernateDao::toUserModel);
    }

    @Override
    public UserModel createUser(final UserModel user) {
        final UsersOrm orm = new UsersOrm();
        orm.setFirstName(user.getGivenName());
        orm.setLastName(user.getLastName());
        orm.setEmail(user.getEmail());
        orm.setLanguage(persistenceLanguage(user));
        orm.setPasswordHash(user.getPasswordHash());
        orm.setMailToken(user.getPasswordRecoveryToken());
        orm.setMailTokenEmittedAt(toLocalDateTime(user.getPasswordRecoveryUsedAt()));
        orm.setAlias(user.getPaymentAlias());
        orm.setPhone(user.getPhone());
        orm.setVerified(user.isVerified());
        orm.setCreatedAt(LocalDateTime.now());
        entityManager.persist(orm);
        entityManager.flush();
        return toUserModel(orm);
    }

    @Override
    public Optional<UserModel> claimUser(final UserModel user) {
        return findOrmByEmail(user.getEmail())
                .filter(existing -> existing.getPasswordHash() == null)
                .map(existing -> {
                    existing.setFirstName(user.getGivenName());
                    existing.setLastName(user.getLastName());
                    existing.setLanguage(persistenceLanguage(user));
                    existing.setPasswordHash(user.getPasswordHash());
                    existing.setMailToken(user.getPasswordRecoveryToken());
                    existing.setMailTokenEmittedAt(toLocalDateTime(user.getPasswordRecoveryUsedAt()));
                    existing.setVerified(user.isVerified());
                    if (user.getPaymentAlias() != null) {
                        existing.setAlias(user.getPaymentAlias());
                    }
                    entityManager.flush();
                    return toUserModel(existing);
                });
    }

    @Override
    public Optional<UserModel> updateProfile(final UserModel user) {
        if (user.getId() == null) {
            return Optional.empty();
        }
        final UsersOrm existing = entityManager.find(UsersOrm.class, user.getId());
        if (existing == null) {
            return Optional.empty();
        }
        existing.setFirstName(user.getGivenName());
        existing.setLastName(user.getLastName());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        existing.setAlias(user.getPaymentAlias());
        existing.setLanguage(persistenceLanguage(user));
        existing.setVerified(user.isVerified());
        existing.setMailToken(user.getPasswordRecoveryToken());
        existing.setMailTokenEmittedAt(toLocalDateTime(user.getPasswordRecoveryUsedAt()));
        entityManager.flush();
        return Optional.of(toUserModel(existing));
    }

    @Override
    public Optional<UserModel> updatePasswordRecoveryToken(final UserModel user) {
        if (user.getId() == null) {
            return Optional.empty();
        }
        final UsersOrm existing = entityManager.find(UsersOrm.class, user.getId());
        if (existing == null) {
            return Optional.empty();
        }
        if (!Boolean.TRUE.equals(existing.getVerified())) {
            return Optional.empty();
        }
        existing.setMailToken(user.getPasswordRecoveryToken());
        existing.setMailTokenEmittedAt(null);
        entityManager.flush();
        return Optional.of(toUserModel(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserModel> findByPasswordRecoveryToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return entityManager
                .createQuery("FROM UsersOrm u WHERE u.mailToken = :token AND u.verified = true", UsersOrm.class)
                .setParameter("token", token.trim())
                .getResultStream()
                .findFirst()
                .map(UserHibernateDao::toUserModel);
    }

    @Override
    public boolean resetPasswordByRecoveryToken(final UserModel user) {
        final int updatedRows = entityManager
                .createQuery("UPDATE UsersOrm u"
                        + " SET u.passwordHash = :passwordHash, u.mailTokenEmittedAt = :usedAt"
                        + " WHERE u.mailToken = :token AND u.mailTokenEmittedAt IS NULL AND u.verified = true")
                .setParameter("passwordHash", user.getPasswordHash())
                .setParameter("usedAt", toLocalDateTime(user.getPasswordRecoveryUsedAt()))
                .setParameter("token", user.getPasswordRecoveryToken())
                .executeUpdate();
        return updatedRows > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserModel> findByEmailVerificationToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return entityManager
                .createQuery("FROM UsersOrm u WHERE u.mailToken = :token AND u.verified = false", UsersOrm.class)
                .setParameter("token", token.trim())
                .getResultStream()
                .findFirst()
                .map(UserHibernateDao::toUserModel);
    }

    @Override
    public Optional<UserModel> verifyEmailByToken(final String token) {
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
        return Optional.of(toUserModel(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserModel> findUsersByIds(final Collection<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return entityManager
                .createQuery("FROM UsersOrm u WHERE u.id IN :ids", UsersOrm.class)
                .setParameter("ids", userIds)
                .getResultStream()
                .map(UserHibernateDao::toUserModel)
                .toList();
    }

    private Optional<UsersOrm> findOrmByEmail(final String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return entityManager
                .createQuery("FROM UsersOrm u WHERE LOWER(u.email) = LOWER(:email)", UsersOrm.class)
                .setParameter("email", email.trim())
                .getResultStream()
                .findFirst();
    }

    private static UserModel toUserModel(final UsersOrm orm) {
        final UserModel user = new UserModel();
        user.setId(orm.getId());
        user.setCreatedAt(toOffsetDateTime(orm.getCreatedAt()));
        user.setGivenName(orm.getFirstName());
        user.setLastName(orm.getLastName());
        user.setEmail(orm.getEmail());
        user.setPhone(orm.getPhone());
        user.setPaymentAlias(orm.getAlias());
        user.setPreferredLanguage(PreferredLanguageModel.fromPersistence(orm.getLanguage()));
        user.setPasswordHash(orm.getPasswordHash());
        user.setPasswordRecoveryToken(orm.getMailToken());
        user.setPasswordRecoveryUsedAt(toOffsetDateTime(orm.getMailTokenEmittedAt()));
        user.setVerified(Boolean.TRUE.equals(orm.getVerified()));
        return user;
    }

    private static String persistenceLanguage(final UserModel user) {
        return user.getPreferredLanguage() == null
                ? PreferredLanguageModel.ES.getPersistenceCode()
                : user.getPreferredLanguage().getPersistenceCode();
    }

    private static OffsetDateTime toOffsetDateTime(final LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static LocalDateTime toLocalDateTime(final OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
