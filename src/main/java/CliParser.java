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
                .addOption("t", true, "telegram Bot API token to use\n(default - most recently used token)")
                .addOption("c", true, "http client to use\n<apache> - Apache HttpAsyncClient (default)\n<spring> - Spring Project Reactor WebClient");
    }

    static class CliOptions {
        String token = "";
        HttpClientType httpClientType = HttpClientType.APACHE_HTTP_ASYNC_CLIENT;
    }

    static void printHelpAndExit(int status) {
        HelpFormatter formatter = new HelpFormatter();
        String syntax = "java -jar telebot.jar -t TELEGRAM_BOT_API_TOKEN";
        formatter.printHelp(syntax, CliParser.OPTIONS);
        System.exit(status);
    }

    static CliOptions parseArguments(String[] args) {
        CommandLineParser parser = new DefaultParser();
        CliOptions cliOptions = new CliOptions();
        try {

            CommandLine line = parser.parse(CliParser.OPTIONS, args);

            if (line.hasOption("h")) printHelpAndExit(0);

            cliOptions.token = line.getOptionValue("t");

            String clientKey = line.getOptionValue("c");
            if ("spring".equals(clientKey)) cliOptions.httpClientType = HttpClientType.SPRING_WEB_CLIENT;

        } catch (ParseException e) {
            System.out.println("Unexpected exception while parsing program arguments: " + e.getMessage());
        }
        return cliOptions;
    }

}
