package org.addns.conf;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author YahocenMiniPC
 */
public class Command {

    /**
     * 单例实例
     */
    private static volatile Command instance;

    private final List<String> params;

    private Command(String[] args) {
        this.params = Arrays.stream(args).map(String::trim).toList();
    }

    /**
     * 获取单例实例的方法。
     * @param args 程序入口参数
     */
    public static void initInstance(String[] args) {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Command(args);
                }
            }
        }
    }

    public static String getStr(String param) {
        if(!contains(param)) {
            return null;
        }
        return instance.params.get(instance.params.indexOf(param) + 1);
    }

    public static boolean contains(String param) {
        return instance.params.contains(param);
    }

}
