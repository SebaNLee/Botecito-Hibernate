package ar.edu.itba.paw.models.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SelfBlockCreate {
    private final String startTime;
    private final String endTime;
}
