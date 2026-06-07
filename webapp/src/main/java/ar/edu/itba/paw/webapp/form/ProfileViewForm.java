package ar.edu.itba.paw.webapp.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileViewForm {

    private String tab;
    private Integer listingsPage;
    private Integer listingsPageSize;
    private Integer reviewsPage;
    private Integer reviewsPageSize;
}
