package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.FileSize;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class EditPublicationForm {

    @NotBlank(message = "{publish.validation.title.required}")
    @Size(max = 100, message = "{publish.validation.title.max}")
    private String title;

    @Size(max = 1000, message = "{publish.validation.description.max}")
    private String description;

    @NotBlank(message = "{publish.validation.price.required}")
    @Pattern(regexp = "\\d+", message = "{publish.validation.price.numeric}")
    private String pricePerHour;

    @Min(value = 1, message = "{publish.validation.difficulty.min}")
    @Max(value = 5, message = "{publish.validation.difficulty.max}")
    private Integer difficultyLevel;

    @NotBlank(message = "{publish.validation.location.required}")
    @Pattern(regexp = "\\d+", message = "{publish.validation.location.invalid}")
    private String marina;

    @FileSize(max = 5242880)
    private MultipartFile file;
}
