package org.addns;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import org.addns.conf.Config;
import org.addns.conf.Constant;
import org.addns.util.LogUtil;
import org.addns.dns.DnsOper;
import org.addns.dns.TrafficRouteDnsOper;
import org.addns.util.FileUtil;
import org.addns.util.HttpUtil;
import org.addns.util.StrUtil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author YahocenMiniPC
 */
public class App implements Runnable {

    private static final Map<String, DnsOper> DDNS_OPER = new HashMap<>();

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private static Config CONFIG;

    public static void main(String[] args) {
        //初始化配置信息
        String confPath = args.length==0 ? Constant.CONFIG_PATH : args[0];
        if(!FileUtil.exist(confPath)) {
            LogUtil.error("请检查配置文件：%s", confPath);
            return;
        }
        CONFIG = new Config(confPath);
        //初始化 DNS API
        for (Any dnsConf : CONFIG.getList(Constant.DNS_KEY)) {
            DDNS_OPER.put(dnsConf.get("name").toString(), switch (dnsConf.get("type").toString()) {
                case "trddns" -> new TrafficRouteDnsOper(dnsConf);
                default -> throw new RuntimeException("暂不支持此 DNS API 类型" + dnsConf.get("name"));
            });
        }
        // 安排任务在 5 秒后开始执行，并且每隔 2 秒执行一次
        EXECUTOR.scheduleAtFixedRate(new App(), 0, CONFIG.getLong("period"), TimeUnit.SECONDS);
        // 当不再需要定时任务时，关闭执行器
        Runtime.getRuntime().addShutdownHook(new Thread(EXECUTOR::shutdown));
    }

    @Override
    public void run() {
        //检查IPv4变化
        String newIpv4 = getIpv4();
        boolean v4Change = !StrUtil.equals(newIpv4, Constant.V4_FILE.exists() ? FileUtil.readUtf8String(Constant.V4_FILE) :null);
        //检查IPv6变化
        String newIpv6 = getIpv6();
        boolean v6Change = !StrUtil.equals(newIpv6, FileUtil.exist(Constant.V6_FILE) ?FileUtil.readUtf8String(Constant.V6_FILE) :null);
        //没有变化，结束进程
        if(!v4Change && !v6Change) {
            LogUtil.info("IP未发生变化：[%s, %s]", newIpv4, newIpv6);
            return;
        }
        //更新DNS解析
        CONFIG.getList("domains").parallelStream().forEach(domain -> {
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
        FileUtil.writeUtf8String(newIpv4, Constant.V4_FILE);
        FileUtil.writeUtf8String(newIpv6, Constant.V6_FILE);
        LogUtil.info("IP已更新为：[%s, %s]", newIpv4, newIpv6);
    }

    private String getIpv4() {
        List<Any> v4api = CONFIG.getList("v4api");
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

    private String getIpv6() {
        List<Any> v6api = CONFIG.getList("v6api");
        for (Any api : v6api) {
            try {
                String result = HttpUtil.get(api.get("url").toString());
                if(api.keys().contains("field") && !api.get("field").toString().isBlank()) {
                    return JsonIterator.deserialize(result).get(api.get("field").toString()).toString();
                }
                return result;
            }catch (Exception e){
                LogUtil.error("获取IPv6地址错误: %s（%s）", api.get("url").toString(), e.getMessage());
            }
        }
        return null;
    }

}
