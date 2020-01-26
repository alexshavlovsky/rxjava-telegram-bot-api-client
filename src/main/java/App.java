import org.apache.commons.cli.*;
import telegrambot.TelegramBot;
import telegrambot.apimodel.Chat;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;


public class App {

    private static void print(String s) {
        System.out.println(s);
    }

    public static void main(String[] args) throws IOException {

        // parse command line arguments
        CommandLineParser parser = new DefaultParser();
        String token = null;
        try {
            CommandLine line = parser.parse(CliOptions.options, args);
            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("telebot", CliOptions.options);
                System.exit(0);
            }
            token = line.getOptionValue("t");
        } catch (ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
        }

        // create a bot instance
        print("Open session...");
        TelegramBot telegramBot;
        if (token == null) {
            // try to restore a bot state from the file system
            telegramBot = new TelegramBot();
            print(String.format("Use token: %s", telegramBot.getCurrentToken()));
        } else {
            print(String.format("Use token: %s", token));
            telegramBot = new TelegramBot(token);
        }
        print(String.format("Bot name: %s", telegramBot.getBotName()));

        // stream messages from API to console
        telegramBot.messageHistoryObservable().concatWith(telegramBot.messageUpdatesObservable()).subscribe(App::print);

        // stream current chat events to console
        AtomicReference<Chat> chat = new AtomicReference<>();
        telegramBot.currentChatObservable().subscribe(ch -> {
            chat.set(ch);
            print(String.format("Current chat is set to: %s (%d)", ch.getFirst_name(), ch.getId()));
        });

        // main loop
        print("To exit type: ':q'");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (chat.get() == null) print("A current chat is not assigned. Please send a message to this bot first!");
            String line = scanner.nextLine();
            if (":q".equals(line)) break;
            if (chat.get() != null && line != null && !line.isBlank())
                telegramBot.sendMessage(chat.get().getId(), line).subscribe(App::print);
        }

        print("Close session...");
        telegramBot.close();
    }
}
