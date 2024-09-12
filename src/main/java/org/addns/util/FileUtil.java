package org.addns.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author YahocenMiniPC
 */
public class FileUtil {

    public static String readUtf8String(String file) {
        return readUtf8String(new File(file));
    }

    public static String readUtf8String(File file) {
        return readUtf8String(file.toPath());
    }

    public static String readUtf8String(Path file) {
        if(Files.notExists(file)) {
            return null;
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LogUtil.error(String.format("读取文件 %s 错误", file), e);
            return null;
        }
    }

    public static boolean mkdir(String file) {
        if(ObjUtil.isNull(file)) {
            return false;
        }
        return mkdir(new File(file));
    }

    public static boolean mkdir(File file) {
        if(ObjUtil.isNull(file)) {
            return false;
        }
        return file.mkdir();
    }

    public static boolean exist(String file) {
        if(ObjUtil.isNull(file)) {
            return false;
        }
        return exist(new File(file));
    }

    /**
     * 判断文件是否存在，如果file为null，则返回false
     *
     * @param file 文件
     * @return 如果存在返回true
     */
    public static boolean exist(File file) {
        return (null != file) && file.exists();
    }

    public static boolean exist(Path file) {
        return (null != file) && Files.exists(file);
    }

    public static String writeUtf8String(String text, File file) {
        writeUtf8String(text, file.toPath());
        return text;
    }

    public static void writeUtf8String(String text, Path file) {
        if(!exist(file)) {
            try {
                if(!exist(file.getParent())) {
                    Files.createDirectory(file.getParent());
                }
                Files.createFile(file);
            } catch (IOException e) {
                LogUtil.error(String.format("创建写入文件 %s 错误：%s", file, text), e);
                return;
            }
        }
        try {
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LogUtil.error(String.format("写入文件 %s 错误：%s", file, text), e);
        }
    }

}
