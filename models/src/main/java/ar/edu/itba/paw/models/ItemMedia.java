package ar.edu.itba.paw.models;

public class ItemMedia {
    private Integer id;
    private Integer itemId;
    private byte[] imageData;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(final Integer itemId) {
        this.itemId = itemId;
    }

    public byte[] getImageData() {
        return imageData == null ? null : imageData.clone();
    }

    public void setImageData(final byte[] imageData) {
        this.imageData = imageData == null ? null : imageData.clone();
    }
}
