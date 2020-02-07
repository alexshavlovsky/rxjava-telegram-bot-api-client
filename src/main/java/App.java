import io.reactivex.Observable;
import org.apache.commons.cli.ParseException;
import telegrambot.BotException;
import telegrambot.TelegramBot;
import telegrambot.ui.KeyboardObservableFactory;

public class App {

    private static void print(String s) {
        System.out.println(s);
    }

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

        // stream messages from API to console
        telegramBot.messageObservable().subscribe(App::print);

        // stream current chat events to console
        telegramBot.currentChatObservable().subscribe(s -> print("Current chat is set to: " + s));

        // create a system input scanner in a new thread
        Observable<String> keyboard = KeyboardObservableFactory.getInstance();

        //subscribe to user input
        keyboard.subscribe(
                telegramBot::sendMessage,
                e -> print("System input scanner error: " + e.toString()),
                () -> telegramBot.close()
        );
    }
}
