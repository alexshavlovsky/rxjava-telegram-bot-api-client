import org.apache.commons.cli.Options;

class CliOptions {

    private CliOptions() {
        throw new AssertionError();
    }

    static Options options = createOptions();

    private static Options createOptions() {
        Options res = new Options();
        res.addOption("t", true, "telegram Bot API token to use");
        res.addOption("h", false, "print this message");
        return res;
    }

}
