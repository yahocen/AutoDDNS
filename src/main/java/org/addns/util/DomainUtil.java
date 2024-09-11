package org.addns.util;

import org.addns.util.regdom4j.RegDomain;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * @author YahocenMiniPC
 */
public class DomainUtil {

    private static final RegDomain REG_DOMAIN = new RegDomain();

    /**
     * 通过域名获取注册域名
     * @param domain 域名
     * @return 返回主域名
     */
    public static String getRegisteredDomain(String domain) {
        return REG_DOMAIN.getRegisteredDomain(domain);
    }

    /**
     * 获取域名前的HOST
     * @param domain 域名
     * @return host部分
     */
    public static String getSubdomain(String domain) {
        String registeredDomain = getRegisteredDomain(domain);
        return domain.substring(0, domain.indexOf(registeredDomain) - 1);
    }

    public static String getTxtRecord(String domain) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        DirContext dirContext = new InitialDirContext(env);
        //There is an exception here
        Attributes attrs = dirContext.getAttributes(domain, new String[] { "TXT" });
        Attribute txt = attrs.get("TXT");
        NamingEnumeration<?> e = txt.getAll();
        String value = null;
        while (e.hasMore()) {
            value = e.next().toString();
        }
        return value;
    }

    public static List<String> getTxtRecordPatch(String domain) throws IOException, InterruptedException {
        List<String> txtRecords = new ArrayList<>();
        // 使用 ProcessBuilder 运行 nslookup 命令
        var pb = new ProcessBuilder("nslookup", "-query=TXT", domain);
        var process = pb.start();
        // 读取命令输出
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            boolean recordFound = false;
            while ((line = reader.readLine()) != null) {
                // 检查是否为TXT记录
                if (line.contains("text =")) {
                    recordFound = true;
                }
                // 获取TXT记录值部分
                if (recordFound && line.trim().startsWith("\"")) {
                    txtRecords.add(line.trim().replace("\"", ""));
                }
            }
        }
        process.waitFor();
        return txtRecords;
    }

}
