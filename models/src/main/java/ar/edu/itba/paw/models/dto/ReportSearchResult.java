package ar.edu.itba.paw.models.dto;

import ar.edu.itba.paw.models.entity.Report;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class ReportSearchResult {
    private final List<Report> reports;
    private final long totalCount;
}
