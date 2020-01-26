package telegrambot.apimodel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Message implements Serializable {
    @EqualsAndHashCode.Include
    Long message_id;
    User from;
    @JsonDeserialize(using = UnixTimestampDeserializer.class)
    Date date;
    Chat chat;
    String text;
}
