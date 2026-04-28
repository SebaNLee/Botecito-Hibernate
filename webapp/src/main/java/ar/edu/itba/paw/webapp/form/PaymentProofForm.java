package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.FileSize;
import javax.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class PaymentProofForm {

    @FileSize(max = 5242880, message = "{paymentProof.validation.file.size}")
    private MultipartFile file;

    @Size(max = 500, message = "{paymentProof.validation.guestReply.size}")
    private String guestReply;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(final MultipartFile file) {
        this.file = file;
    }

    public String getGuestReply() {
        return guestReply;
    }

    public void setGuestReply(final String guestReply) {
        this.guestReply = guestReply;
    }
}
