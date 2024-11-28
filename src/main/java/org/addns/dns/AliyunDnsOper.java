package org.addns.dns;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import org.addns.conf.Constant;
import org.addns.util.DomainUtil;
import org.addns.util.HttpUtil;
import org.addns.util.LogUtil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author YahocenMiniPC
 */
public class AliyunDnsOper implements DnsOper {

    /**
     * 日期格式化工具，用于将日期时间字符串格式化为"yyyy-MM-dd'T'HH:mm:ss'Z'"的格式。
     */
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * RPC接口无资源路径，故使用正斜杠（/）作为CanonicalURI
     */
    private static final String CANONICAL_URI = "/";

    /**
     * endpoint
     */
    private static final String HOST = "dns.aliyuncs.com";

    /**
     * API版本号
     */
    private static final String X_ACS_VERSION = "2015-01-09";

    /**
     * 这里通过环境变量获取Access Key ID和Access Key Secret，
     */
    private final String ACCESS_KEY_ID;

    private final String ACCESS_KEY_SECRET;

    public AliyunDnsOper(Any conf) {
        this.ACCESS_KEY_ID = conf.get("ak").toString();
        this.ACCESS_KEY_SECRET = conf.get("sk").toString();
    }

    @Override
    public void editDnsV4(String domain, String ipv4) {
        editDns(domain, Constant.MODE_V4, ipv4);
    }

    @Override
    public void editDnsV6(String domain, String ipv6) {
        editDns(domain, Constant.MODE_V6, ipv6);
    }

    private void editDns(String domain, String type, String value) {
        getRecordId(domain, type).ifPresentOrElse(rid -> updateDomainRecord(rid, domain, type, value), () -> addDomainRecord(domain, type, value));
    }

    private Optional<String> getRecordId(String domain, String type) {
        // RPC接口请求
        String httpMethod = "GET";
        String xAcsAction = "DescribeDomainRecords";
        Request request = new Request(httpMethod, CANONICAL_URI, HOST, xAcsAction, X_ACS_VERSION);
        // 调用API所需要的参数，参数按照参数名的字符代码升序排列，具有重复名称的参数应按值进行排序。
        request.queryParam.put("DomainName", DomainUtil.getRegisteredDomain(domain));
        request.queryParam.put("PageSize", 500);
        // 签名过程
        getAuthorization(request);
        // 调用API
        String resultStr = callApi(request);
        if(null == resultStr) {
            return Optional.empty();
        }
        //解析结果
        Any deserialize = JsonIterator.deserialize(resultStr);
        var rr = DomainUtil.getSubdomain(domain);
        return deserialize.get("DomainRecords").get("Record").asList().stream()
                .filter(r -> r.get("Type").toString().equals(type) && r.get("RR").toString().equals(rr))
                .map(r -> r.get("RecordId").toString())
                .findFirst();
    }

    private void addDomainRecord(String domain, String type, String value) {
        // RPC接口请求
        String httpMethod = "POST";
        String xAcsAction = "AddDomainRecord";
        Request request = new Request(httpMethod, CANONICAL_URI, HOST, xAcsAction, X_ACS_VERSION);
        // 调用API所需要的参数，参数按照参数名的字符代码升序排列，具有重复名称的参数应按值进行排序。
        request.queryParam.put("DomainName", DomainUtil.getRegisteredDomain(domain));
        request.queryParam.put("RR", DomainUtil.getSubdomain(domain));
        request.queryParam.put("Type", type);
        request.queryParam.put("Value", value);
        // 签名过程
        getAuthorization(request);
        // 调用API
        String resultStr = callApi(request);
        LogUtil.info("addDomainRecord:" + resultStr);
    }

    private void updateDomainRecord(String recordId, String domain, String type, String value) {
        // RPC接口请求
        String httpMethod = "POST";
        String xAcsAction = "UpdateDomainRecord";
        Request request = new Request(httpMethod, CANONICAL_URI, HOST, xAcsAction, X_ACS_VERSION);
        // 调用API所需要的参数，参数按照参数名的字符代码升序排列，具有重复名称的参数应按值进行排序。
        request.queryParam.put("RecordId", recordId);
        request.queryParam.put("RR", DomainUtil.getSubdomain(domain));
        request.queryParam.put("Type", type);
        request.queryParam.put("Value", value);
        // 签名过程
        getAuthorization(request);
        // 调用API
        String resultStr = callApi(request);
        LogUtil.info("updateDomainRecord:" + resultStr);
    }

    private static class Request {
        // HTTP Method
        private final String httpMethod;
        // 请求路径，当资源路径为空时，使用正斜杠(/)作为CanonicalURI
        private final String canonicalUri;
        // endpoint
        private final String host;
        // API name
        private final String xAcsAction;
        // API version
        private final String xAcsVersion;
        // headers
        TreeMap<String, Object> headers = new TreeMap<>();
        // 调用API所需要的参数，参数位置在body。Json字符串
        String body;
        // 调用API所需要的参数，参数位置在query，参数按照参数名的字符代码升序排列
        TreeMap<String, Object> queryParam = new TreeMap<>();

        public Request(String httpMethod, String canonicalUri, String host, String xAcsAction, String xAcsVersion) {
            this.httpMethod = httpMethod;
            this.canonicalUri = canonicalUri;
            this.host = host;
            this.xAcsAction = xAcsAction;
            this.xAcsVersion = xAcsVersion;
            initHeader();
        }

        // init headers
        private void initHeader() {
            //headers.put("host", host);
            headers.put("x-acs-action", xAcsAction);
            headers.put("x-acs-version", xAcsVersion);
            // 设置日期格式化时区为GMT
            SDF.setTimeZone(new SimpleTimeZone(0, "GMT"));
            headers.put("x-acs-date", SDF.format(new Date()));
            headers.put("x-acs-signature-nonce", UUID.randomUUID().toString());
        }
    }

    /**
     * 签名协议
     */
    private static final String ALGORITHM = "ACS3-HMAC-SHA256";

    private String callApi(Request request) {
        try {
            // 构建 URI
            String url = "https://" + request.host + request.canonicalUri;
            URI uri = new URI(url + (request.queryParam.isEmpty() ? "" : "?" + toQueryString(request.queryParam)));
            // 创建 HttpRequest.Builder
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri).headers(toHeadersArray(request.headers));
            // 根据 HTTP 方法设置请求体（如果需要）
            switch (request.httpMethod) {
                case "GET":
                    builder.GET();
                    break;
                case "POST":
                    if (request.body != null) {
                        builder.POST(HttpRequest.BodyPublishers.ofString(request.body, StandardCharsets.UTF_8));
                    } else {
                        builder.POST(HttpRequest.BodyPublishers.noBody());
                    }
                    break;
                case "DELETE":
                    builder.DELETE();
                    break;
                default:
                    LogUtil.info("Unsupported HTTP method: " + request.httpMethod);
                    throw new IllegalArgumentException("Unsupported HTTP method");
            }
            // 发送请求并获取响应
            HttpResponse<String> response = HttpUtil.HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            // 打印响应
            return response.body();
        } catch (Exception e) {
            // 异常处理
            LogUtil.error("Failed to send request", e);
            return null;
        }
    }

    private static String toQueryString(TreeMap<String, Object> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
    }

    private static String[] toHeadersArray(TreeMap<String, Object> headers) {
        return headers.entrySet().stream()
                .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue().toString()))
                .toArray(String[]::new);
    }

    /**
     * 该方法用于根据传入的HTTP请求方法、规范化的URI、查询参数等，计算并生成授权信息。
     */
    private void getAuthorization(Request request) {
        try {
            // 处理queryParam中参数值为List、Map类型的参数，将参数平铺
            TreeMap<String, Object> newQueryParam = new TreeMap<>();
            processObject(newQueryParam, "", request.queryParam);
            request.queryParam = newQueryParam;
            // 步骤 1：拼接规范请求串
            // 请求参数，当请求的查询字符串为空时，使用空字符串作为规范化查询字符串
            StringBuilder canonicalQueryString = new StringBuilder();
            request.queryParam.entrySet().stream().map(entry -> percentCode(entry.getKey()) + "=" + percentCode(String.valueOf(entry.getValue()))).forEachOrdered(queryPart -> {
                // 如果canonicalQueryString已经不是空的，则在查询参数前添加"&"
                if (!canonicalQueryString.isEmpty()) {
                    canonicalQueryString.append("&");
                }
                canonicalQueryString.append(queryPart);
            });
            // 请求体，当请求正文为空时，比如GET请求，RequestPayload固定为空字符串
            String requestPayload = "";
            if (request.body != null) {
                requestPayload = request.body;
            }

            // 计算请求体的哈希值
            String hashedRequestPayload = sha256Hex(requestPayload);
            request.headers.put("x-acs-content-sha256", hashedRequestPayload);
            // 构造请求头，多个规范化消息头，按照消息头名称（小写）的字符代码顺序以升序排列后拼接在一起
            StringBuilder canonicalHeaders = new StringBuilder();
            // 已签名消息头列表，多个请求头名称（小写）按首字母升序排列并以英文分号（;）分隔
            StringBuilder signedHeadersSb = new StringBuilder();
            request.headers.entrySet().stream().filter(entry -> entry.getKey().toLowerCase().startsWith("x-acs-") || "host".equalsIgnoreCase(entry.getKey()) || "content-type".equalsIgnoreCase(entry.getKey())).sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                String lowerKey = entry.getKey().toLowerCase();
                String value = String.valueOf(entry.getValue()).trim();
                canonicalHeaders.append(lowerKey).append(":").append(value).append("\n");
                signedHeadersSb.append(lowerKey).append(";");
            });
            String signedHeaders = signedHeadersSb.substring(0, signedHeadersSb.length() - 1);
            String canonicalRequest = request.httpMethod + "\n" + request.canonicalUri + "\n" + canonicalQueryString + "\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + hashedRequestPayload;
            LogUtil.debug("canonicalRequest=========>\n" + canonicalRequest);

            // 步骤 2：拼接待签名字符串 计算规范化请求的哈希值
            String hashedCanonicalRequest = sha256Hex(canonicalRequest);
            String stringToSign = ALGORITHM + "\n" + hashedCanonicalRequest;
            LogUtil.debug("stringToSign=========>\n" + stringToSign);

            // 步骤 3：计算签名
            String signature = HexFormat.of().formatHex(hmac256(ACCESS_KEY_SECRET.getBytes(StandardCharsets.UTF_8), stringToSign)).toLowerCase();
            LogUtil.debug("signature=========>" + signature);

            // 步骤 4：拼接 Authorization
            String authorization = ALGORITHM + " " + "Credential=" + ACCESS_KEY_ID + ",SignedHeaders=" + signedHeaders + ",Signature=" + signature;
            LogUtil.debug("authorization=========>" + authorization);
            request.headers.put("Authorization", authorization);
        } catch (Exception e) {
            // 异常处理
            LogUtil.error("Failed to get authorization", e);
        }
    }

    /**
     * 递归处理对象，将复杂对象（如Map和List）展开为平面的键值对
     *
     * @param map   原始的键值对集合，将被递归地更新
     * @param key   当前处理的键，随着递归的深入，键会带有嵌套路径信息
     * @param value 对应于键的值，可以是嵌套的Map、List或其他类型
     */
    private static void processObject(Map<String, Object> map, String key, Object value) {
        // 如果值为空，则无需进一步处理
        if (value == null) {
            return;
        }
        if (key == null) {
            key = "";
        }
        // 当值为List类型时，遍历List中的每个元素，并递归处理
        if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); ++i) {
                processObject(map, key + "." + (i + 1), list.get(i));
            }
        } else if (value instanceof Map<?, ?> subMap) {
            // 当值为Map类型时，遍历Map中的每个键值对，并递归处理
            for (Map.Entry<?, ?> entry : subMap.entrySet()) {
                processObject(map, key + "." + entry.getKey().toString(), entry.getValue());
            }
        } else {
            // 对于以"."开头的键，移除开头的"."以保持键的连续性
            if (key.startsWith(".")) {
                key = key.substring(1);
            }
            // 对于byte[]类型的值，将其转换为UTF-8编码的字符串
            if (value instanceof byte[]) {
                map.put(key, new String((byte[]) value, StandardCharsets.UTF_8));
            } else {
                // 对于其他类型的值，直接转换为字符串
                map.put(key, String.valueOf(value));
            }
        }
    }

    /**
     * 使用HmacSHA256算法生成消息认证码（MAC）。
     *
     * @param secretKey 密钥，用于生成MAC的密钥，必须保密。
     * @param str       需要进行MAC认证的消息。
     * @return 返回使用HmacSHA256算法计算出的消息认证码。
     * @throws Exception 如果初始化MAC或计算MAC过程中遇到错误，则抛出异常。
     */
    private static byte[] hmac256(byte[] secretKey, String str) throws Exception {
        // 实例化HmacSHA256消息认证码生成器
        Mac mac = Mac.getInstance("HmacSHA256");
        // 创建密钥规范，用于初始化MAC生成器
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, mac.getAlgorithm());
        // 初始化MAC生成器
        mac.init(secretKeySpec);
        // 计算消息认证码并返回
        return mac.doFinal(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 使用SHA-256算法计算字符串的哈希值并以十六进制字符串形式返回。
     *
     * @param stringToSign 需要进行SHA-256哈希计算的字符串。
     * @return 计算结果为小写十六进制字符串。
     * @throws Exception 如果在获取SHA-256消息摘要实例时发生错误。
     */
    private static String sha256Hex(String stringToSign) throws Exception {
        // 获取SHA-256消息摘要实例
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        // 计算字符串s的SHA-256哈希值
        byte[] d = md.digest(stringToSign.getBytes(StandardCharsets.UTF_8));
        // 将哈希值转换为小写十六进制字符串并返回
        return HexFormat.of().formatHex(d).toLowerCase();
    }

    /**
     * 对指定的字符串进行URL编码。
     * 使用UTF-8编码字符集对字符串进行编码，并对特定的字符进行替换，以符合URL编码规范。
     *
     * @param str 需要进行URL编码的字符串。
     * @return 编码后的字符串。其中，加号"+"被替换为"%20"，星号"*"被替换为"%2A"，波浪号"%7E"被替换为"~"。
     */
    private static String percentCode(String str) {
        if (str == null) {
            throw new IllegalArgumentException("输入字符串不可为null");
        }
        return URLEncoder.encode(str, StandardCharsets.UTF_8).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

}