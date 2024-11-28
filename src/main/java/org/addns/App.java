package org.addns;

import org.addns.conf.Command;
import org.addns.conf.Config;
import org.addns.conf.Constant;
import org.addns.core.DDNS;
import org.addns.core.DDNSError;
import org.addns.dns.AliyunDnsOper;
import org.addns.dns.DnsOper;
import org.addns.dns.TrafficRouteDnsOper;
import org.addns.util.FileUtil;
import org.addns.util.HttpUtil;
import org.addns.util.LogUtil;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author YahocenMiniPC
 */
public class App {

    private static final Map<String, DnsOper> DDNS_OPER = new HashMap<>();

    public static void main(String[] args) {
        //初始化命令行参数
        Command.initInstance(args);
        if(Command.Option.HELP.is()) {
            Command.usage(System.out);
            System.exit(0);
        }
        if(Command.Option.VERSION.is()) {
            Command.version(System.out);
            System.exit(0);
        }
        //查询是否有实例正在运行
        if(Constant.PID_FILE.exists()) {
            long pid = Long.parseLong(FileUtil.readUtf8String(Constant.PID_FILE));
            var processHandle = ProcessHandle.of(pid);
            //是否用于停止现有进程
            if(processHandle.isPresent() && Command.Option.STOP.is()) {
                processHandle.ifPresent(ProcessHandle::destroy);
                LogUtil.info("终止正在运行的进程：%s", pid);
                return;
            }
            //仅允许一个进程运行
            if(processHandle.isPresent()) {
                LogUtil.warn("已有进程正在运行：%s，当前进程终止", pid);
                return;
            }
        }
        //初始化配置信息
        Config.initInstance(Command.Option.CONF.get());
        //初始化 DNS API
        initDnsOper();
        //创建单线程池
        var executor = Executors.newSingleThreadScheduledExecutor();
        // 安排配置定时进行IP变化扫描
        executor.scheduleAtFixedRate(DDNS.getInstance(), 1, Config.getLong("period"), TimeUnit.SECONDS);
        // 系统关闭时释放资源
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdown();
            HttpUtil.HTTP_CLIENT.close();
        }));
        //保存进程PID
        FileUtil.writeUtf8String(String.valueOf(ProcessHandle.current().pid()), Constant.PID_FILE);
    }

    private static void initDnsOper() {
        for (var dnsConf : Config.getList(Constant.DNS_KEY)) {
            DDNS_OPER.put(dnsConf.get("name").toString(), switch (dnsConf.get("type").toString()) {
                case "trddns" -> new TrafficRouteDnsOper(dnsConf);
                case "alidns" -> new AliyunDnsOper(dnsConf);
                //TODO 请在此处增加其他 DNS API 处理
                default -> throw new DDNSError("暂不支持此 DNS API 类型：%s", dnsConf.get("name"));
            });
        }
    }

    public static Optional<DnsOper> getDnsOper(String name) {
        return Optional.ofNullable(DDNS_OPER.get(name));
    }

}
