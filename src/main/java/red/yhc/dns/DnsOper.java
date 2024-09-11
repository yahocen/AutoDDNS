package red.yhc.dns;

import com.jsoniter.any.Any;
import red.yhc.conf.Config;

/**
 * @author YahocenMiniPC
 */
public interface DnsOper {

    void editDnsV4(String domain, String ipv4);

    void editDnsV6(String domain, String ipv6);

}
