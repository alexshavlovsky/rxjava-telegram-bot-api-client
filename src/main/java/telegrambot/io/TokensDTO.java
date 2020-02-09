package telegrambot.io;

import telegrambot.apimodel.User;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

class TokensDTO implements Serializable {
    Map<String, User> knownBotPrincipals;
    String mostRecentToken;

    TokensDTO() {
        knownBotPrincipals = new HashMap<>();
    }
}
