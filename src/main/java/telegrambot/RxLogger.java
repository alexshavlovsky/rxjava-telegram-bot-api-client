package telegrambot;

import io.reactivex.Observable;
import io.reactivex.subjects.ReplaySubject;
import org.slf4j.event.EventRecodingLogger;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.SubstituteLogger;

import java.util.LinkedList;

class RxLogger extends EventRecodingLogger {

    private static class RxQueue<T> extends LinkedList<T> {
        ReplaySubject<T> loggingEventSubject = ReplaySubject.create();

        @Override
        public boolean add(T o) {
            loggingEventSubject.onNext(o);
            return true;
        }
    }

    private final RxQueue<SubstituteLoggingEvent> eventQueue;

    private RxLogger(RxQueue<SubstituteLoggingEvent> eventQueue) {
        super(new SubstituteLogger("RxLogger", eventQueue, false), eventQueue);
        this.eventQueue = eventQueue;
    }

    static RxLogger newInstance() {
        RxQueue<SubstituteLoggingEvent> queue = new RxQueue<>();
        return new RxLogger(queue);
    }

    Observable<SubstituteLoggingEvent> loggingEventObservable() {
        return eventQueue.loggingEventSubject;
    }

}
