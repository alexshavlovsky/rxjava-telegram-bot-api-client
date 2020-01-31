package telegrambot.apimodel;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import rx.Observable;

@Getter
@Setter
@ToString
public class ApiResponse<T> {
    Boolean ok;
    Integer error_code;
    String description;
    T result;

    public static <T> Observable<ApiResponse<T>> fromByteArrayAsObservable(byte[] response, Class<T> clazz) {
        ObjectMapper objectMapper = ObjectMapperFactory.getInstance();
        JavaType type = objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, clazz);
        try {
            return Observable.just(objectMapper.readValue(response, type));
        } catch (Exception e) {
            return Observable.error(e);
        }
    }

    public String getErrorDescription() {
        return String.format("%s (%s)", description, error_code);
    }
}
