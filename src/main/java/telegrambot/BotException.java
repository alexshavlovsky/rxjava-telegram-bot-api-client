package telegrambot;

class BotException extends RuntimeException {
    private BotException(String message) {
        super(message);
    }

    static BotException NO_SAVED_TOKEN = new BotException(
            "Can't find any saved token\n" +
                    "Please provide an API token via command line argument\n" +
                    "You can get a token from BotFather"
    );

    static BotException TOKEN_VALIDATION_ERROR = new BotException(
            "Unable to validate token against Telegram API"
    );

    static BotException MALFORMED_TOKEN_PROVIDED = new BotException(
            "The provided token is malformed"
    );

}
