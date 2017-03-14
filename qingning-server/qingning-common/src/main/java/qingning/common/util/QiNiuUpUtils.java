package qingning.common.util;

import com.google.gson.Gson;
import com.qiniu.cdn.CdnManager;
import com.qiniu.cdn.CdnResult;
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
    public static String uploadByIO( byte[] uploadBytes,String fileName)throws Exception{
        String upToken = auth.uploadToken(MiscUtils.getConfigByKey("qnlive_qrcode_image"),fileName); //生成上传凭证 覆盖上传
        Configuration cfg = new Configuration(Zone.zone0());//上传链接
        UploadManager uploadManager = new UploadManager(cfg);//生成上传那工具
        Response response = uploadManager.put(uploadBytes,fileName,upToken);//上传类
        DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);//上传
        String url = MiscUtils.getConfigByKey("images_qrcode_domain_name")+"/"+putRet.key;//文件地址
        CdnManager c = new CdnManager(auth);//刷新缓存工具
        String[] urls = new String[]{url};//要刷新缓存的路径
        CdnResult.RefreshResult result = c.refreshUrls(urls);//刷新缓存
        return url;//返回url
    }

}
