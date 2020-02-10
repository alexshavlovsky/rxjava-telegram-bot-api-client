import io.reactivex.Observable;
import org.apache.commons.cli.ParseException;
import telegrambot.BotException;
import telegrambot.TelegramBot;
import telegrambot.ui.KeyboardObservableFactory;

public class App {

    private static TelegramBot telegramBot;

    public static void main(String[] args) {

        // parse command line arguments
        CliParser.CliOptions cliOptions = null;
        try {
            cliOptions = CliParser.parseArguments(args);
        } catch (ParseException e) {
            CliParser.printHelpAndExit(1, "Unable parse program arguments: " + e.getMessage());
        }

        // create a bot instance
        try {
            telegramBot = new TelegramBot(cliOptions.token, cliOptions.botApiHttpClientType);
        } catch (BotException e) {
            CliParser.printHelpAndExit(1, "Unable to initialize: " + e.getMessage());
        }

        // create a system input scanner in a new thread (not demon)
        // this observable will be completed when the scanner reads line ':q'
        Observable<String> keyboard = KeyboardObservableFactory.getInstance();

        // subscribe to messages from API
        // the subscription wil be completed and an underlying http client will be closed when the keyboard observable completes
        telegramBot.messageObservable(keyboard).subscribe(
                System.out::println,
                e -> System.out.println(e.toString()),
                () -> telegramBot.close()
        );

    }
}
