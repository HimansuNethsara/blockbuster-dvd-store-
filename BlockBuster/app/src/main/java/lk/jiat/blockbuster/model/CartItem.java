package lk.jiat.blockbuster.model;

import com.google.firebase.firestore.Exclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class CartItem {

    @Getter(onMethod_ = {@Exclude})
    @Setter(onMethod_ = {@Exclude})
    private String documentId;
    private String dvdId;
    private int quantity;

}
