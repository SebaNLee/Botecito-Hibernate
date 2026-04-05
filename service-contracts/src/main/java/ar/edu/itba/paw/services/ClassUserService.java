package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ClassUser;
import java.util.Optional;

public interface ClassUserService {
    ClassUser createClassUser(final String email, final String password, final String username);

    Optional<ClassUser> findClassUserByEmail(final String email);

    Optional<ClassUser> findClassUserById(final long id);
}
