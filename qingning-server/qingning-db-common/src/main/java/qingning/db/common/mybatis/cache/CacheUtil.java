/*
package qingning.db.common.mybatis.cache;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

*/
/**
 * Created by Administrator on 2017/9/14.
 *//*

public class CacheUtil {
    private static String configPropertyPath="classpath:application.properties";
    private static Map<String, String> configProperty = null;

    public static Map<String, String> convertPropertiesFileToMap(String path) throws Exception{
        InputStream input = null;
        Map<String,String> propertiesMap = null;
        try{
            if(path.toLowerCase().startsWith("classpath:")){
                String fileName = path.substring("classpath:".length());
                input = CacheUtil.class.getClassLoader().getResourceAsStream(fileName);
                if(input==null){
                    path=CacheUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                    path=path.substring(0, path.lastIndexOf(File.separatorChar)+1)+fileName;
                }
            }
            if(input==null){
                input = new FileInputStream(path);
            }

            Properties properties = new Properties();
            properties.load(input);
            propertiesMap = new HashMap<>();
            for(String name : properties.stringPropertyNames()){
                propertiesMap.put(name, properties.getProperty(name));
            }
        }catch (IOException e){

        }finally{
            if(input!=null){
                input.close();
            }
        }
        return propertiesMap;
    }
    public static String getConfigByKey(String key) {
        String value="";
        if(StringUtils.isEmpty(key)){
            return null;
        }
        try{
            if(configProperty==null){
                configProperty= CacheUtil.convertPropertiesFileToMap(configPropertyPath);
            }
            value = configProperty.get(key);
        } catch(Exception e){
            //TODO add log info
        }
        return value;
    }
}
*/
