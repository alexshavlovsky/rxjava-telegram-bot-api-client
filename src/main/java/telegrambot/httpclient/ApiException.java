package telegrambot.httpclient;

import telegrambot.apimodel.ApiResponse;

import java.rmi.ServerException;

class ApiException extends ServerException {
    ApiException(ApiResponse response) {
        super(String.format("Telegram API returned an error %d: %s", response.getError_code(), response.getDescription()));
    }
}
