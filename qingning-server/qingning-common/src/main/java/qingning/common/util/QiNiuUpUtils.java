package qingning.common.util;

import com.google.gson.Gson;
import com.qiniu.cdn.CdnManager;
import com.qiniu.cdn.CdnResult;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by 宫洪深 on 2017/2/28.
 * 上传东西到七牛服务器
 */
public class QiNiuUpUtils {

    private static Auth auth;
    static {//利用
        auth = Auth.create (MiscUtils.getConfigKey("qiniu_AK"), MiscUtils.getConfigKey("qiniu_SK"));
    }

    /**
     * 青牛服务器 字节上传
     * @param uploadBytes 字节
     * @param fileName 文件名
     * @return
     */
    public static String uploadByIO( byte[] uploadBytes, String fileName) throws Exception {
        String upToken = auth.uploadToken(MiscUtils.getConfigKey("image_space"),fileName); //生成上传凭证 覆盖上传
        Configuration cfg = new Configuration(Zone.zone0());//上传链接
        UploadManager uploadManager = new UploadManager(cfg);//生成上传那工具
        Response response = uploadManager.put(uploadBytes,fileName,upToken);//上传类
        DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);//上传
        String url = MiscUtils.getConfigKey("images_space_domain_name")+"/"+putRet.key;//文件地址
//        CdnManager c = new CdnManager(auth);//刷新缓存工具
//        String[] urls = new String[]{url};//要刷新缓存的路径
//        CdnResult.RefreshResult result = c.refreshUrls(urls);//刷新缓存
        return url;//返回url
    }

    public static String uploadImage(String imgUrl, String fileName) throws Exception {
        byte[] resByt = null;
        try {
            URL urlObj = new URL(imgUrl);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            // 连接超时
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setConnectTimeout(25000);
            // 读取超时 --服务器响应比较慢,增大时间
            conn.setReadTimeout(25000);
            conn.setRequestMethod("GET");
            conn.addRequestProperty("Accept-Language", "zh-cn");
            conn.addRequestProperty("Content-type", "image/jpeg");
            conn.addRequestProperty( "User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 2.0.50727)");
            conn.connect();

            BufferedImage bufImg = ImageIO.read(conn.getInputStream());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufImg, "jpg", outputStream);
            resByt = outputStream.toByteArray();
            outputStream.close();
            // 断开连接
            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return uploadByIO(resByt, fileName);
    }

    public static void main(String[] args) throws Exception {

//        String url = "http://mmbiz.qpic.cn/mmbiz_jpg/cxIPufbGFZlHfLtUPfMwibEuhFVLnCRUeia90tJw0zuKfu9Q8dyKZJgGJs6WzZJUsVia9nxqNjo0dLzMTxIzslvuQ/0";

    }


}
