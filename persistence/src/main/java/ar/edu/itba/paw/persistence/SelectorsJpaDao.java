package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
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
    public List<Location> getLocationOptions() {
        final TypedQuery<Location> query =
                entityManager.createQuery("SELECT l FROM Location l ORDER BY l.id", Location.class);
        return query.getResultList();
    }

    @Override
    public List<ItemType> getItemTypeOptions() {
        final TypedQuery<ItemType> query =
                entityManager.createQuery("SELECT t FROM ItemType t ORDER BY t.name", ItemType.class);
        return query.getResultList();
    }
}
