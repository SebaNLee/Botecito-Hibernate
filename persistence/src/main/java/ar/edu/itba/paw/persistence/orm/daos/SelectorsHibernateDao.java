package ar.edu.itba.paw.persistence.orm.daos;

import ar.edu.itba.paw.models.nuevo.ItemTypeModel;
import ar.edu.itba.paw.models.nuevo.Location;
import ar.edu.itba.paw.persistence.nuevo.SelectorsDao;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class SelectorsHibernateDao implements SelectorsDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Location> getLocationOptions() {
        final List<Object[]> rows = entityManager
                .createQuery("SELECT l.id, l.name, l.slug FROM LocationOrm l ORDER BY l.id", Object[].class)
                .getResultList();
        return rows.stream()
                .map(row -> {
                    final Location option = new Location();
                    option.setId((Integer) row[0]);
                    option.setName((String) row[1]);
                    option.setSlug((String) row[2]);
                    return option;
                })
                .toList();
    }

    @Override
    public List<ItemTypeModel> getItemTypeOptions() {
        final List<Object[]> rows = entityManager
                .createQuery("SELECT t.id, t.name, t.slug FROM ItemTypeOrm t ORDER BY t.name", Object[].class)
                .getResultList();
        return rows.stream()
                .map(row -> {
                    final ItemTypeModel option = new ItemTypeModel();
                    option.setId((Integer) row[0]);
                    option.setName((String) row[1]);
                    option.setSlug((String) row[2]);
                    return option;
                })
                .toList();
    }
}
