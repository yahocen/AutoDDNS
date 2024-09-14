package org.addns.core;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import org.addns.App;
import org.addns.conf.Command;
import org.addns.conf.Config;
import org.addns.conf.Constant;
import org.addns.dns.DnsOper;
import org.addns.util.FileUtil;
import org.addns.util.HttpUtil;
import org.addns.util.LogUtil;
import org.addns.util.StrUtil;

import java.util.List;

/**
 * @author YahocenMiniPC
 */
public class DDNS implements Runnable {

    private static volatile DDNS instance;

    private String oldIpv4 = Constant.V4_FILE.exists() ? FileUtil.readUtf8String(Constant.V4_FILE) :null;

    private String oldIpv6 = FileUtil.exist(Constant.V6_FILE) ?FileUtil.readUtf8String(Constant.V6_FILE) :null;

    private DDNS() {}

    /**
     * 获取单例实例的方法。
     */
    public static DDNS getInstance() {
        if (instance == null) {
            synchronized (DDNS.class) {
                if (instance == null) {
                    instance = new DDNS();
                }
            }
        }
        return instance;
    }

    @Override
    public void run() {
        try {
            ddns();
        } catch (Exception e){
            LogUtil.error("本次 DDNS 处理失败", e);
        } finally {
            System.gc();
        }
    }

    /**
     * DDNS 主要业务
     */
    public void ddns() {
        //检查IPv4变化
        String newIpv4 = getIpv4();
        boolean v4Change = !StrUtil.equals(newIpv4, oldIpv4);
        //检查IPv6变化
        String newIpv6 = getIpv6();
        boolean v6Change = !StrUtil.equals(newIpv6, oldIpv6);
        //没有变化，结束进程
        if(!v4Change && !v6Change) {
            LogUtil.info("IP未发生变化：[%s, %s]", newIpv4, newIpv6);
            return;
        }
        //更新DNS解析
        Config.getList("domains").parallelStream().forEach(domain -> {
            try {
                App.getDnsOper(domain.get(Constant.DNS_KEY).toString()).ifPresent(dnsOper -> {
                    if(Constant.MODE_V4.equals(domain.get(Constant.MODE_KEY).toString())) {
                        dnsOper.editDnsV4(domain.get(Constant.DOMAIN_KEY).toString(), newIpv4);
                    }else{
                        dnsOper.editDnsV6(domain.get(Constant.DOMAIN_KEY).toString(), newIpv6);
                    }
                });
            }catch (Exception e){
                LogUtil.error("更新域名解析失败：%s", e, domain.toString());
            }
        });
        //保存新IP
        oldIpv4 = FileUtil.writeUtf8String(newIpv4, Constant.V4_FILE);
        oldIpv6 = FileUtil.writeUtf8String(newIpv6, Constant.V6_FILE);
        LogUtil.info("IP已更新为：[%s, %s]", newIpv4, newIpv6);
    }

    /**
     * 获取公网IPv4
     * @return 公网IPv4
     */
    private String getIpv4() {
        List<Any> v4api = Config.getList("v4api");
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

    /**
     * 获取公网IPv6
     * @return 公网IPv6
     */
    private String getIpv6() {
        List<Any> v6api = Config.getList("v6api");
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
