package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.webapp.util.UploadLimits;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("maxUploadFileBytes")
    public long maxUploadFileBytes() {
        return UploadLimits.MAX_FILE_BYTES;
    }
}
