import org.apache.commons.cli.*;
import telegrambot.io.HttpClientType;

class CliParser {

    private CliParser() {
        throw new AssertionError();
    }

    private static Options options = createOptions();

    private static Options createOptions() {
        Options res = new Options();
        res
                .addOption("h", false, "print this message")
                .addOption("t", true, "telegram Bot API token to use\n(default - most recently used token)")
                .addOption("c", true, "http client to use\n<apache> - Apache HttpAsyncClient (default)\n<spring> - Spring Project Reactor WebClient");
        return res;
    }

    static class CliOptions {
        String token = "";
        HttpClientType httpClientType = HttpClientType.APACHE_HTTP_ASYNC_CLIENT;
    }

    static CliOptions parseArguments(String[] args) {
        CommandLineParser parser = new DefaultParser();
        CliOptions cliOptions = new CliOptions();
        String token = null;
        try {
            CommandLine line = parser.parse(CliParser.options, args);
            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar telebot.jar -t TELEGRAM_BOT_API_TOKEN", CliParser.options);
                System.exit(0);
            }
            cliOptions.token = line.getOptionValue("t");
            String clientId = line.getOptionValue("c");
            if ("spring".equals(clientId)) cliOptions.httpClientType = HttpClientType.SPRING_WEB_CLIENT;
            System.out.println(cliOptions.httpClientType);
        } catch (ParseException e) {
            System.out.println("Unexpected exception while parsing program arguments: " + e.getMessage());
        }
        return cliOptions;
    }

}
