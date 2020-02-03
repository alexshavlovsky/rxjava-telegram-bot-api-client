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
        return String.format("%s (%s)", description, error_code);
    }
}
