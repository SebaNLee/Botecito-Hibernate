package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.presentation.PublishPresentation;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

@Controller
@SessionAttributes("publishForm")
@RequiredArgsConstructor
public class PublishController {

    private final PublishPresentation publishPresentation;

    @ModelAttribute("publishForm")
    public PublishBoatForm publishForm() {
        return new PublishBoatForm();
    }

    @ModelAttribute("itemTypeOptions")
    public Map<String, String> itemTypeOptions() {
        return PublishPresentation.buildItemTypeOptions();
    }

    @ModelAttribute("capacityOptions")
    public Map<String, String> capacityOptions() {
        return PublishPresentation.buildCapacityOptions();
    }

    @ModelAttribute("difficultyOptions")
    public Map<String, String> difficultyOptions() {
        return PublishPresentation.buildDifficultyOptions();
    }

    @ModelAttribute("uploadedImagePreviewUrls")
    public List<String> uploadedImagePreviewUrls(@ModelAttribute("publishForm") final PublishBoatForm form) {
        return PublishPresentation.buildUploadedImagePreviewUrls(form);
    }

    @ModelAttribute("maxGalleryImages")
    public int maxGalleryImages() {
        return PublishPresentation.MAX_GALLERY_IMAGES;
    }

    @RequestMapping(value = "/publish", method = RequestMethod.GET)
    public ModelAndView publishStepOne(@ModelAttribute("publishForm") final PublishBoatForm form) {
        return publishPresentation.publishStepOne(form);
    }

    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    public ModelAndView publishStepOneSubmit(
            @Validated(PublishBoatForm.Step1.class) @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors) {
        return publishPresentation.publishStepOneSubmit(form, errors);
    }

    @RequestMapping(value = "/publish/images/upload", method = RequestMethod.POST)
    public ModelAndView publishImagesUpload(
            @ModelAttribute("publishForm") final PublishBoatForm form, final BindingResult errors) {
        return publishPresentation.publishImagesUpload(form, errors);
    }

    @RequestMapping(value = "/publish/images/remove", method = RequestMethod.POST)
    public ModelAndView publishImagesRemove(
            @ModelAttribute("publishForm") final PublishBoatForm form, @RequestParam("index") final int index) {
        return publishPresentation.publishImagesRemove(form, index);
    }

    @RequestMapping(value = "/publish/images/reorder", method = RequestMethod.POST)
    public ModelAndView publishImagesReorder(
            @ModelAttribute("publishForm") final PublishBoatForm form,
            @RequestParam(value = "order", required = false) final String order) {
        return publishPresentation.publishImagesReorder(form, order);
    }

    @RequestMapping(value = "/publish/availability", method = RequestMethod.GET)
    public ModelAndView publishStepTwo(@ModelAttribute("publishForm") final PublishBoatForm form) {
        return publishPresentation.publishStepTwo(form);
    }

    @RequestMapping(value = "/publish/availability", method = RequestMethod.POST)
    public ModelAndView publishStepTwoSubmit(
            @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors,
            final Locale locale,
            @RequestParam(value = "enabledDays", required = false) final List<String> enabledDays,
            @RequestParam(value = "availabilityRanges", required = false) final List<String> availabilityRanges) {
        return publishPresentation.publishStepTwoSubmit(form, errors, locale, enabledDays, availabilityRanges);
    }

    @RequestMapping(value = "/publish/contact", method = RequestMethod.GET)
    public ModelAndView publishStepThree(
            @ModelAttribute("publishForm") final PublishBoatForm form, final Locale locale) {
        return publishPresentation.publishStepThree(form, locale);
    }

    @RequestMapping(value = "/publish/preview-image/{index}", method = RequestMethod.GET)
    public ResponseEntity<byte[]> publishPreviewImage(
            @ModelAttribute("publishForm") final PublishBoatForm form, @PathVariable("index") final int index) {
        return publishPresentation.publishPreviewImage(form, index);
    }

    @RequestMapping(value = "/publish/contact", method = RequestMethod.POST)
    public ModelAndView publishStepThreeSubmit(
            @Validated(PublishBoatForm.Step3.class) @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors,
            final Locale locale,
            final SessionStatus sessionStatus) {
        return publishPresentation.publishStepThreeSubmit(form, errors, locale, sessionStatus);
    }

    @RequestMapping(value = "/publish/success", method = RequestMethod.GET)
    public ModelAndView publishSuccess(
            final HttpServletRequest request, @RequestParam(value = "itemId", required = false) final Integer itemId) {
        return publishPresentation.publishSuccess(request, itemId);
    }
}
