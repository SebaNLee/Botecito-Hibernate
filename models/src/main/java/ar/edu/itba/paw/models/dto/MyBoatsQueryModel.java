package ar.edu.itba.paw.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class MyBoatsQueryModel {
    private int ownerId;
    private String searchQuery;
    private String status;
    private String locationSlug;
    private int page;
    private int pageSize;
    private String sortBy;
}
