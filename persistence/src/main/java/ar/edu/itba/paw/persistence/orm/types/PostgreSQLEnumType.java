package ar.edu.itba.paw.persistence.orm.types;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.EnumType;

public class PostgreSQLEnumType extends EnumType<Enum<?>> {

    @Override
    public Enum<?> nullSafeGet(
            final ResultSet rs,
            final String[] names,
            final SharedSessionContractImplementor session,
            final Object owner)
            throws HibernateException, SQLException {
        final Object value = rs.getObject(names[0]);
        if (value == null) {
            return null;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        final Enum<?> enumValue = Enum.valueOf((Class) returnedClass(), value.toString());
        return enumValue;
    }

    @Override
    public void nullSafeSet(
            final PreparedStatement st,
            final Object value,
            final int index,
            final SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setObject(index, value.toString(), Types.OTHER);
        }
    }
}
