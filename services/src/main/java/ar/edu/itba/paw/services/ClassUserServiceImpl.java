package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ClassUser;
import ar.edu.itba.paw.persistence.ClassUserDao;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClassUserServiceImpl implements ClassUserService {
    private final ClassUserDao classUserDao;

    @Autowired
    public ClassUserServiceImpl(final ClassUserDao classUserDao) {
        this.classUserDao = classUserDao;
    }

    @Override
    public ClassUser createClassUser(final String email, final String password, final String username) {
        return classUserDao.createClassUser(email, password, username);
    }

    @Override
    public Optional<ClassUser> findClassUserByEmail(final String email) {
        return classUserDao.findClassUserByEmail(email);
    }

    @Override
    public Optional<ClassUser> findClassUserById(final long id) {
        return classUserDao.findClassUserById(id);
    }
}
