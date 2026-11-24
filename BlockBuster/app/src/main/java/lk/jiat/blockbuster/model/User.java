package lk.jiat.blockbuster.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String uid;
    private String firstName;
    private String lastName;
    private String email;
    private String profilePicUrl;

    private String registeredDate;


    public String getFullName() {
        return firstName + " " + lastName;
    }
}
