import org.apache.commons.cli.*;

class CliParser {

    private CliParser() {
        throw new AssertionError();
    }

    private static Options options = createOptions();

    private static Options createOptions() {
        Options res = new Options();
        res.addOption("h", false, "print this message");
        res.addOption("t", true, "telegram Bot API token to use");
        return res;
    }

    static String parseToken(String[] args) {
        CommandLineParser parser = new DefaultParser();
        String token = null;
        try {
            CommandLine line = parser.parse(CliParser.options, args);
            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("telebot", CliParser.options);
                System.exit(0);
            }
            token = line.getOptionValue("t");
        } catch (ParseException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
        }
        return token;
    }

}
