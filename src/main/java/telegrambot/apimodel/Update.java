package telegrambot.apimodel;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Update {
    @EqualsAndHashCode.Include
    Long update_id;
    Message message;
}
