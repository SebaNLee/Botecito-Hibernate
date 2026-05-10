package ar.edu.itba.paw.models.nuevo;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Getter
@Setter
public class ItemDetail {
    private List<ItemModelVersion> versions;

    /**
     * Immutable pair for JSP EL ({@code ver.versionId}, {@code ver.itemModel}); Java record accessors are not
     * JavaBean-style, so JSP EL cannot resolve {@code ver.versionId} on a record.
     */
    @Value
    public static class ItemModelVersion {
        ItemModel itemModel;
        long versionId;
    }
}
