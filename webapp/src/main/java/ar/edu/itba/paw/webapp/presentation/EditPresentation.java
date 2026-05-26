package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.services.EditService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.SelectorsService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Component
@RequiredArgsConstructor
public class EditPresentation {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final EditService editService;
    private final ItemService itemService;
    private final SelectorsService selectorsInterface;

    public ModelAndView bootstrapEdit(
            final BotecitoUserDetails principal, final int itemId, final HttpServletRequest request) {

        final Version version = itemService.requireOwnedFullData(itemId, principal.getId());
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        final Map<String, Object> draft = buildDraftPayload(version, itemId, contextPath);

        final ModelAndView mav = new ModelAndView("edit-bootstrap");
        mav.addObject("itemId", itemId);
        mav.addObject("detailsUrl", "/edit/" + itemId + "/details");
        try {
            mav.addObject("draftJson", OBJECT_MAPPER.writeValueAsString(draft));
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize edit draft", e);
        }
        return mav;
    }

    public ModelAndView editStepOne(final int itemId) {
        final ModelAndView mav = new ModelAndView("edit-details");
        mav.addObject("itemId", itemId);
        return mav;
    }

    public ModelAndView editStepOneSubmit(final int itemId, final PublishBoatForm form, final BindingResult errors) {
        if (errors.hasErrors()) {
            final ModelAndView mav = new ModelAndView("edit-details");
            mav.addObject("itemId", itemId);
            return mav;
        }
        return new ModelAndView("redirect:/edit/" + itemId + "/availability");
    }

    public ModelAndView editStepTwo(final int itemId) {
        final ModelAndView mav = new ModelAndView("edit-availability");
        mav.addObject("itemId", itemId);
        PublishWizardMapping.addAvailabilityEditorData(mav, new PublishBoatForm());
        return mav;
    }

    public ModelAndView editStepTwoSubmit(final int itemId, final PublishBoatForm form, final BindingResult errors) {
        if (errors.hasErrors()) {
            if (PublishWizardMapping.hasErrorsOutsideAvailability(errors)) {
                return redirectToEditDetails(itemId);
            }
            final ModelAndView mav = new ModelAndView("edit-availability");
            mav.addObject("itemId", itemId);
            PublishWizardMapping.addAvailabilityEditorData(mav, form);
            return mav;
        }
        return new ModelAndView("redirect:/edit/" + itemId + "/images");
    }

    public ModelAndView editStepThree(final int itemId) {
        final ModelAndView mav = new ModelAndView("edit-images");
        mav.addObject("itemId", itemId);
        mav.addObject("galleryPreviewUrls", List.of());
        return mav;
    }

    public ModelAndView editStepThreeSubmit(
            final BotecitoUserDetails principal,
            final int itemId,
            final PublishBoatForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {

        if (errors.hasErrors()) {
            if (PublishWizardMapping.hasErrorsOutsideAvailability(errors)) {
                return redirectToEditDetails(itemId);
            }
            return editImagesView(itemId);
        }

        final boolean updated = editService.edit(
                itemId,
                principal.getId(),
                form.getItemTypeId(),
                form.getTitle().trim(),
                form.getDescription() == null ? "" : form.getDescription().trim(),
                form.getPricePerHour(),
                form.getCapacity(),
                form.getWeight(),
                form.getDifficulty(),
                form.getLocationOptionId(),
                PublishWizardMapping.toAvailabilityWindows(form),
                PublishWizardMapping.toEditImageUploads(form));
        if (updated) {
            ToastSupport.success(redirectAttributes, "settings.publications.updated");
        } else {
            ToastSupport.info(redirectAttributes, "settings.publications.noChanges");
        }

        return new ModelAndView("redirect:/my-boats");
    }

    public Map<String, String> buildDifficultyOptions() {
        return selectorsInterface.getDifficultyOptions();
    }

    public int maxGalleryImages() {
        return PublishBoatForm.MAX_GALLERY_IMAGES;
    }

    private static ModelAndView editImagesView(final int itemId) {
        final ModelAndView mav = new ModelAndView("edit-images");
        mav.addObject("itemId", itemId);
        mav.addObject("galleryPreviewUrls", List.of());
        return mav;
    }

    private static ModelAndView redirectToEditDetails(final int itemId) {
        final RedirectView redirectView = new RedirectView("/edit/" + itemId + "/details", true);
        redirectView.setExposeModelAttributes(false);
        return new ModelAndView(redirectView);
    }

    private static Map<String, Object> buildDraftPayload(
            final Version version, final int itemId, final String contextPath) {
        final Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("v", 1);
        draft.put("itemId", itemId);
        draft.put("versionId", version.getId());
        draft.put("title", version.getTitle());
        draft.put("description", version.getDescription() == null ? "" : version.getDescription());
        draft.put("itemTypeId", version.getType().getId());
        draft.put("pricePerHour", version.getPrice().intValue());
        draft.put("capacity", version.getCapacity());
        draft.put("weight", version.getWeight());
        draft.put("difficulty", version.getDifficulty());
        draft.put("locationOptionId", version.getLocation().getId());
        draft.put("availability", buildAvailabilityDraft(version));
        draft.put("images", buildImageDraft(version, contextPath));
        return draft;
    }

    private static Map<String, Object> buildAvailabilityDraft(final Version version) {
        final List<String> ranges = new ArrayList<>();
        final LinkedHashSet<String> enabledDays = new LinkedHashSet<>();
        if (version.getAvailabilities() != null) {
            for (final Availability availability : version.getAvailabilities()) {
                if (availability.getWeekday() == null
                        || availability.getStartTime() == null
                        || availability.getEndTime() == null) {
                    continue;
                }
                final String weekday = availability.getWeekday().name();
                enabledDays.add(weekday);
                ranges.add(weekday + "|" + availability.getStartTime() + "|" + availability.getEndTime());
            }
        }
        final Map<String, Object> availability = new LinkedHashMap<>();
        availability.put("enabledDays", new ArrayList<>(enabledDays));
        availability.put("ranges", ranges);
        return availability;
    }

    private static List<Map<String, Object>> buildImageDraft(final Version version, final String contextPath) {
        if (version.getMedia() == null || version.getMedia().isEmpty()) {
            return List.of();
        }
        final List<Map<String, Object>> images = new ArrayList<>();
        version.getMedia().stream()
                .sorted(Comparator.comparingInt(m -> m.getId().getIndex()))
                .forEach(media -> {
                    if (media.getImage() == null || media.getImage().getId() == null) {
                        return;
                    }
                    final Map<String, Object> image = new LinkedHashMap<>();
                    image.put("id", media.getImage().getId());
                    image.put("url", contextPath + "/image/" + media.getImage().getId());
                    images.add(image);
                });
        return images;
    }
}
