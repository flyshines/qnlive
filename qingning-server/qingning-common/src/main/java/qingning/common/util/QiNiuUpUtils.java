package qingning.common.util;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
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
     * @param uploadBytes 字节
     * @param fileName 文件名
     * @return
     */
    public static String uploadByIO( byte[] uploadBytes,String fileName){
        String upToken = auth.uploadToken(MiscUtils.getConfigByKey("qnlive_qrcode_image"));
        Configuration cfg = new Configuration(Zone.zone0());
        UploadManager uploadManager = new UploadManager(cfg);
        try {
            Response response = uploadManager.put(uploadBytes,fileName,upToken);
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            String url = MiscUtils.getConfigByKey("images_qrcode_domain_name")+"/"+putRet.key;
            return url;
        } catch (QiniuException ex) {
            //删除文件
            BucketManager bucketManager = new BucketManager(auth, cfg);
            try {
                bucketManager.delete(MiscUtils.getConfigByKey("qnlive_qrcode_image"), fileName);
                Response response = uploadManager.put(uploadBytes,fileName,upToken);
                DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
                String url = MiscUtils.getConfigByKey("images_qrcode_domain_name")+"/"+putRet.key;
                return url;
            } catch (QiniuException e) {
                //如果遇到异常，说明删除失败
                System.err.println(e.code());
                System.err.println(e.response.toString());
            }

        }
        return null;
    }

}
