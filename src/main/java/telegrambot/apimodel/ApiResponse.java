package telegrambot.apimodel;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ApiResponse<T> {
    Boolean ok;
    Integer error_code;
    String description;
    T result;

    public String getErrorDescription() {
        return String.format("Telegram API responded with an error code %d: %s", error_code, description);
    }
}
