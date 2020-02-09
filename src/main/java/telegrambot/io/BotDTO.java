package telegrambot.io;

import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.User;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

class BotDTO implements Serializable {
    Set<User> users;
    Set<Chat> chats;
    Set<Message> messages;

    BotDTO() {
        users = new HashSet<>();
        chats = new HashSet<>();
        messages = new HashSet<>();
    }
}
