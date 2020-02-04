# RxJava Telegram Bot API Client

Just another command line java Telegram Bot API client. Main features:
- built on top of Java 8 and RxJava2 Reactive Extensions
- includes two versions of HTTP client: Apache HTTPAsyncClient based and Spring Project Reactor WebClient based
- implemented HTTP methods: POST and GET
- implemented Telegram Bot API methods: "getMe", "getUpdates", "sendMessage"
- only text messages are supported
- API tokens and a message history are saved to file system

## Build and run instructions

```
git clone https://github.com/alexshavlovsky/rxjava-telegram-bot-api-client.git
cd rxjava-telegram-bot-api-client
mvn package
cd target

Usage:
java -jar telebot.jar -t TELEGRAM_BOT_API_TOKEN
 -c <arg>   http client to use
            <apache> - Apache HttpAsyncClient (default)
            <spring> - Spring Project Reactor WebClient
 -h         print this message
 -t <arg>   telegram Bot API token to use
            (default - most recently used token)
```

## Technology Stack

Component                     | Technology
---                           | ---
RxJava Http clients           | Apache Netflix HttpAsyncClient
Project Reactor Http client   | Spring Project Reactor WebClient
RxJava types Adapter          | [RxJava to RxJava2](https://github.com/akarnokd/RxJavaInterop)
Project Reactor types Adapter | [Project Reactor to RxJava2](https://github.com/reactor/reactor-addons)
Command line interface        | Apache Commons CLI
