package ar.edu.itba.paw.services;

public interface ManageItemService {

    void setEnabled(int itemId, int ownerId, boolean enabled);

    void deleteItem(int itemId, int ownerId);

    void deleteItemAsAdmin(int itemId);
}
