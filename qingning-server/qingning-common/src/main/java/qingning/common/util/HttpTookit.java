package qingning.common.util;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by qingning on 2015/9/15.
 */
public class HttpTookit {
    private static Logger logger = LoggerFactory.getLogger(HttpTookit.class);
    private static final CloseableHttpClient httpClient;
    public static final String CHARSET = "UTF-8";

    static {
        RequestConfig config = RequestConfig.custom().setConnectTimeout(60000).setSocketTimeout(15000).build();
        httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    public String doLazyGet(String url, Map<String, Object> params)
    {
        synchronized(this){
            try {
                Thread.sleep(300); //延时0.1秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return doGet(url, params,CHARSET);
        }
    }

    public static String doGet(String url, Map<String, Object> params){
        return doGet(url, params,CHARSET);
    }
    public static String doPost(String url, Map<String, String> params){
        return doPost(url, params,CHARSET);
    }
    /**
     * HTTP Get 获取内容
     * @param url  请求的url地址 ?之前的地址
     * @param params 请求的参数
     * @param charset    编码格式
     * @return    页面内容
     */
    public static String doGet(String url,Map<String,Object> params,String charset){
        if(StringUtils.isBlank(url)){
            return null;
        }
        try {
            if(params != null && !params.isEmpty()){
//                List<NameValuePair> pairs = new ArrayList<NameValuePair>(params.size());
//                String paramStr = "";
//                for(Map.Entry<String,String> entry : params.entrySet()){
//                    String value = entry.getValue();
//                    if(value != null){
//                        pairs.add(new BasicNameValuePair(entry.getKey(),value));
//                        paramStr += entry.getKey() + "=" + value + "&";
//                    }
//                }
//                if(paramStr.contains("&"))
//                    paramStr = paramStr.substring(0, paramStr.length()-1);
//                url += "?" + paramStr;//EntityUtils.toString(new UrlEncodedFormEntity(pairs, charset));

                url = getURL(url, params);
            }

//          logger.info("指数url:" + url);
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                httpGet.abort();
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
    
    
    public static String doGet(String url){
        if(MiscUtils.isEmpty(url)){
            return null;
        }
        try {

//          logger.info("指数url:" + url);
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                httpGet.abort();
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

    public  static String getURL(String p_url, Map<String, Object> params) {
        StringBuilder url = new StringBuilder(p_url);
        if(url.indexOf("?")<0)
            url.append('?');

        for(String name : params.keySet()){
            url.append('&');
            url.append(name);
            url.append('=');
            url.append(UrlEncode(String.valueOf(params.get(name))));
        }
        return url.toString().replace("?&", "?");
    }

    public static String UrlEncode(String s) {
        try {
            s = URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return s;
    }


    /**
     * HTTP Post 获取内容
     * @param url  请求的url地址 ?之前的地址
     * @param params 请求的参数
     * @param charset    编码格式
     * @return    页面内容
     */
    public static String doPost(String url,Map<String,String> params,String charset){
        if(StringUtils.isBlank(url)){
            return null;
        }
        try {
            List<NameValuePair> pairs = null;
            if(params != null && !params.isEmpty()){
                pairs = new ArrayList<NameValuePair>(params.size());
                for(Map.Entry<String,String> entry : params.entrySet()){
                    String value = entry.getValue();
                    if(value != null){
                        pairs.add(new BasicNameValuePair(entry.getKey(),value));
                    }
                }
            }
            HttpPost httpPost = new HttpPost(url);
            if(pairs != null && pairs.size() > 0){
                httpPost.setEntity(new UrlEncodedFormEntity(pairs,CHARSET));
            }

            CloseableHttpResponse response = httpClient.execute(httpPost);
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

    /**
     * HTTP Post 获取内容
     * @param url  请求的url地址 ?之前的地址
     * @param headerParams  请求头的参数
     * @param bodyParams 请求体的参数
     * @param charset    编码格式
     * @return    页面内容
     */
    public static String doPost(String url, Map<String,String> headerParams, Map<String,String> bodyParams, String charset){

        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(3000).setConnectTimeout(3000).build();//设置请求和传输超时时间
        if(StringUtils.isBlank(url)){
            return null;
        }

        try {
            HttpPost post = new HttpPost(url);

            if(bodyParams != null && bodyParams.size() > 0){
                String contentJsonString = JSON.toJSONString(bodyParams);

                if(contentJsonString != null){
                   // String encoderJson = URLEncoder.encode(contentJsonString, charset);
                    String encoderJson = contentJsonString;
                    StringEntity se = new StringEntity(encoderJson);
                    se.setContentType("application/json");
                    se.setContentEncoding(HTTP.UTF_8);
                    post.setEntity(se);
                    logger.info(EntityUtils.toString(post.getEntity(), "utf-8"));
                }
            }

            if(headerParams != null && !headerParams.isEmpty()){
                for(Map.Entry<String,String> entry : headerParams.entrySet()){
                    String value = entry.getValue();
                    if(value != null){
                        post.addHeader(entry.getKey(),value);
                    }
                }
            }

            logger.info("===post请求信息==="+post.toString());
            //logger.info("===post请求信息==="+post.getEntity().);
            CloseableHttpResponse response = httpClient.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                post.abort();
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

    /**
     *获取ip地址
     * @param request 请求
     * @return
     */
    public static String getIpAdress(HttpServletRequest request){
        String ip = request.getHeader ("X-Real-IP");
        if (!StringUtils.isBlank (ip) && !"unknown".equalsIgnoreCase (ip)) { return ip; }
        ip = request.getHeader ("X-Forwarded-For");
        if (!StringUtils.isBlank (ip) && !"unknown".equalsIgnoreCase (ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP。
            int index = ip.indexOf (',');
            if (index != -1) {
                return ip.substring (0, index);
            } else {
                return ip;
            }
        } else {
            return request.getRemoteAddr();
        }
    }


    public static void main(String []args){
//        Map<String, String> param = new HashMap<String, String>();
//        param.put("name", "wan");
//        param.put("pass", "123456");
//        param.put("timestamp", MiscUtil.getTimestamp()+"");
//
//        String sign = "";
//        Object[] names = param.keySet().toArray();
//        Arrays.sort(names);
//        for (Object name : names)
//        {
//            sign += name + "=" + param.get(name) + "&";
//        }
//
//        sign += "zixiong&qingning";

        /*param.put("sign", Md5Util.getMD5(sign));

        doPost("http://192.168.2.158:8080/infoServer/api/chart_add", param);*/
    	
    	String data = doGet("http://7xqyrg.media1.z0.glb.clouddn.com/video/Ab82ccc26b89fb334012b735e04753e43.mp4?avinfo");
    	System.out.println("视频信息：" + data);
    }
}
