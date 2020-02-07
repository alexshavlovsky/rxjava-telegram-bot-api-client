# RxJava Telegram Bot API Client

Just another command line java Telegram Bot API client. Main features:
- built on top of Java 8 and RxJava2 Reactive Extensions
- includes several types of HTTP clients
- implemented HTTP methods: POST and GET
- implemented Telegram Bot API methods: "getMe", "getUpdates", "sendMessage"
- only text messages are supported
- API tokens and a message history are saved to the file system

## Build and run instructions

```
git clone https://github.com/alexshavlovsky/rxjava-telegram-bot-api-client.git
cd rxjava-telegram-bot-api-client
mvn package
cd target

Usage:
java -jar telebot.jar -t TELEGRAM_BOT_API_TOKEN
 -c <arg>   http client type
            <ahc> - Netty AsyncHttpClient
            <apache> - Netflix ApacheHttpClient
            <spring> - Spring ProjectReactor Netty WebClient
            (default - Netty AsyncHttpClient)
 -h         print this message
 -t <arg>   telegram Bot API token
            (default - most recently used token)
```

## Technology Stack

Component                      | Technology
---                            | ---
Java Reactive extensions       | RxJava v2.2
RxJava Http client             | Netflix HttpAsyncClient
Project Reactor Http client    | Spring ProjectReactor Netty WebClient
Netty Async HTTP client        | [AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client)
RxJava types Adapter           | [RxJava to RxJava2](https://github.com/akarnokd/RxJavaInterop)
Project Reactor types Adapter  | [Project Reactor to RxJava2](https://github.com/reactor/reactor-addons)
Command line interface         | Apache Commons CLI
