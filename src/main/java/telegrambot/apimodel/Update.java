package telegrambot.apimodel;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Data
public class Update {
    Long update_id;
    Message message;
}
