package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.services.PublishService;
import ar.edu.itba.paw.services.SelectorsService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Component
@RequiredArgsConstructor
public class PublishPresentation {

    private final PublishService publishService;
    private final SelectorsService selectorsInterface;

    public ModelAndView publishStepOne() {
        return new ModelAndView("publish");
    }

    public ModelAndView publishStepOneSubmit(final PublishBoatForm form, final BindingResult errors) {
        if (errors.hasErrors()) {
            return new ModelAndView("publish");
        }
        return new ModelAndView("redirect:/publish/availability");
    }

    public ModelAndView publishStepTwo() {
        final ModelAndView mav = new ModelAndView("publish-availability");
        addAvailabilityEditorData(mav, new PublishBoatForm());
        return mav;
    }

    public ModelAndView publishStepTwoSubmit(final PublishBoatForm form, final BindingResult errors) {
        if (errors.hasErrors()) {
            if (hasErrorsOutsideAvailability(errors)) {
                return redirectToPublish();
            }
            final ModelAndView mav = new ModelAndView("publish-availability");
            addAvailabilityEditorData(mav, form);
            return mav;
        }

        return new ModelAndView("redirect:/publish/images");
    }

    public ModelAndView publishStepThree() {
        final ModelAndView mav = new ModelAndView("publish-images");
        mav.addObject("galleryPreviewUrls", List.of());
        return mav;
    }

    public ModelAndView publishStepThreeSubmit(
            final BotecitoUserDetails principal,
            final PublishBoatForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }

        if (errors.hasErrors()) {
            if (hasErrorsOutsideAvailability(errors)) {
                return redirectToPublish();
            }
            return publishImagesView();
        }

        publishService.create(
                principal.getId(),
                form.getItemTypeId(),
                form.getTitle().trim(),
                form.getDescription() == null ? "" : form.getDescription().trim(),
                form.getPricePerHour(),
                form.getCapacity(),
                form.getWeight(),
                form.getDifficulty(),
                form.getLocationOptionId(),
                toAvailabilityWindows(form),
                toImageUploads(form));

        ToastSupport.success(redirectAttributes, "profile.publications.created");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public Map<String, String> buildDifficultyOptions() {
        return selectorsInterface.getDifficultyOptions();
    }

    public int maxGalleryImages() {
        return PublishBoatForm.MAX_GALLERY_IMAGES;
    }

    private static ModelAndView publishImagesView() {
        final ModelAndView mav = new ModelAndView("publish-images");
        mav.addObject("galleryPreviewUrls", List.of());
        return mav;
    }

    private static ModelAndView redirectToPublish() {
        final RedirectView redirectView = new RedirectView("/publish", true);
        redirectView.setExposeModelAttributes(false);
        return new ModelAndView(redirectView);
    }

    private static boolean hasErrorsOutsideAvailability(final BindingResult errors) {
        return errors.getFieldErrors().stream().anyMatch(error -> !isAvailabilityError(error.getField()));
    }

    private static boolean isAvailabilityError(final String field) {
        if (field == null) {
            return false;
        }
        return field.startsWith("availability") || "availabilityByWeekday".equals(field);
    }

    private static void addAvailabilityEditorData(final ModelAndView mav, final PublishBoatForm form) {
        mav.addObject("existingSlotsJson", toExistingSlotsJson(form));
        mav.addObject("enabledWeekdays", buildEnabledWeekdaysModel(form));
    }

    private static Map<String, Boolean> buildEnabledWeekdaysModel(final PublishBoatForm form) {
        final Map<String, Boolean> model = new LinkedHashMap<>();
        for (final DayOfWeek weekday : DayOfWeek.values()) {
            model.put(weekday.name(), form.getEnabledDays().contains(weekday));
        }
        return model;
    }

    private static List<AvailabilityWindow> toAvailabilityWindows(final PublishBoatForm form) {
        final List<AvailabilityWindow> availabilities = new ArrayList<>();
        for (final PublishBoatForm.AvailabilityRangeBinding range : form.getAvailabilityRanges()) {
            if (range == null
                    || range.getWeekday() == null
                    || range.getStartTime() == null
                    || range.getEndTime() == null) {
                continue;
            }
            final AvailabilityWindow window = new AvailabilityWindow();
            window.setWeekday(range.getWeekday());
            window.setStartTime(range.getStartTime());
            window.setEndTime(range.getEndTime());
            availabilities.add(window);
        }
        return availabilities;
    }

    private static List<ImageUpload> toImageUploads(final PublishBoatForm form) {
        final List<MultipartFile> uploaded = form.getFiles();
        if (uploaded == null || uploaded.isEmpty()) {
            return List.of();
        }
        final List<ImageUpload> uploads = new ArrayList<>();
        for (final MultipartFile file : uploaded) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                final byte[] bytes = file.getBytes();
                if (bytes.length > 0) {
                    uploads.add(new ImageUpload(bytes, file.getContentType()));
                }
            } catch (final IOException ignored) {
                // skip unreadable file
            }
        }
        return uploads;
    }

    private static String toExistingSlotsJson(final PublishBoatForm form) {
        if (form.getAvailabilityRanges() == null || form.getAvailabilityRanges().isEmpty()) {
            return "[]";
        }
        final StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (final PublishBoatForm.AvailabilityRangeBinding range : form.getAvailabilityRanges()) {
            if (range == null
                    || range.getWeekday() == null
                    || range.getStartTime() == null
                    || range.getEndTime() == null) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            sb.append("{\"weekday\":\"")
                    .append(range.getWeekday().name())
                    .append("\",\"startTime\":\"")
                    .append(range.getStartTime())
                    .append("\",\"endTime\":\"")
                    .append(range.getEndTime())
                    .append("\"}");
            first = false;
        }
        return sb.append("]").toString();
    }
}
