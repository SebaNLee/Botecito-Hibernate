package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;

public interface UserDao {
    public User createUser(final String email, final String password, final String username);
}
