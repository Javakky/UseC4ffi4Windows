import jnr.ffi.LibraryLoader;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.io.FilenameUtils.*;

/**
 * コマンドでもdllをシステムプロパティに追加しつつこれを使うとバグるとおもいます
 */
public class LoadDll {

    /**
     * システムプロパティに追加したdllを保存しておく(重複回避)
     */
    private static final Set<String> setDll = new HashSet<>();

    /**
     * dllをシステムプロパティに追加する <br>
     * jar化することも考慮するため、dllを一端外のtmpファイルに保存してから追加している。<br>
     * リソースフォルダにdllは入れてね☆
     * @param filepath リソースフォルダより下のパス(/resource/a/test.dllならa/test.dll)
     * @return 追加したtmpファイルの絶対パス
     * @throws IOException tmpファイルの作成失敗
     */
    public static String setDll(String filepath) throws IOException {
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

    /**
     * dllファイルからInterfaceの実装オブジェクトをロードする。<br>
     * 内部で {@link LoadDll#setDll(String)}を呼ぶので、他でシステムプロパティに追加するのはやめてね☆
     * @param filename dllファイルのリソースフォルダより下のパス(/resource/a/test.dllならa/test.dll)
     * @param clazz 生成したいInterfaceのクラス(jextract等で生成したやつ)
     * @param <T> 生成したいInterface
     * @return 生成したオブジェクト
     */

    public static <T> T loadDll(String filename, Class<T> clazz) {

        String tmpFileName = null;
        try {
            tmpFileName = setDll(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return LibraryLoader.create(clazz).load(tmpFileName);
    }
}
