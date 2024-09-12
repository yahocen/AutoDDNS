package org.addns.conf;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import org.addns.core.DDNSError;
import org.addns.util.FileUtil;
import org.addns.util.ObjUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 单例模式下的配置类。
 *
 * @author YahocenMiniPC
 */
public class Config {

    /**
     * 私有构造方法
     * @param configPath 配置文件地址
     */
    private Config(String configPath) {
        if (!FileUtil.exist(configPath)) {
            throw new DDNSError("读取配置文件错误：%s", configPath);
        }
        this.json = JsonIterator.deserialize(FileUtil.readUtf8String(configPath));
    }

    /**
     * 单例实例
     */
    private static volatile Config instance;

    /**
     * 存储配置的 JSON 数据
     */
    private final Any json;

    /**
     * 获取单例实例的方法。
     *
     * @param configPath 配置文件路径
     */
    public static void initInstance(String configPath) {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config(configPath);
                }
            }
        }
    }

    public static Any getAny(Object... keys) {
        Any any = instance.json.get(keys);
        if (ObjUtil.isNull(any) || any.toString().isBlank()) {
            throw new DDNSError("缺少必要设置：%s", Arrays.stream(keys).map(Object::toString).collect(Collectors.joining(".")));
        }
        return any;
    }

    public static String getStr(Object... keys) {
        return getAny(keys).toString();
    }

    public static Long getLong(Object... keys) {
        return getAny(keys).toLong();
    }

    public static <T> List<T> getList(Class<T> clazz, Object... keys) {
        return getAny(keys).asList().stream().map(i -> i.as(clazz)).collect(Collectors.toList());
    }

    public static List<Any> getList(Object... keys) {
        return getAny(keys).asList();
    }

    public static boolean hasKey(Object key) {
        return instance.json.keys().contains(key);
    }

}