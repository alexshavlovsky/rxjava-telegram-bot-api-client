package telegrambot.httpclient;

public class BotApiHttpClientFactory {
    public static BotApiHttpClient newInstance(BotApiHttpClientType botApiHttpClientType) {
        switch (botApiHttpClientType) {
            case SPRING_REACTOR_WEB_CLIENT:
                return new SpringWebClientEmbeddedMapper();
            case APACHE_HTTP_ASYNC_CLIENT:
                return new ApacheHttpAsyncClient();
            case NETTY_ASYNC_HTTP_CLIENT:
            default:
                return new NettyAsyncHttpClient();
        }
    }
}
