package telegrambot.io;

import telegrambot.apimodel.User;

import static telegrambot.io.FileUtils.saveObject;
import static telegrambot.io.FileUtils.tryLoadObject;

public class TokensRepository {
    private static final String TOKEN_STORAGE_FILE = "tokens.data";
    private TokensDTO tokensDTO;

    public TokensRepository() {
        tokensDTO = tryLoadObject(TOKEN_STORAGE_FILE, TokensDTO.class);
        if (tokensDTO == null) {
            tokensDTO = new TokensDTO();
            save();
        }
    }

    private void save() {
        saveObject(TOKEN_STORAGE_FILE, tokensDTO);
    }

    public void saveToken(String token, User user) {
        if (!token.equals(tokensDTO.mostRecentToken)) {
            tokensDTO.knownBotPrincipals.put(token, user);
            tokensDTO.mostRecentToken = token;
            save();
        }
    }

    public boolean containsToken(String token) {
        return tokensDTO.knownBotPrincipals.containsKey(token);
    }

    public User getUserForToken(String token) {
        return tokensDTO.knownBotPrincipals.get(token);
    }

    public String getMostRecentToken() {
        return tokensDTO.mostRecentToken;
    }
}
