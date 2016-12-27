package qingning.common.util;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;

public class TenPayHttpClientUtil {

    private static final String USER_AGENT_VALUE = "Mozilla/4.0 (compatible; MSIE 6.0; Windows XP)";
    private static final int TIMEOUT = 30;

    public static HttpURLConnection getHttpURLConnection(String strUrl) throws IOException {
        URL url = new URL(strUrl);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        return httpURLConnection;
    }

    public static HttpsURLConnection getHttpsURLConnection(String strUrl) throws IOException {
        URL url = new URL(strUrl);
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
        return httpsURLConnection;
    }

    public static String doPost(HttpURLConnection conn, byte[] postData) throws IOException {
        // 以post方式通信
        conn.setRequestMethod("POST");
        // 设置请求默认属性
        setHttpRequest(conn);
        // Content-Type
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
        final int len = 1024; // 1KB
        doOutput(out, postData, len);
        // 读取响应
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String lines;
        StringBuffer sb = new StringBuffer("");
        while ((lines = reader.readLine()) != null) {
            lines = new String(lines.getBytes(), "utf-8");
            sb.append(lines);
        }
        // 关闭流
        out.close();
        return sb.toString();
    }

    public static String doPost(String url, String sendContent, CloseableHttpClient httpclient){
        if(StringUtils.isBlank(url)){
            return null;
        }
        try {
            HttpPost httpPost = new HttpPost(url);
            HttpEntity httpEntity = EntityBuilder.create().setText(sendContent).build();
            httpPost.setEntity(httpEntity);
            CloseableHttpResponse response = httpclient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                httpPost.abort();
                throw new RuntimeException("HttpClient,error status code :" + statusCode);
            }
            HttpEntity entity = response.getEntity();
            String result = null;
            if (entity != null){
                result = EntityUtils.toString(entity, "utf-8");
            }
            EntityUtils.consume(entity);
            response.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static CloseableHttpClient getWinxinRefundHttpClient(String mch_id) throws Exception {
        //指定读取证书格式为PKCS12
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        //读取本机存放的PKCS12证书文件
        FileInputStream instream = new FileInputStream(new File("D:/certs/gcw/apiclient_cert.p12"));
        try {
            //指定PKCS12的密码(商户ID)
            keyStore.load(instream, mch_id.toCharArray());
        } finally {
            instream.close();
        }
        SSLContext sslcontext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, mch_id.toCharArray()).build();
        //指定TLS版本
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslcontext, new String[]{"TLSv1"}, null,
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        //设置httpclient的SSLSocketFactory
        CloseableHttpClient httpclient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .build();
        return httpclient;
    }

    public static void doOutput(OutputStream out, byte[] data, int len) throws IOException {
        int dataLen = data.length;
        int off = 0;
        while (off < data.length) {
            if (len >= dataLen) {
                out.write(data, off, dataLen);
                off += dataLen;
            } else {
                out.write(data, off, len);
                off += len;
                dataLen -= len;
            }
            // 刷新缓冲区
            out.flush();
        }
    }

    public static void setHttpRequest(HttpURLConnection httpConnection) {
        // 设置连接超时时间
        httpConnection.setConnectTimeout(TIMEOUT * 1000);
        // User-Agent
        httpConnection.setRequestProperty("User-Agent", USER_AGENT_VALUE);
        // 不使用缓存
        httpConnection.setUseCaches(false);
        // 允许输入输出
        httpConnection.setDoInput(true);
        httpConnection.setDoOutput(true);
    }

}
