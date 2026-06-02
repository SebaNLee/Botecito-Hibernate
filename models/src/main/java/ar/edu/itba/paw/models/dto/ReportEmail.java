package ar.edu.itba.paw.models.dto;

import ar.edu.itba.paw.models.entity.ReportEnum;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ReportEmail {
    private final String email;
    private final String displayName;
    private final Locale locale;
    private final String itemTitle;
    private final String description;
    private final ReportEnum reason;
}
