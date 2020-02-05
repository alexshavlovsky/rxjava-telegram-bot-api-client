package telegrambot.ui;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import java.util.Scanner;

public class KeyboardObservableFactory {

    private static class Holder {
        static final PublishSubject<String> INSTANCE = PublishSubject.create();
        static final Scanner scanner = new Scanner(System.in);
        static final KeyboardScannerWorker scannerWorker = new KeyboardScannerWorker(INSTANCE, scanner);
        static final Thread scannerThread = new Thread(scannerWorker, "sys_in_scanner");

        static {
            scannerThread.setDaemon(false);
            scannerThread.start();
        }
    }

    public KeyboardObservableFactory() {
        throw new AssertionError();
    }

    public static Observable<String> getInstance() {
        return Holder.INSTANCE;
    }
}
