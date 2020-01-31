package telegrambot;

public class BotException extends Exception {
    private BotException(String message) {
        super(message);
    }

    private BotException(String message, Throwable cause) {
        super(message, cause);
    }

    static BotException NO_SAVED_TOKEN = new BotException(
            "Can't find any saved token.\n" +
                    "Please provide an API token via command line argument.\n" +
                    "You can get a token from BotFather."
    );

    static BotException TOKEN_VALIDATION_ERROR = new BotException(
            "Unable to validate token against Telegram API"
    );
}
