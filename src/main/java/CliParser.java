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
                .addOption("c", true, "http client type\n<ahc> - AsyncHttpClient\n<apache> - Apache HttpAsyncClient\n<spring> - Spring Project Reactor WebClient\n(default - AsyncHttpClient)");
    }

    static class CliOptions {
        String token; // null = try to load a token from the file system
        HttpClientType httpClientType = HttpClientType.ASYNC_HTTP_CLIENT; // set a default client
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
        if ("spring".equals(clientKey)) cliOptions.httpClientType = HttpClientType.SPRING_REACTOR_WEB_CLIENT;
        if ("apache".equals(clientKey)) cliOptions.httpClientType = HttpClientType.APACHE_HTTP_ASYNC_CLIENT;
        if ("ahc".equals(clientKey)) cliOptions.httpClientType = HttpClientType.ASYNC_HTTP_CLIENT;
        return cliOptions;
    }

}
