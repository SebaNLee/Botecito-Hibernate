package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.CatalogUser;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemMedia;
import ar.edu.itba.paw.models.ItemType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public final class ItemCatalogServiceImpl implements ItemCatalogService {
    private static final String ITEMS_JSON_PATH = "mock/items.json";
    private final CatalogData catalogData;

    public ItemCatalogServiceImpl() {
        this.catalogData = loadCatalogData();
    }

    @Override
    public List<Item> listItems() {
        return catalogData.getItem();
    }

    @Override
    public Optional<Item> findItemById(final int id) {
        return catalogData.getItem().stream().filter(item -> item.getId() == id).findFirst();
    }

    @Override
    public Optional<CatalogUser> findUserById(final int id) {
        return catalogData.getUsers().stream()
                .filter(user -> user.getId() == id)
                .findFirst();
    }

    @Override
    public Optional<ItemType> findItemTypeById(final int id) {
        return catalogData.getItemType().stream()
                .filter(itemType -> itemType.getId() == id)
                .findFirst();
    }

    @Override
    public Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId) {
        return catalogData.getItemAvailability().stream()
                .filter(itemAvailability -> itemAvailability.getItemId() == itemId)
                .min(Comparator.comparing(ItemAvailability::getStartTime));
    }

    @Override
    public Optional<String> findImageUrlByItemId(final int itemId) {
        return catalogData.getItemMedia().stream()
                .filter(itemMedia -> itemMedia.getItemId() == itemId)
                .map(ItemMedia::getImageUrl)
                .findFirst();
    }

    private static CatalogData loadCatalogData() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        try (InputStream inputStream = new ClassPathResource(ITEMS_JSON_PATH).getInputStream()) {
            return objectMapper.readValue(inputStream, CatalogData.class);
        } catch (final IOException e) {
            throw new UncheckedIOException("Could not load mock items JSON", e);
        }
    }

    public static class CatalogData {
        private List<CatalogUser> users;
        private List<ItemType> itemType;
        private List<Item> item;
        private List<ItemAvailability> itemAvailability;
        private List<ItemMedia> itemMedia;

        public List<CatalogUser> getUsers() {
            return List.copyOf(users);
        }

        public void setUsers(final List<CatalogUser> users) {
            this.users = immutableCopy(users);
        }

        public List<ItemType> getItemType() {
            return List.copyOf(itemType);
        }

        public void setItemType(final List<ItemType> itemType) {
            this.itemType = immutableCopy(itemType);
        }

        public List<Item> getItem() {
            return List.copyOf(item);
        }

        public void setItem(final List<Item> item) {
            this.item = immutableCopy(item);
        }

        public List<ItemAvailability> getItemAvailability() {
            return List.copyOf(itemAvailability);
        }

        public void setItemAvailability(final List<ItemAvailability> itemAvailability) {
            this.itemAvailability = immutableCopy(itemAvailability);
        }

        public List<ItemMedia> getItemMedia() {
            return List.copyOf(itemMedia);
        }

        public void setItemMedia(final List<ItemMedia> itemMedia) {
            this.itemMedia = immutableCopy(itemMedia);
        }

        private static <T> List<T> immutableCopy(final List<T> values) {
            return values == null ? List.of() : List.copyOf(values);
        }
    }
}
