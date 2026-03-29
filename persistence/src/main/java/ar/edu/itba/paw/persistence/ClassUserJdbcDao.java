package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ClassUser;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

@Repository
public class ClassUserJdbcDao implements ClassUserDao {

    private static final RowMapper<ClassUser> CLASS_USER_ROW_MAPPER = (ResultSet rs, int rowNum) ->
            new ClassUser(rs.getLong("id"), rs.getString("email"), rs.getString("password"), rs.getString("username"));

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public ClassUserJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert =
                new SimpleJdbcInsert(dataSource).withTableName("class_users").usingGeneratedKeyColumns("id");
    }

    @Override
    public ClassUser createClassUser(final String email, final String password, final String username) {
        final Map<String, Object> values = new HashMap<>();
        values.put("email", email);
        values.put("password", password);
        values.put("username", username);
        final Number id = jdbcInsert.executeAndReturnKey(values);

        return new ClassUser(id.longValue(), email, password, username);
    }

    @Override
    public Optional<ClassUser> findClassUserByEmail(final String email) {
        return jdbcTemplate.query("SELECT * FROM class_users WHERE email = ?", CLASS_USER_ROW_MAPPER, email).stream()
                .findAny();
    }

    @Override
    public Optional<ClassUser> findClassUserById(final long id) {
        return jdbcTemplate.query("SELECT * FROM class_users WHERE id = ?", CLASS_USER_ROW_MAPPER, id).stream()
                .findAny();
    }
}
