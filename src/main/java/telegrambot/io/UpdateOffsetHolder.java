package telegrambot.io;

import telegrambot.apimodel.Update;

public class UpdateOffsetHolder {
    private static final long NOT_SET = -1;
    private long value = NOT_SET;

    public void refresh(Update[] updates) {
        int updatesNum = updates.length;
        value = updatesNum > 0 ? updates[updatesNum - 1].getUpdate_id() : NOT_SET;
    }

    public long getNext() {
        return value + 1;
    }

    public boolean isSet() {
        return value != NOT_SET;
    }
}
