package qingning.common.util;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;

import javax.crypto.Mac;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by 宫洪深 on 2017/2/28.
 * 上传东西到七牛服务器
 */
public class QiNiuUpUtils {
    private static Auth auth;
    static {//利用
        auth = Auth.create (MiscUtils.getConfigByKey("qiniu_AK"), MiscUtils.getConfigByKey("qiniu_SK"));
    }

    /**
     * 青牛服务器 字节上传
     * @param uploadBytes
     * @param userName
     * @return
     */
    public static String uploadByIO( byte[] uploadBytes,String userName){
        String upToken = auth.uploadToken(MiscUtils.getConfigByKey("qnlive_qrcode_image"));
        Configuration cfg = new Configuration(Zone.zone0());
        UploadManager uploadManager = new UploadManager(cfg);
        try {
            Response response = uploadManager.put(uploadBytes,userName,upToken);
            //解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            String url = MiscUtils.getConfigByKey("qnlive_qrcode_image")+putRet.key;
            return url;
        } catch (QiniuException ex) {
            Response r = ex.response;
            System.err.println(r.toString());
            try {
                System.err.println(r.bodyString());
            } catch (QiniuException ex2) {
                //ignore
            }
        }
        return null;
    }

}
