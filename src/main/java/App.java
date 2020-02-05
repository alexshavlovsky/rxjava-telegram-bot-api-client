import io.reactivex.Observable;
import telegrambot.BotException;
import telegrambot.TelegramBot;
import telegrambot.ui.KeyboardObservableFactory;

public class App {

    private static void print(String s) {
        System.out.println(s);
    }

    private static TelegramBot telegramBot;

    public static void main(String[] args) throws Exception {

        // parse command line arguments
        CliParser.CliOptions cliOptions = CliParser.parseArguments(args);

        // create a bot instance

        try {
            telegramBot = new TelegramBot(cliOptions.token, cliOptions.httpClientType);
        } catch (BotException e) {
            print("Unable to initialize: " + e.getMessage());
            CliParser.printHelpAndExit(1);
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
