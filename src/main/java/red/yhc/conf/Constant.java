package red.yhc.conf;

/**
 * @author YahocenMiniPC
 */
public class Constant {

    //软件名称
    public static final String NAME = "AutoDDNS";

    public static final String DOMAIN_KEY = "domain";

    public static final String DNS_KEY = "dns";

    public static final String MODE_KEY = "mode";

    //ipv4模式
    public static final String MODE_V4 = "v4";

    //ipv6模式
    public static final String MODE_V6 = "v6";

    //默认配置文件位置
    public static final String CONFIG_PATH = "./conf/config.json";

    public static final String V4_PATH = "./data/ipv4.txt";

    public static final String V6_PATH = "./data/ipv6.txt";

    //日志储存大小字节数 3MB
    public static final int LOG_LIMIT = 3145728;

    //日志储存数量
    public static final int LOG_COUNT = 10;

}
