import org.apache.commons.cli.*;
import telegrambot.httpclient.HttpClientType;

class CliParser {

    private CliParser() {
        throw new AssertionError();
    }

    private static Options OPTIONS = new Options();

    static {
        OPTIONS
                .addOption("h", false, "print this message")
                .addOption("t", true, "telegram Bot API token\n(default - most recently used token)")
                .addOption("c", true, String.format("http client type\n%s\n(default - %s)",
                        HttpClientType.joinToString(),
                        HttpClientType.defaultClient.getDescription()));
    }

    static class CliOptions {
        String token; // null = try to load a token from the file system
        HttpClientType httpClientType = HttpClientType.defaultClient;
    }

    static void printHelpAndExit(int status, String... headerLines) {
        HelpFormatter formatter = new HelpFormatter();
        String syntax = "java -jar telebot.jar -t TELEGRAM_BOT_API_TOKEN";
        for (String line : headerLines) System.out.println(line);
        formatter.printHelp(syntax, CliParser.OPTIONS);
        System.exit(status);
    }

    static CliOptions parseArguments(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CliOptions cliOptions = new CliOptions();
        CommandLine line = parser.parse(CliParser.OPTIONS, args);
        if (line.hasOption("h")) printHelpAndExit(0);
        cliOptions.token = line.getOptionValue("t");
        String clientKey = line.getOptionValue("c");
        if (clientKey != null) {
            HttpClientType requestedClientType = HttpClientType.getByKey(clientKey);
            if (requestedClientType == null) throw new UnrecognizedArgumentException(clientKey, "c");
            else cliOptions.httpClientType = requestedClientType;
        }
        return cliOptions;
    }

    private static class UnrecognizedArgumentException extends ParseException {
        UnrecognizedArgumentException(String argument, String option) {
            super(String.format("Unrecognized argument '%s' for option '%s'", argument, option));
        }
    }
}
