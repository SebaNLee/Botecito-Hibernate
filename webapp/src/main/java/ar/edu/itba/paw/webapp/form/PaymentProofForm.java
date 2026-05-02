package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.FileSize;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class PaymentProofForm {

    @FileSize(max = 5242880, message = "{paymentProof.validation.file.size}")
    private MultipartFile file;

    @Size(max = 500, message = "{paymentProof.validation.guestReply.size}")
    private String guestReply;
}
