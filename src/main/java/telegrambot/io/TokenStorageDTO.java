package telegrambot.io;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

class TokenStorageDTO implements Serializable {
    Set<String> knownTokens;
    String mostRecentToken;

    TokenStorageDTO() {
        knownTokens = new HashSet<>();
    }
}
