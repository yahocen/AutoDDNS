package org.addns.dns;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import org.addns.conf.Constant;
import org.addns.util.DomainUtil;
import org.addns.util.LogUtil;
import org.addns.util.StrUtil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author YahocenMiniPC
 */
public class TrafficRouteDnsOper implements DnsOper {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final String DEFAULT_LINE = "default";
    private static final int TTL_VALUE = 600;

    private final String AK;
    private final String SK;

    public TrafficRouteDnsOper(Any conf) {
        this.AK = conf.get("ak").toString();
        this.SK = conf.get("sk").toString();
    }

    @Override
    public void editDnsV4(String domain, String ipv4) {
        editDns(domain, ipv4, Constant.MODE_V4);
    }

    @Override
    public void editDnsV6(String domain, String ipv6) {
        editDns(domain, ipv6, Constant.MODE_V6);
    }

    private void editDns(String domain, String ipAddress, String recordType) {
        String subdomain = DomainUtil.getSubdomain(domain);
        Optional<Long> zid = getZid(DomainUtil.getRegisteredDomain(domain));
        if(zid.isEmpty()) {
            LogUtil.info("Error, unable to find domain name record: {} parsing: {} -> {}", recordType, domain, ipAddress);
            return;
        }
        handleRecord(zid.get(), domain, ipAddress, subdomain, recordType);
    }

    private void handleRecord(Long zid, String domain, String ipAddress, String subdomain, String recordType) {
        Optional<String> recordId = getRecordId(zid, domain, recordType);
        try {
            if (recordId.isPresent()) {
                updateDns(recordId.get(), recordType, ipAddress, subdomain);
                LogUtil.info("Update {} parsing successful: {} -> {}", recordType, domain, ipAddress);
            } else {
                createDns(zid, recordType, ipAddress, subdomain);
                LogUtil.info("{} parsing successfully created: {} -> {}", recordType, domain, ipAddress);
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            // 根据 recordId 是否存在来决定日志信息
            if (recordId.isPresent()) {
                LogUtil.error("Failed to update {} parsing: {} -> {}", recordType, e, domain, ipAddress);
            } else {
                LogUtil.error("Failed to create {} parsing: {} -> {}", recordType, e, domain, ipAddress);
            }
        }
    }

    //=================================================================

    private void updateDns(String recordId, String type, String ip, String host) throws URISyntaxException, IOException, InterruptedException {
        Map<String, Object> map = new HashMap<>(7);
        map.put("Host", host);
        map.put("Line", DEFAULT_LINE);
        map.put("RecordID", recordId);
        map.put("TTL", TTL_VALUE);
        map.put("Type", type);
        map.put("Value", ip);
        map.put("Weight", 1);
        byte[] body = JsonStream.serialize(map).getBytes();
        var result = postRequest("UpdateRecord", body);
        if(StrUtil.equals(result.get("RecordID").toString(), recordId)){
            LogUtil.info("Update DNS successfully");
        }else{
            LogUtil.error("Update DNS failed");
        }
    }

    private void createDns(Long zid, String type, String ip, String host) throws URISyntaxException, IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>(6);
        data.put("Host", host);
        data.put("Line", DEFAULT_LINE);
        data.put("TTL", TTL_VALUE);
        data.put("Type", type);
        data.put("Value", ip);
        data.put("ZID", zid);
        byte[] body = JsonStream.serialize(data).getBytes();
        var result = postRequest("CreateRecord", body);
        if(StrUtil.equals(result.as(String.class, "Host"), host)){
            LogUtil.info("Create DNS successfully");
        }else{
            LogUtil.error("Create DNS failed");
        }
    }

    private Optional<String> getRecordId(Long zid, String domain, String type) {
        try {
            var query = new HashMap<String, Object>(2) {{
                put("ZID", zid);
                put("PageSize", 500);
            }};
            var result = getRequest(query, "ListRecords", new byte[]{});
            var record = result.get("Records").asList()
                    .stream().filter(r -> domain.equals(r.toString("PQDN")) && type.equals(r.toString("Type")))
                    .findFirst();
            return record.map(e -> e.get("RecordID").toString());
        }catch (Exception e){
            LogUtil.error(e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Long> getZid(String domain) {
        try {
            var query = new HashMap<String, Object>(2) {{
                put("PageSize", 500);
            }};
            var result = getRequest(query, "ListZones", new byte[]{});
            var zone = result.get("Zones").asList().stream()
                    .filter(z -> domain.equals(z.toString("ZoneName")))
                    .findFirst();
            return zone.map(e -> e.get("ZID").as(Long.class));
        }catch (Exception e){
            LogUtil.error(e.getMessage());
            return Optional.empty();
        }
    }

    //=================================================================

    /**
     * sha256非对称加密
     */
    private static byte[] hmacSha256(byte[] key, String content) {
        try {
            Mac mac = Mac.getInstance("hmacSha256");
            mac.init(new SecretKeySpec(key, "hmacSha256"));
            return mac.doFinal(content.getBytes());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * sha256 hash算法
     */
    private static String hashSha256(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return encodeHexStr(md.digest(content));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     * @param byteArray 字节数组
     * @return 十六进制字符串
     */
    private static String encodeHexStr(byte[] byteArray) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : byteArray) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    /**
     * 发送请求
     */
    private Any request(String method, Map<String, Object> query, Map<String, String> header, String action, byte[] body) throws URISyntaxException, IOException, InterruptedException {
        // 初始化身份证明
        var credential = Map.of("accessKeyId", this.AK, "secretKeyId", this.SK, "service", "DNS", "region", "cn-north-1");

        //计算签名
        var queryList = new ArrayList<>(query.entrySet());
        queryList.sort(Map.Entry.comparingByKey());
        var pairs = queryList.stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.toCollection(ArrayList::new));
        pairs.add("Action=" + action);
        pairs.add("Version=2018-08-01");
        //按参数名称对查询参数进行升序排序
        pairs.sort(Comparator.comparing(s -> s.split("=")[0]));
        // 初始化签名结构
        record RequestParam(byte[] body, String method, Date date, String path, String host, String contentType, List<Map.Entry<String, Object>> queryList) {}
        var requestParam = new RequestParam(body, method, new Date(), "/", "open.volcengineapi.com", "application/json", queryList);
        var uri = new URI("https", requestParam.host, requestParam.path, String.join("&", pairs), null);
        // 接下来开始计算签名
        var formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("UTC"));
        var zonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"));
        var xDate = formatter.format(zonedDateTime);
        var shortXDate = xDate.substring(0, 8);
        var xContentSha256 = hashSha256(body);
        //计算
        var headStr = new String[]{"content-type", "host", "x-content-sha256", "x-date"};
        var signedHeadersStr = String.join(";", headStr);
        var headStrSecond = new String[]{"content-type:" + requestParam.contentType, "host:" + requestParam.host, "x-content-sha256:" + xContentSha256, "x-date:" + xDate};
        var preRequestStr = String.join("\n", headStrSecond);
        var preCanonicalRequestStr = new String[]{requestParam.method, requestParam.path, uri.getRawQuery(), preRequestStr, "", signedHeadersStr, xContentSha256};
        var canonicalRequestStr = String.join("\n", preCanonicalRequestStr);
        var hashedCanonicalRequest = hashSha256(canonicalRequestStr.getBytes());
        var credentialStr = new String[]{shortXDate, credential.get("region"), credential.get("service"), "request"};
        var credentialScope = String.join("/", credentialStr);
        var preStringToSign = new String[]{"HMAC-SHA256", xDate, credentialScope, hashedCanonicalRequest};
        var stringToSign = String.join("\n", preStringToSign);
        var kDate = hmacSha256(credential.get("secretKeyId").getBytes(), shortXDate);
        var kRegion = hmacSha256(kDate, credential.get("region"));
        var kService = hmacSha256(kRegion, credential.get("service"));
        var kSigning = hmacSha256(kService, "request");
        var signature = encodeHexStr(Objects.requireNonNull(hmacSha256(kSigning, stringToSign)));
        var authorization = String.format("HMAC-SHA256 Credential=%s, SignedHeaders=%s, Signature=%s", credential.get("accessKeyId") + "/" + credentialScope, signedHeadersStr, signature);
        //构建请求头
        var requestBuilder = HttpRequest.newBuilder().uri(uri)
                .header("Content-Type", requestParam.contentType)
                .header("X-Date", xDate)
                .header("X-Content-Sha256", xContentSha256)
                .header("Authorization", authorization);
        header.forEach(requestBuilder::header);
        //构建请求
        var request = switch (method.toUpperCase()) {
            case "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
            case "GET" -> requestBuilder.GET().build();
            default -> throw new UnsupportedOperationException();
        };
        //发送请求
        var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        //解析结果
        Any entries = JsonIterator.deserialize(response.body());
        if(!entries.keys().contains("Result")) {
            RuntimeException error = new RuntimeException(entries.get("ResponseMetadata").get("Error").toString("Message"));
            LogUtil.error("错误", error);
            throw error;
        }
        return entries.get("Result");
    }

    private Any postRequest(String action, byte[] body) throws URISyntaxException, IOException, InterruptedException {
        return request("POST", Map.of(), Map.of(), action, body);
    }

    private Any getRequest( Map<String, Object> query, String action, byte[] body) throws URISyntaxException, IOException, InterruptedException {
        return request("GET", query, Map.of(), action, body);
    }

    @Override
    public void close() {
        if(null != CLIENT) {
            CLIENT.close();
        }
    }

}