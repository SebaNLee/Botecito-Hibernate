package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.controller.support.PublishMvcSupport;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = PublishController.class)
@Order(0)
public class PublishControllerAdvice {

    @ModelAttribute("publishForm")
    public PublishBoatForm publishForm() {
        return new PublishBoatForm();
    }

    @ModelAttribute("itemTypeOptions")
    public Map<String, String> itemTypeOptions() {
        return PublishMvcSupport.buildItemTypeOptions();
    }

    @ModelAttribute("capacityOptions")
    public Map<String, String> capacityOptions() {
        return PublishMvcSupport.buildCapacityOptions();
    }

    @ModelAttribute("difficultyOptions")
    public Map<String, String> difficultyOptions() {
        return PublishMvcSupport.buildDifficultyOptions();
    }

    @ModelAttribute("uploadedImagePreviewUrls")
    public List<String> uploadedImagePreviewUrls(@ModelAttribute("publishForm") final PublishBoatForm form) {
        return PublishMvcSupport.buildUploadedImagePreviewUrls(form);
    }

    @ModelAttribute("maxGalleryImages")
    public int maxGalleryImages() {
        return PublishMvcSupport.MAX_GALLERY_IMAGES;
    }
}
