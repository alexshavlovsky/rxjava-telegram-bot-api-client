package telegrambot.httpclient;

import telegrambot.apimodel.ApiResponse;

class ApiException extends RuntimeException {
    ApiException(ApiResponse response) {
        super(String.format("Telegram API returned an error %d: %s", response.getError_code(), response.getDescription()));
    }
}
