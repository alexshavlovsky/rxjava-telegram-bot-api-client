package telegrambot.pollingclient;

import telegrambot.apimodel.Update;

class UpdateOffsetHolder {
    private static final long NOT_SET = -1;
    private long value = NOT_SET;

    void refresh(Update[] updates) {
        int updatesNum = updates.length;
        value = updatesNum > 0 ? updates[updatesNum - 1].getUpdate_id() : NOT_SET;
    }

    long getNext() {
        return value + 1;
    }

    boolean isSet() {
        return value != NOT_SET;
    }
}
