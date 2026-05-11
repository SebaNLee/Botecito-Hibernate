package ar.edu.itba.paw.webapp.form.nuevo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {

    private String error;
    private String logout;
    private String registered;
    private String legacyToken;
    private String passwordRecovered;
    private String next;
}
