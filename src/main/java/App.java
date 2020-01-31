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

        print("Current bot: " + telegramBot.getBotName());
        print("Commands:\n:q - exit");
        Scanner scanner = new Scanner(System.in);

        // main loop
        while (true) {
//            if (chat.get() == null) print("A current chat is not assigned. Please send a message to this bot first!");
            String line = scanner.nextLine();
            if (":q".equals(line)) break;
            if (!line.isBlank()) telegramBot.sendMessage(line);
        }

        print("Close session...");
        telegramBot.close();
    }
}
