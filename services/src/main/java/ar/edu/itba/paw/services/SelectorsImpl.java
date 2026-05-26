package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.BookingStatusOptionModel;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import ar.edu.itba.paw.persistence.SelectorsDao;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public final class SelectorsImpl implements SelectorsService {

    private final SelectorsDao selectorsDao;
    private final MessageSource messageSource;

    @Override
    @Transactional(readOnly = true)
    public List<Location> getLocationOptions() {
        return selectorsDao.getLocationOptions();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemType> getItemTypeOptions() {
        return selectorsDao.getItemTypeOptions();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingStatusOptionModel> getBookingStatusOptions() {
        final Locale locale = LocaleContextHolder.getLocale();
        return Arrays.stream(BookingStatusEnum.values())
                .map(status -> {
                    final String code = "booking.status." + status.name();
                    final String label = messageSource.getMessage(code, null, status.name(), locale);
                    return new BookingStatusOptionModel(status.name(), label);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getDifficultyOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        // TODO hardcode, maybe create table in DB?
        options.put("1", "1 - Principiante");
        options.put("2", "2 - Basico");
        options.put("3", "3 - Intermedio");
        options.put("4", "4 - Avanzado");
        options.put("5", "5 - Experto");
        return options;
    }
}
