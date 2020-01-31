package telegrambot.io;

import java.io.*;

class FileUtils {

    private FileUtils() {
        throw new AssertionError();
    }

    static <T> T tryLoadObject(String fileName, Class<T> clazz) {
        if (fileExists(fileName)) {
            try (FileInputStream fis = new FileInputStream(fileName);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                return clazz.cast(ois.readObject());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    static void saveObject(String fileName, Object data) {
        try (FileOutputStream fos = new FileOutputStream(fileName);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean fileExists(String fileName) {
        File file = new File(fileName);
        return file.exists() && file.isFile();
    }

}
