package telegrambot;

import org.joda.time.DateTime;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.User;
import telegrambot.io.BotService;

import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

class MessageFormatter {

    private static int maxNameLength = -1;

    private static int updateAndGetMaxNameWidth(String name, BotService botService) {
        if (maxNameLength == -1) maxNameLength = getMaxNameWidth(botService);
        return Math.max(maxNameLength, name.length());
    }

    private static int getMaxNameWidth(BotService botService) {
        return maxNameLength = botService.getUsers().stream()
                .map(MessageFormatter::formatName)
                .map(String::length)
                .max(Integer::compare).orElse(0);
    }

    private static String appendSpace(String s, int n) {
        StringBuilder builder = new StringBuilder(s);
        while (builder.length() < n) builder.append(" ");
        return builder.toString();
    }

    private static String joinNotNullNotBlank(String... entries) {
        return Arrays.stream(entries).filter(e -> e != null && !e.isBlank()).collect(Collectors.joining(" "));
    }

    static String formatName(User user) {
        return joinNotNullNotBlank(user.getFirst_name(), user.getLast_name(), user.getUsername());
    }

    private static String formatChat(Chat chat) {
        return joinNotNullNotBlank(chat.getFirst_name(), chat.getLast_name(), chat.getUsername(), chat.getTitle());
    }

    private static String formatDirection(Message message, BotService botService) {
        return botService.getToken().contains(message.getFrom().getId().toString()) ? ">" : "<";
    }

    private static String formatTime(Date date) {
        return new DateTime(date).toString("HH:mm:ss");
    }

    static String format(Message message, BotService botService) {
        String name = formatName(message.getFrom());
        int nameWidth = updateAndGetMaxNameWidth(name, botService);
        String dir = formatDirection(message, botService);
        String time = formatTime(message.getDate());
        return joinNotNullNotBlank(time, appendSpace(name, nameWidth), dir, message.getText());
    }

}
