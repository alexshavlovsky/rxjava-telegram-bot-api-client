package telegrambot.apimodel;

import lombok.*;

import java.io.Serializable;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User implements Serializable {
    @EqualsAndHashCode.Include
    Long id;
    String is_bot;
    String first_name;
    String last_name;
    String username;
    String language_code;
}
