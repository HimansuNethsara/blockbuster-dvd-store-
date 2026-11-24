package lk.jiat.blockbuster.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class Dvd {

    private String dvdId;
    private double price;
    private String movieId;
    private int stockCount;
    private String statusId;

}
