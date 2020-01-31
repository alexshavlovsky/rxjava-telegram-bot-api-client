import telegrambot.BotException;
import telegrambot.TelegramBot;

import java.io.IOException;
import java.util.Scanner;

public class App {

    private static void print(String s) {
        System.out.println(s);
    }

    public static void main(String[] args) throws IOException {

        // parse command line arguments
        String token = CliParser.parseToken(args);

        // create a bot instance
        TelegramBot telegramBot = null;
        try {
            telegramBot = new TelegramBot(token);
        } catch (BotException e) {
            print("Unable to initialize: " + e.getMessage());
            System.exit(1);
        }

        // stream messages from API to console
        telegramBot.messageObservable().subscribe(App::print);

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
