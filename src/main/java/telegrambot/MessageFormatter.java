package telegrambot;

import ch.qos.logback.classic.spi.LoggingEvent;
import org.joda.time.DateTime;
import telegrambot.apimodel.Chat;
import telegrambot.apimodel.Message;
import telegrambot.apimodel.User;
import telegrambot.io.BotRepository;

import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

class MessageFormatter {

    private static int maxNameLength = -1;

    private static int updateAndGetMaxNameWidth(String name, BotRepository botRepository) {
        if (maxNameLength == -1) maxNameLength = getMaxNameWidth(botRepository);
        return Math.max(maxNameLength, name.length());
    }

    private static int getMaxNameWidth(BotRepository botRepository) {
        return maxNameLength = botRepository.getUsers().stream()
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
        return Arrays.stream(entries).filter(e -> e != null && !e.trim().isEmpty()).collect(Collectors.joining(" "));
    }

    static String formatName(User user) {
        return joinNotNullNotBlank(user.getFirst_name(), user.getLast_name(), user.getUsername());
    }

    static String formatChat(Chat chat) {
        return joinNotNullNotBlank(chat.getFirst_name(), chat.getLast_name(),
                chat.getUsername(), chat.getTitle(), "(" + chat.getId() + ")");
    }

    private static String formatDirection(User messageUser, User botUser) {
        return botUser.equals(messageUser) ? ">" : "<";
    }

    private static String formatTime(Date date) {
        return new DateTime(date).toString("HH:mm:ss");
    }

    private static String messageToText(Message message) {
        return message.getText() != null ?
                message.getText() :
                message.getCaption() != null ?
                        message.getCaption() :
                        "NOT IMPLEMENTED: the message can't be represented as text";
    }

    static String formatMessage(Message message, BotRepository botRepository, User botUser) {
        String name = formatName(message.getFrom());
        int nameWidth = updateAndGetMaxNameWidth(name, botRepository);
        String dir = formatDirection(message.getFrom(), botUser);
        String time = formatTime(message.getDate());
        return joinNotNullNotBlank(time, appendSpace(name, nameWidth), dir, messageToText(message));
    }

    private static String formatParametrizedLogMessage(String f, Object... args) {
        return String.format(f.replace("{}", "%s"), args);
    }

    private static String formatLogMessage(LoggingEvent e) {
        Object[] args = e.getArgumentArray();
        if (args == null || args.length == 0) return e.getMessage();
        return formatParametrizedLogMessage(e.getMessage(), args);
    }

    static String formatLoggingEvent(LoggingEvent e) {
        String time = formatTime(new Date(e.getTimeStamp()));
        String message = formatLogMessage(e);
        return joinNotNullNotBlank(time, appendSpace(e.getLevel().toString(), 5), message);
    }
}
