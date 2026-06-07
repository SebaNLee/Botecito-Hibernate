package ar.edu.itba.paw.webapp.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingsViewForm {

    private Integer subscriptionsPage;
    private Integer subscriptionsPageSize;
    private Boolean edit;
}
