package telegrambot.io;

import java.util.Collections;
import java.util.Set;

import static telegrambot.io.FileUtils.saveObject;
import static telegrambot.io.FileUtils.tryLoadObject;

public class TokenStorageService {
    private static final String TOKEN_STORAGE_FILE = "tokens.data";
    private TokenStorageDTO tokenStorageDTO;

    public TokenStorageService() {
        tokenStorageDTO = tryLoadObject(TOKEN_STORAGE_FILE, TokenStorageDTO.class);
        if (tokenStorageDTO == null) {
            tokenStorageDTO = new TokenStorageDTO();
            save();
        }
    }

    private void save() {
        saveObject(TOKEN_STORAGE_FILE, tokenStorageDTO);
    }

    public void saveToken(String token) {
        if (!token.equals(tokenStorageDTO.mostRecentToken)) {
            tokenStorageDTO.knownTokens.add(token);
            tokenStorageDTO.mostRecentToken = token;
            save();
        }
    }

    public String getMostRecentToken() {
        return tokenStorageDTO.mostRecentToken;
    }

    public Set<String> getKnownTokens() {
        return Collections.unmodifiableSet(tokenStorageDTO.knownTokens);
    }
}
