package telegrambot.io;

import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.User;

import java.util.Collections;
import java.util.Set;

import static telegrambot.io.FileUtils.saveObject;
import static telegrambot.io.FileUtils.tryLoadObject;

public class BotService {
    private static final String BOT_FILE_EXTENSION = ".data";
    private String botFileName;
    private BotDTO botDTO;

    public BotService(String token) {
        int pos = token.indexOf(':');
        if (pos == -1) throw new RuntimeException("Malformed token");
        botFileName = token.substring(0, pos) + BOT_FILE_EXTENSION;
        botDTO = tryLoadObject(botFileName, BotDTO.class);
        if (botDTO == null) {
            botDTO = new BotDTO(token);
            saveObject(botFileName, botDTO);
        }
    }

    public void saveMessage(Message message) {
        botDTO.messages.add(message);
        botDTO.chats.add(message.getChat());
        botDTO.users.add(message.getFrom());
        saveObject(botFileName, botDTO);
    }

    public void saveUser(User user) {
        botDTO.users.add(user);
        saveObject(botFileName, botDTO);
    }

    public Set<Message> getMessages() {
        return Collections.unmodifiableSet(botDTO.messages);
    }

    public Set<User> getUsers() {
        return Collections.unmodifiableSet(botDTO.users);
    }

    public Set<Chat> getChats() {
        return Collections.unmodifiableSet(botDTO.chats);
    }

    public String getToken() {
        return botDTO.token;
    }
}
