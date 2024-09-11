package org.addns;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import org.addns.conf.Constant;
import org.addns.util.LogUtil;
import org.addns.conf.Config;
import org.addns.dns.DnsOper;
import org.addns.dns.TrafficRouteDnsOper;
import org.addns.util.FileUtil;
import org.addns.util.HttpUtil;
import org.addns.util.StrUtil;

import java.io.File;
import java.util.*;

/**
 * @author YahocenMiniPC
 */
public class App {

    private static final Map<String, DnsOper> DDNS_OPER = new HashMap<>();

    /**
     * Program entry point
     * @param args Parameters
     */
    public static void main(String[] args) {
        //初始化配置信息
        String confPath = args.length==0 ? Constant.CONFIG_PATH : args[0];
        if(!FileUtil.exist(confPath)) {
            LogUtil.error("请检查配置文件：%s", confPath);
            return;
        }
        Config config = new Config(confPath);
        //检查IPv4变化
        File v4File = new File(Constant.V4_PATH);
        String newIpv4 = getIpv4(config);
        boolean v4Change = !StrUtil.equals(newIpv4, v4File.exists() ? FileUtil.readUtf8String(v4File) :null);
        //检查IPv6变化
        File v6File = new File(Constant.V6_PATH);
        String newIpv6 = getIpv6(config);
        boolean v6Change = !StrUtil.equals(newIpv6, FileUtil.exist(v6File) ?FileUtil.readUtf8String(v6File) :null);
        //没有变化，结束进程
        if(!v4Change && !v6Change) {
            LogUtil.info("IP未发生变化：[%s, %s]", newIpv4, newIpv6);
            return;
        }
        //初始化DNS API操作类
        initDnsApi(config);
        //更新DNS解析
        config.getList("domains").parallelStream().forEach(domain -> {
            try {
                DnsOper dnsOper = DDNS_OPER.get(domain.get(Constant.DNS_KEY).toString());
                if(Constant.MODE_V4.equals(domain.get(Constant.MODE_KEY).toString())) {
                    dnsOper.editDnsV4(domain.get(Constant.DOMAIN_KEY).toString(), newIpv4);
                }else{
                    dnsOper.editDnsV6(domain.get(Constant.DOMAIN_KEY).toString(), newIpv6);
                }
            }catch (Exception e){
                LogUtil.error("更新域名解析失败：%s", e, domain.toString());
            }
        });
        //保存新IP
        FileUtil.writeUtf8String(newIpv4, v4File);
        FileUtil.writeUtf8String(newIpv6, v6File);
        LogUtil.info("IP已更新为：[%s, %s]", newIpv4, newIpv6);
    }

    /**
     * 初始化 DNS API
     * @param config 配置
     */
    private static void initDnsApi(Config config) {
        for (Any dnsConf : config.getList(Constant.DNS_KEY)) {
            DDNS_OPER.put(dnsConf.get("name").toString(), switch (dnsConf.get("type").toString()) {
                case "trddns" -> new TrafficRouteDnsOper(dnsConf);
                default -> throw new RuntimeException("暂不支持此 DNS API 类型" + dnsConf.get("name"));
            });
        }
    }

    private static String getIpv4(Config config) {
        List<Any> v4api = config.getList("v4api");
        for (Any api : v4api) {
            try {
                String result = HttpUtil.get(api.get("url").toString());
                if(api.keys().contains("field") && !api.get("field").toString().isBlank()) {
                    return JsonIterator.deserialize(result).get(api.get("field").toString()).toString();
                }
                return result;
            }catch (Exception e){
                LogUtil.error("获取IPv4地址错误: %s", e, api.get("url").toString());
            }
        }
        return null;
    }

    private static String getIpv6(Config config) {
        List<Any> v6api = config.getList("v6api");
        for (Any api : v6api) {
            try {
                String result = HttpUtil.get(api.get("url").toString());
                if(api.keys().contains("field") && !api.get("field").toString().isBlank()) {
                    return JsonIterator.deserialize(result).get(api.get("field").toString()).toString();
                }
                return result;
            }catch (Exception e){
                LogUtil.error("获取IPv6地址错误: %s", e, api.get("url").toString());
            }
        }
        return null;
    }

}
