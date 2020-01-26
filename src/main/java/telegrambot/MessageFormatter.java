package telegrambot;

import org.joda.time.DateTime;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.User;
import telegrambot.io.BotService;

import java.util.ArrayList;
import java.util.List;

class MessageFormatter {

    private static int maxNameLength = -1;

    private static int updateAndGetMaxNameLength(String name, BotService botService) {
        if (maxNameLength == -1)
            maxNameLength = botService.getUsers().stream()
                    .map(MessageFormatter::formatName)
                    .map(String::length)
                    .max(Integer::compare).orElseGet(() -> 0);
        if (name.length() > maxNameLength) maxNameLength = name.length();
        return maxNameLength;
    }

    private static void addNotBlank(List<String> list, String entry) {
        if (entry != null && !entry.isEmpty() && !entry.isBlank()) list.add(entry);
    }

    private static String appendSpace(String s, int n) {
        StringBuilder builder = new StringBuilder(s);
        while (builder.length() < n) builder.append(" ");
        return builder.toString();
    }

    static String formatName(User user) {
        List<String> entries = new ArrayList<>();
        addNotBlank(entries, user.getFirst_name());
        addNotBlank(entries, user.getLast_name());
        return String.join(" ", entries);
    }

    static String format(Message message, BotService botService) {
        String name = formatName(message.getFrom());
        name = appendSpace(name, updateAndGetMaxNameLength(name, botService));
        String dir = botService.getToken().contains(message.getFrom().getId().toString()) ? ">" : "<";
        String time = new DateTime(message.getDate()).toString("HH:mm:ss");
        return time + " " + name + " " + dir + " " + message.getText();
    }

}
