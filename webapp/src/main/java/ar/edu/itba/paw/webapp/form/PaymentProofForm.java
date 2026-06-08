package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.FileSize;
import java.io.IOException;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class PaymentProofForm {

    @FileSize(max = 5242880, message = "{paymentProof.validation.file.size}")
    private MultipartFile file;

    @Size(max = 255, message = "{paymentProof.validation.guestReply.size}")
    private String guestReply;

    public String getTrimmedGuestReply() {
        return StringUtils.hasText(guestReply) ? guestReply.trim() : null;
    }

    public String getFileName() {
        return file != null && StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "proof";
    }

    public String getContentType() {
        return file != null && StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    public byte[] getFileBytes() throws IOException {
        return file != null ? file.getBytes() : null;
    }
}
