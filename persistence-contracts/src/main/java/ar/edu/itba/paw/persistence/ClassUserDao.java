package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ClassUser;
import java.util.Optional;

public interface ClassUserDao {
    ClassUser createClassUser(final String email, final String password, final String username);

    Optional<ClassUser> findClassUserByEmail(final String email);

    Optional<ClassUser> findClassUserById(final long id);
}
