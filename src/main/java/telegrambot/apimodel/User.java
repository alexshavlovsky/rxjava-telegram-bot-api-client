package telegrambot.apimodel;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
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
