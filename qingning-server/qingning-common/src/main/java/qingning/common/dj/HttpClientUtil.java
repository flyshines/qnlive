package qingning.common.dj;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

/* 
 * 利用HttpClient进行post请求的工具类 
 */  
public class HttpClientUtil {  
    public String doPost(String url,Map<String,String> headerParams,Map<String,String> map,String charset){
        HttpClient httpClient = null;
        HttpPost httpPost = null;
        String result = null;  
        try{  
            httpClient = new SSLClient();  
            httpPost = new HttpPost(url);
            if(headerParams != null && !headerParams.isEmpty()){
                for(Entry<String,String> entry : headerParams.entrySet()){
                    String value = entry.getValue();
                    if(value != null){
                        httpPost.addHeader(entry.getKey(),value);
                    }
                }
            }

            if(map != null && map.size() > 0){
                String contentJsonString = JSON.toJSONString(map);

                if(contentJsonString != null){
                    // String encoderJson = URLEncoder.encode(contentJsonString, charset);
                    String encoderJson = contentJsonString;
                    StringEntity se = new StringEntity(encoderJson);
                    se.setContentType("application/json;charset=UTF-8");
                    //se.setContentEncoding(HTTP.UTF_8);
                    httpPost.setHeader("Accept", "application/json");
                    httpPost.addHeader("Content-type","application/json; charset=utf-8");
                    httpPost.setEntity(se);
                  //  logger.info(EntityUtils.toString(post.getEntity(), "utf-8"));
                }
            }


            HttpResponse response = httpClient.execute(httpPost);
            if(response != null){
                HttpEntity resEntity = response.getEntity();
                if(resEntity != null){
                    result = EntityUtils.toString(resEntity,charset);
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return result;
    }
    public static String doPostUrl(String url,Map<String,String> headerParams,Map<String,Object> map,String charset){
        HttpClient httpClient = null;
        HttpPost httpPost = null;
        String result = null;
        try{
            httpClient = new SSLClient();
            httpPost = new HttpPost(url);
            if(headerParams != null && !headerParams.isEmpty()){
                for(Entry<String,String> entry : headerParams.entrySet()){
                    String value = entry.getValue();
                    if(value != null){
                        httpPost.addHeader(entry.getKey(),value);
                    }
                }
            }

            if(map != null && map.size() > 0){
                String contentJsonString = JSON.toJSONString(map);

                if(contentJsonString != null){
                    // String encoderJson = URLEncoder.encode(contentJsonString, charset);
                    String encoderJson = contentJsonString;
                    StringEntity se = new StringEntity(encoderJson, Charset.forName("UTF-8"));
                    se.setContentType("application/json");
                    //se.setContentEncoding(HTTP.UTF_8);
                    httpPost.addHeader("Content-type","application/json");
                    httpPost.setEntity(se);
                    //  logger.info(EntityUtils.toString(post.getEntity(), "utf-8"));
                }
            }


            HttpResponse response = httpClient.execute(httpPost);
            if(response != null){
                HttpEntity resEntity = response.getEntity();
                if(resEntity != null){
                    result = EntityUtils.toString(resEntity,charset);
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * HTTP Get 获取内容
     * @param url  请求的url地址 ?之前的地址
     * @param params 请求的参数
     * @param charset    编码格式
     * @return    页面内容
     */
    public static String doGet(String url,Map<String,String> headerParams,Map<String,Object> params,String charset){
        if(StringUtils.isBlank(url)){
            return null;
        }
        try {
            if(params != null && !params.isEmpty()){
                url = getURL(url, params);
            }

//          logger.info("指数url:" + url);
            HttpClient httpClient = new SSLClient();
            HttpGet httpGet = new HttpGet(url);
            if(headerParams != null && !headerParams.isEmpty()){
                for(Entry<String,String> entry : headerParams.entrySet()){
                    String value = entry.getValue();
                    if(value != null){
                        httpGet.addHeader(entry.getKey(),value);
                    }
                }
            }
            HttpResponse response = httpClient.execute(httpGet);
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


    public static String doPutUrl(String url,Map<String,String> headerParams,Map<String,String> map,String charset){
        HttpClient httpClient = null;
        HttpPut httpPost = null;
        String result = null;
        try{
            httpClient = new SSLClient();
            httpPost = new HttpPut(url);
            if(headerParams != null && !headerParams.isEmpty()){
                for(Entry<String,String> entry : headerParams.entrySet()){
                    String value = entry.getValue();
                    if(value != null){
                        httpPost.addHeader(entry.getKey(),value);
                    }
                }
            }

            if(map != null && map.size() > 0){
                String contentJsonString = JSON.toJSONString(map);

                if(contentJsonString != null){
                    // String encoderJson = URLEncoder.encode(contentJsonString, charset);
                    String encoderJson = contentJsonString;
                    StringEntity se = new StringEntity(encoderJson, Charset.forName("UTF-8"));
                    se.setContentType("application/json");
                    //se.setContentEncoding(HTTP.UTF_8);
                    httpPost.addHeader("Content-type","application/json");
                    httpPost.setEntity(se);
                    //  logger.info(EntityUtils.toString(post.getEntity(), "utf-8"));
                }
            }


            HttpResponse response = httpClient.execute(httpPost);
            if(response != null){
                HttpEntity resEntity = response.getEntity();
                if(resEntity != null){
                    result = EntityUtils.toString(resEntity,charset);
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return result;
    }
}
