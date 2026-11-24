package lk.jiat.blockbuster.model;

import com.google.firebase.firestore.Exclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Genre {
    private String genreId;
    private String name;
    private String imageUrl;

    @Exclude
    private String downloadUri;

}

