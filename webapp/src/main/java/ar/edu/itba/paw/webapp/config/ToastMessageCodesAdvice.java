package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.webapp.util.ToastMessageCodes;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ToastMessageCodesAdvice {

    @ModelAttribute("allowedToastCodes")
    public Map<String, Boolean> allowedToastCodes() {
        return ToastMessageCodes.ALLOWED.stream().collect(Collectors.toMap(code -> code, code -> Boolean.TRUE));
    }
}
