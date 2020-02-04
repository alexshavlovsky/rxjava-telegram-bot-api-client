import telegrambot.BotException;
import telegrambot.TelegramBot;

import java.util.Scanner;

public class App {

    private static void print(String s) {
        System.out.println(s);
    }

    public static void main(String[] args) throws Exception {

        // parse command line arguments
        CliParser.CliOptions cliOptions = CliParser.parseArguments(args);

        // create a bot instance
        TelegramBot telegramBot = null;
        try {
            telegramBot = new TelegramBot(cliOptions.token, cliOptions.httpClientType);
        } catch (BotException e) {
            print("Unable to initialize: " + e.getMessage());
            System.exit(1);
        }

        // stream messages from API to console
        telegramBot.messageObservable().subscribe(App::print,e->print("error"+e.toString()),()->print("complete"));

        // stream current chat events to console
        telegramBot.currentChatObservable().subscribe(s -> print("Current chat is set to: " + s));

        print("Commands:\n\t:q - exit");

        // main loop
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) continue;
            if (":q".equals(line)) break;
            telegramBot.sendMessage(line);
        }

        print("Close session...");
        telegramBot.close();
    }
}
