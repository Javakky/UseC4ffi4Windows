import jnr.ffi.LibraryLoader;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.io.FilenameUtils.*;

public class LoadDll {

    private static final Set<String> setDll = new HashSet<>();

    public static String setDll(String filepath) throws NoSuchFieldException, IllegalAccessException, IOException {
        filepath = System.getProperty("user.dir") + "\\" + filepath;
        if (setDll.contains(filepath)) return null;
        String filename = getName(filepath);
        String prefix = "";
        String suffix = null;

        if (filename != null) {
            prefix = filename;
        }

        if (prefix.length() < 3) {
            throw new IllegalArgumentException("ファイル名は3文字以上");
        }

        File temp = File.createTempFile(prefix, suffix);
        temp.deleteOnExit();

        if (!temp.exists()) {
            throw new FileNotFoundException("File " + temp.getAbsolutePath() + " does not exist.");
        }

        byte[] buffer = new byte[1024];
        int readBytes;

        try (InputStream is = LoadDll.class.getResourceAsStream(filename); OutputStream os = new FileOutputStream(temp)) {
            if (is == null) {
                throw new FileNotFoundException("File " + filename + " was not found inside JAR.");
            }
            while ((readBytes = is.read(buffer)) != -1) {
                os.write(buffer, 0, readBytes);
            }
        } catch (IOException e) {
            throw e;
        }

        System.load(temp.getAbsolutePath());

        setDll.add(filename);

        return temp.getName();
    }

    public static <T> T loadDll(String filename, Class<T> clazz) {

        String tmpFileName = null;
        try {
            tmpFileName = setDll(filename);
        } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
            e.printStackTrace();
        }

        return LibraryLoader.create(clazz).load(tmpFileName);
    }
}
