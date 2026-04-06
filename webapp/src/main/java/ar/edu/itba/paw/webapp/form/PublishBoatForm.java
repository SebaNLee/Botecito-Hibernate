package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.FileSize;
import org.springframework.web.multipart.MultipartFile;

public class PublishBoatForm {

    private String title;
    private String description;
    private String marina;
    private String capacity;
    private String maxWeight;

    @FileSize(max = 5242880)
    private MultipartFile file;

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getMarina() {
        return marina;
    }

    public void setMarina(final String marina) {
        this.marina = marina;
    }

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(final String capacity) {
        this.capacity = capacity;
    }

    public String getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(final String maxWeight) {
        this.maxWeight = maxWeight;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(final MultipartFile file) {
        this.file = file;
    }
}
