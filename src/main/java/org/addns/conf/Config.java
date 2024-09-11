package org.addns.conf;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import org.addns.util.FileUtil;
import org.addns.util.LogUtil;
import org.addns.util.ObjUtil;
import org.addns.util.StrUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author YahocenMiniPC
 */
public class Config {

    private final Any json;

    public Config(String configPath) {
        this.json = JsonIterator.deserialize(FileUtil.readUtf8String(configPath));
        System.out.println(this.json.get("TrafficRouteDns", "mode"));
    }

    public Any getAny(Object ...keys) {
        Any any = json.get(keys);
        if(ObjUtil.isNull(any) || any.toString().isBlank()) {
            throw new RuntimeException(String.format("缺少必要设置：%s", Arrays.stream(keys).map(Object::toString).collect(Collectors.joining("."))));
        }
        return any;
    }

    public String getStr(Object ...keys) {
        return getAny(keys).toString();
    }

    public Long getLong(Object ...keys) {
        return getAny(keys).toLong();
    }

    public <T> List<T> getList(Class<T> clazz, Object ...keys) {
        return getAny(keys).asList().stream().map(i -> i.as(clazz)).collect(Collectors.toList());
    }

    public List<Any> getList(Object ...keys) {
        return getAny(keys).asList();
    }

    public boolean hasKey(Object key) {
        return json.keys().contains(key);
    }

}
