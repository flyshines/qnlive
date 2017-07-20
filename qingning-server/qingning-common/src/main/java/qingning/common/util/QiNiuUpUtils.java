package qingning.common.util;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.processing.OperationManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.qiniu.util.UrlSafeBase64;
import qingning.common.entity.QNLiveException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by 宫洪深 on 2017/2/28.
 * 上传东西到七牛服务器
 */
public class QiNiuUpUtils {

    private static Auth auth;
    private static OperationManager om;

    //转移到新的空间名称
    private static String audioSpace = MiscUtils.getConfigKey("audio_space");
    //转移到新的视频空间名称
    private static String videoSpace = MiscUtils.getConfigKey("video_space");
    //音频空间地址
    private static String autoDomain = MiscUtils.getConfigKey("audio_space_domain_name");
    //视频空间地址
    private static String videoDomain = MiscUtils.getConfigKey("video_space_domain_name");
    //七牛回调地址
    private static String verifyCallback = MiscUtils.getConfigKey("qiniu_verify_callback");
    //七牛处理音视频队列
    private static String pipeline = MiscUtils.getConfigKey("qiniu_pipeline");
    //七牛默认截取时间长度
    private static Long cutTimes = Long.valueOf(MiscUtils.getConfigKey("qiniu_cut_timestamp"));

    static {//利用
        auth = Auth.create(MiscUtils.getConfigKey("qiniu_AK"), MiscUtils.getConfigKey("qiniu_SK"));
        Configuration cfg = new Configuration(Zone.zone0());
        om = new OperationManager(auth,cfg);
    }

    /**
     * 青牛服务器 字节上传
     *
     * @param uploadBytes 字节
     * @param fileName    文件名
     * @return
     */
    public static String uploadByIO(byte[] uploadBytes, String fileName) throws Exception {
        String upToken = auth.uploadToken(MiscUtils.getConfigKey("image_space"), fileName); //生成上传凭证 覆盖上传
        Configuration cfg = new Configuration(Zone.zone0());//上传链接
        UploadManager uploadManager = new UploadManager(cfg);//生成上传那工具
        Response response = uploadManager.put(uploadBytes, fileName, upToken);//上传类
        DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);//上传
        String url = MiscUtils.getConfigKey("images_space_domain_name") + "/" + putRet.key;//文件地址
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
            conn.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 2.0.50727)");
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

    private static byte[] getFileContent(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fin = new FileInputStream(file);
        byte[] buffer = new byte[(int) file.length()];
        int offset = 0;
        int numRead = 0;
        while (offset < buffer.length
                && (numRead = fin.read(buffer, offset, buffer.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset != buffer.length) {
            throw new IOException("读取出错！");
        }
        fin.close();

        return buffer;

    }

    /**
     * 青牛服务器 音频上传
     *
     * @param uploadBytes 字节
     * @param fileName    文件名
     * @return
     */
    public static String uploadAuto(byte[] uploadBytes, String fileName) throws Exception {
        String upToken = auth.uploadToken(MiscUtils.getConfigKey("audio_space"), fileName); //生成上传凭证 覆盖上传
        Configuration cfg = new Configuration(Zone.zone0());//上传链接
        UploadManager uploadManager = new UploadManager(cfg);//生成上传那工具
        Response response = uploadManager.put(uploadBytes, fileName, upToken);//上传类
        DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);//上传
        String url = MiscUtils.getConfigKey("audio_space_domain_name") + "/" + putRet.key;//文件地址

        return url;//
    }


    /**
     * @param url       文件地址
     * @param times     截取时间（秒）
     * @param callBack  是否需要回调
     * @return
     */
    public static String cutAuto(String url,Long times,boolean callBack) throws Exception{
        //默认截取时间
        if(times == null){
            times = cutTimes;
        }
        //文件名称
        String srcKey = url.replace(autoDomain + "/", "");
        String srcNames[] = srcKey.split("\\.");
        String newSrcName;
        if(srcNames.length<2){
            newSrcName = srcKey+"_cut_"+times;
        }else{
            String srcName = srcNames[0]+"_cut_"+times;
            //拼接后的新名称
            newSrcName = srcName+".mp3";
        }
        //转码参数
        String fops = "avthumb/mp3/t/"+times+"|saveas/"+UrlSafeBase64.encodeToString(audioSpace+":"+newSrcName);
        try {
            String persistentId;
            if(callBack){
                persistentId = om.pfop(audioSpace, srcKey,fops,pipeline,verifyCallback);
            }else{
                persistentId = om.pfop(audioSpace, srcKey,fops,pipeline,"");
            }
            return persistentId;
        } catch (QiniuException e) {
            e.printStackTrace();
            throw new QNLiveException("210008");
        }
    }

    /**
     * @param space     空间名称
     * @param domain    空间地址
     * @param url       文件地址
     * @param times     截取时间（秒）
     * @param callBack  是否需要回调
     * @return
     */
    public static String cutVideo(String space,String domain,String url,Long times,boolean callBack) throws Exception{
        //默认截取时间
        if(times == null){
            times = cutTimes;
        }
        //文件名称
        String srcKey = url.replace(domain + "/", "");
        String srcNames[] = srcKey.split("\\.");
        String newSrcName;
        if(srcNames.length<2){
            newSrcName = srcKey+"_cut_"+times;
        }else{
            String srcName = srcNames[0]+"_cut_"+times;
            //拼接后的新名称
            newSrcName = srcName+".mp4";
        }
        //转码参数
        String fops = "avthumb/mp4/t/"+times+"|saveas/"+UrlSafeBase64.encodeToString(space+":"+newSrcName);
        try {
            String persistentId;
            if(callBack){
                persistentId = om.pfop(space, srcKey,fops,pipeline,verifyCallback);
            }else{
                persistentId = om.pfop(space, srcKey,fops,pipeline,"");
            }
            return persistentId;
        } catch (QiniuException e) {
            e.printStackTrace();
            throw new QNLiveException("210008");
        }
    }


    public static void main(String[] args) throws Exception {
        //byte[] file = getFileContent("E:\\transfer.mp3");
        //System.out.println(uploadAuto(file, "transfer.mp3"));
        String newSpace = "qnlive-audio:transfer.wav";
        String audioSpace = "qnlive-audio";//;MiscUtils.getConfigKey("audio_space_domain_name");
        //String url = "http://audio.qnhdlive.tsingning.com/transfer.mp3";
        String url = "http://audio.qnhdlive.tsingning.com/lu2cK_qNF_kUOjaxkcsSsOOIHdGt";
        String srcKey = "lu2cK_qNF_kUOjaxkcsSsOOIHdGt";
        /*String persistentId = om.pfop(audioSpace,srcKey,
                //"avthumb/mp3/m3u8/noDomain/1/segtime/1/A/libmp3lame"
                "avthumb/mp3/|saveas/"+UrlSafeBase64.encodeToString(newSpace)
                ,new StringMap().putNotEmpty("notifyURL", ""));
*/
        String persistentId = QiNiuUpUtils.cutVideo(audioSpace,autoDomain,url,10L,false);
        /*String persistentId = om.pfop(audioSpace, srcKey, "avthumb/mp3/|saveas/"+UrlSafeBase64.encodeToString(newSpace),"","");
        System.out.print(persistentId);*/
    }


}
