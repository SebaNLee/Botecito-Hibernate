package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.entity.ReportEnum;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportForm {

    @NotNull(message = "{report.validation.reason.required}")
    private ReportEnum reason;

    @Size(max = 255, message = "{report.validation.description.size}")
    private String description;

    public String getDescription() {
        if (description == null) {
            return null;
        }
        final String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
