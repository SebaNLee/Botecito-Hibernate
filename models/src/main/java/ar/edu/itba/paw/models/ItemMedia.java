package ar.edu.itba.paw.models;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ItemMedia {
    private Integer id;
    private Integer itemId;
    private byte[] imageData;
}
