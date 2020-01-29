package telegrambot.io;

import java.io.*;

class FileUtils {

    private FileUtils() {
        throw new AssertionError();
    }

    static <T> T tryLoadObject(String fileName, Class<T> clazz) {
        if (fileExists(fileName)) {
            try (var fis = new FileInputStream(fileName);
                 var ois = new ObjectInputStream(fis)) {
                return clazz.cast(ois.readObject());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    static void saveObject(String fileName, Object data) {
        try (var fos = new FileOutputStream(fileName);
             var oos = new ObjectOutputStream(fos)) {
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
