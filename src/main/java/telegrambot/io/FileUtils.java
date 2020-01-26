package telegrambot.io;

import java.io.*;

class FileUtils {

    private FileUtils() {
        throw new AssertionError();
    }

    static Object tryLoadObject(String fileName) {
        if (fileExists(fileName)) {
            try (var fis = new FileInputStream(fileName);
                 var ois = new ObjectInputStream(fis)) {
                return ois.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    static <T> void saveObject(String fileName, T data) {
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
