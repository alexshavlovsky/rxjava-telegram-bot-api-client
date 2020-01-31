package telegrambot.io;

import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.User;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static telegrambot.io.FileUtils.saveObject;
import static telegrambot.io.FileUtils.tryLoadObject;

public class BotService {
    private static final String BOT_FILE_EXTENSION = ".data";
    private String fileName;
    private BotDTO botDTO;

    public BotService(String token) {
        fileName = BotService.getFileName(token);
        botDTO = tryLoadObject(fileName, BotDTO.class);
        if (botDTO == null) {
            botDTO = new BotDTO(token);
            save();
        }
    }

    private static String getFileName(String token) {
        int pos = token.indexOf(':');
        if (pos == -1) throw new RuntimeException("Provided token is malformed");
        return token.substring(0, pos) + BOT_FILE_EXTENSION;
    }

    private void save() {
        saveObject(fileName, botDTO);
    }

    public void saveMessage(Message message) {
        botDTO.messages.add(message);
        botDTO.chats.add(message.getChat());
        botDTO.users.add(message.getFrom());
        save();
    }

    public void saveAllMessages(Collection<? extends Message> messages) {
        botDTO.messages.addAll(messages);
        messages.forEach(m -> botDTO.chats.add(m.getChat()));
        messages.forEach(m -> botDTO.users.add(m.getFrom()));
        save();
    }

    public void saveUser(User user) {
        if (!botDTO.users.contains(user)) {
            botDTO.users.add(user);
            save();
        }
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
}
