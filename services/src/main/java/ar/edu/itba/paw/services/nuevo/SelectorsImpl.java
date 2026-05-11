package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.BookingStatusOptionModel;
import ar.edu.itba.paw.models.nuevo.ItemTypeModel;
import ar.edu.itba.paw.models.nuevo.Location;
import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import ar.edu.itba.paw.persistence.nuevo.SelectorsDao;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public final class SelectorsImpl implements SelectorsInterface {

    private final SelectorsDao selectorsDao;
    private final MessageSource messageSource;

    @Override
    public List<Location> getLocationOptions() {
        return selectorsDao.getLocationOptions();
    }

    @Override
    public List<ItemTypeModel> getItemTypeOptions() {
        return selectorsDao.getItemTypeOptions();
    }

    @Override
    public List<BookingStatusOptionModel> getBookingStatusOptions() {
        final Locale locale = LocaleContextHolder.getLocale();
        return Arrays.stream(BookingStatus.values())
                .map(status -> {
                    final String code = "booking.status." + status.name();
                    final String label = messageSource.getMessage(code, null, status.name(), locale);
                    return new BookingStatusOptionModel(status.name(), label);
                })
                .toList();
    }

    @Override
    public Map<String, String> getDifficultyOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("1", "1 - Principiante");
        options.put("2", "2 - Basico");
        options.put("3", "3 - Intermedio");
        options.put("4", "4 - Avanzado");
        options.put("5", "5 - Experto");
        return options;
    }
}
