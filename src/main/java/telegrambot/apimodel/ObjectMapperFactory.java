package telegrambot.apimodel;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

class ObjectMapperFactory {

    private static class Holder {
        static final ObjectMapper INSTANCE;

        static {
            INSTANCE = new ObjectMapper();
            INSTANCE.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

    }

    private ObjectMapperFactory() {
        throw new AssertionError();
    }


    static ObjectMapper getInstance() {
        return Holder.INSTANCE;
    }
}
