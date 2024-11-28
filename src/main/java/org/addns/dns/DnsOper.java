package org.addns.dns;

/**
 * @author YahocenMiniPC
 */
public interface DnsOper extends AutoCloseable {

    void editDnsV4(String domain, String ipv4);

    void editDnsV6(String domain, String ipv6);

}
