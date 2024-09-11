package red.yhc.conf;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import red.yhc.util.FileUtil;

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
        return json.get(keys);
    }

    public String getStr(Object ...keys) {
        return getAny(keys).toString();
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
