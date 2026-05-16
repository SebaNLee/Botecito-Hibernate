package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.ItemTypeOrm;
import ar.edu.itba.paw.models.entity.LocationOrm;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class SelectorsJpaDao implements SelectorsDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<LocationOrm> getLocationOptions() {
        final TypedQuery<LocationOrm> query =
                entityManager.createQuery("SELECT l FROM LocationOrm l ORDER BY l.id", LocationOrm.class);
        return query.getResultList();
    }

    @Override
    public List<ItemTypeOrm> getItemTypeOptions() {
        final TypedQuery<ItemTypeOrm> query =
                entityManager.createQuery("SELECT t FROM ItemTypeOrm t ORDER BY t.name", ItemTypeOrm.class);
        return query.getResultList();
    }
}
