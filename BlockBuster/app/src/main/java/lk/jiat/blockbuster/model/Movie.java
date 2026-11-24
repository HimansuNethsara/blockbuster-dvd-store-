package lk.jiat.blockbuster.model;

import com.google.firebase.firestore.Exclude;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class Movie {

    private String movieId;
    private String title;
    private String description;
    private List<String> images;
    private float imdbRating;
    private float rtRating;
    private String trailerUrl;
    private int releaseYear;
    private int runtime;
    private String mpaaRatingId;
    private String statusId;

    private List<String> genreIds;
    private List<String> actorIds;
    private List<String> directorIds;
    private List<String> plotKeywordIds;

    @Exclude
    private String downloadUri;

}
