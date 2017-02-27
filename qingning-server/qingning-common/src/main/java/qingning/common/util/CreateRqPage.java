package qingning.common.util;

import java.awt.image.BufferedImage;

/**
 * Created by DavidGHS on 2017/2/24.
 * 生成二维码分享链接图片
 */
public class CreateRqPage {

    /**
     * 获取二维码图片
     * @param icon_img 展示头像图片
     * @param nick_name 名称
     * @param qr_url 扫描二维码后的跳转路径
     * @throws Exception 
     */
    public static BufferedImage getQrCodeImage(String icon_img, String nick_name, String qr_url) throws Exception{
        return ZXingUtil.createLivePng(icon_img, nick_name, qr_url);
    }

    /**
     * 获取二维码图片
     * @param icon_img 展示头像图片
     * @param nick_name 名称
     * @param trunToUrl 扫描二维码后的跳转路径
     * @param date 毫秒值 时间戳
     * @throws Exception 
     */
    public static BufferedImage getQrCodeImage(String icon_img,String nick_name,String trunToUrl,Long date) throws Exception{
		return	ZXingUtil.createCoursePng(icon_img, nick_name, trunToUrl, date);
    }

}
