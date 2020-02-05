package telegrambot.apimodel;

import lombok.Data;

@Data
public class ApiResponse<T> {
    Boolean ok;
    Integer error_code;
    String description;
    T result;
}
