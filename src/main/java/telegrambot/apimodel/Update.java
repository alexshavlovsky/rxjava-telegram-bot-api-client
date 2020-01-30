package telegrambot.apimodel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Update {
    Long update_id;
    Message message;
}
