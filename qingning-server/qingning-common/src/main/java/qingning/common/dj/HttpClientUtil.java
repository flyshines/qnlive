package qingning.common.dj;

import com.alibaba.fastjson.JSON;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
                for(Map.Entry<String,String> entry : headerParams.entrySet()){
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
                    se.setContentType("application/json");
                    se.setContentEncoding(HTTP.UTF_8);
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
