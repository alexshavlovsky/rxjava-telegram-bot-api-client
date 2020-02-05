package telegrambot.ui;

import io.reactivex.subjects.PublishSubject;

import java.util.Scanner;

class KeyboardScannerWorker implements Runnable {
    private final PublishSubject<String> publishSubject;
    private final Scanner scanner;

    KeyboardScannerWorker(PublishSubject<String> publishSubject, Scanner scanner) {
        this.publishSubject = publishSubject;
        this.scanner = scanner;
    }

    @Override
    public void run() {
        System.out.println("You've entered the interactive mode (to quit type :q)");
        while (true) {
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) continue;
            if (":q".equals(line)) break;
            publishSubject.onNext(line);
        }
        System.out.println("Quit the interactive mode");
        publishSubject.onComplete();
    }
}
