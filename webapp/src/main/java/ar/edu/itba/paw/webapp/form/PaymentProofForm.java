package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.FileSize;
import org.springframework.web.multipart.MultipartFile;

public class PaymentProofForm {

    @FileSize(max = 5242880, message = "{paymentProof.validation.file.size}")
    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(final MultipartFile file) {
        this.file = file;
    }
}
