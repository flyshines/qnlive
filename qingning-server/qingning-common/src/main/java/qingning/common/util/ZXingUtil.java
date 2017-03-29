package qingning.common.util; 

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.imageio.ImageIO;

import qingning.common.source.BufferedImageLuminanceSource;
import qingning.common.source.MatrixToImageConfig;
import qingning.common.source.MatrixToImageWriter;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/** 
 * ZXing工具类 
 * @see ----------------------------------------------------------------------------------------------------------------------- 
 * @see /首页--https://code.google.com/p/zxing
 * @see/介绍--用于解析多种格式条形码(EAN-13)和二维码(QRCode)的开源Java类库,其提供了多种应用的类库,如javase/jruby/cpp/csharp/android
 * @see /说明--下载到的ZXing-2.2.zip是它的源码,我们在JavaSE中使用时需用到其core和javase两部分
 * @see      /可直接引入它俩的源码到项目中,或将它俩编译为jar再引入,这是我编译好的：http://download.csdn.net/detail/jadyer/6245849
 * @see ----------------------------------------------------------------------------------------------------------------------- 
 * @see /经测试:用微信扫描GBK编码的中文二维码时出现乱码,用UTF-8编码时微信可正常识别
 * @see      / 并且MultiFormatWriter.encode()时若传入hints参数来指定UTF-8编码中文时,微信压根就不识别所生成的二维码
 * @see      / 所以这里使用的是这种方式new String(content.getBytes("UTF-8"), "ISO-8859-1")
 * @see ----------------------------------------------------------------------------------------------------------------------- 
 * @see /将logo图片加入二维码中间时,需注意以下几点
 * @see 1)生成二维码的纠错级别建议采用最高等级H,这样可以增加二维码的正确识别能力(我测试过,不设置级别时,二维码工具无法读取生成的二维码图片) 
 * @see 2)头像大小最好不要超过二维码本身大小的1/5,而且只能放在正中间部位,这是由于二维码本身结构造成的(你就把它理解成图片水印吧) 
 * @see 3)在仿照腾讯微信在二维码四周增加装饰框,那么一定要在装饰框和二维码之间留出白边,这是为了二维码可被识别 
 * @see ----------------------------------------------------------------------------------------------------------------------- 
 * @version v1.0 
 * @history v1.0-->方法新建,目前仅支持二维码的生成和解析,生成二维码时支持添加logo头像 
 * @editor Sep 10, 2013 9:32:23 PM 
 * @create Sep 10, 2013 2:08:16 PM 
 * @author 玄玉<http://blog.csdn.net/jadyer> 
 */  
public class ZXingUtil {  
	
	private static final String CHARSET = "utf-8";  
	
    // 二维码尺寸  
    private static final int QRCODE_SIZE = 300;  
    // LOGO宽度  
    private static final int LOGO_WIDTH = 60;  
    // LOGO高度  
    private static final int LOGO_HEIGHT = 60; 
    
    private static final int WIDTH = 317*3;
    // LOGO高度  
    private static final int HEIGHT = 563*3;
	
    //字体累计增加高度
    private static int font_height = 0;

    //字体
    private static String FONT_NAME = "黑体";
    //img
    private static String IMG_TYPE = "png";

    
    private ZXingUtil(){
    	font_height = 0;
    }  
      
    /** 
     * 为二维码图片增加logo头像 
     * @see /其原理类似于图片加水印
     * @param imagePath 二维码图片存放路径(含文件名) 
     * @param logoPath  logo头像存放路径(含文件名) 
     */  
    private static void overlapImage(String imagePath, String logoPath) throws IOException {  
        BufferedImage image = ImageIO.read(new File(imagePath));  
        int logoWidth = image.getWidth()/5;   //设置logo图片宽度为二维码图片的五分之一  
        int logoHeight = image.getHeight()/5; //设置logo图片高度为二维码图片的五分之一  
        int logoX = (image.getWidth()-logoWidth)/2;   //设置logo图片的位置,这里令其居中  
        int logoY = (image.getHeight()-logoHeight)/2; //设置logo图片的位置,这里令其居中  
        Graphics2D graphics = image.createGraphics();  
        graphics.drawImage(ImageIO.read(new File(logoPath)), logoX, logoY, logoWidth, logoHeight, null);  
        graphics.dispose();  
        ImageIO.write(image, imagePath.substring(imagePath.lastIndexOf(".") + 1), new File(imagePath));  
    }  
  
      
    /** 
     * 生成二维码 
     * @param content   二维码内容 
     * @param charset   编码二维码内容时采用的字符集(传null时默认采用UTF-8编码) 
     * @param imagePath 二维码图片存放路径(含文件名) 
     * @param width     生成的二维码图片宽度 
     * @param height    生成的二维码图片高度 
     * @param logoPath  logo头像存放路径(含文件名,若不加logo则传null即可) 
     * @return 生成二维码结果(true or false) 
     */  
    public static boolean encodeQRCodeImage(String content, String charset, String imagePath, int width, int height, String logoPath) {  
        Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();  
        //指定编码格式  
        //hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");  
        //指定纠错级别(L--7%,M--15%,Q--25%,H--30%)  
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);  
        //编码内容,编码类型(这里指定为二维码),生成图片宽度,生成图片高度,设置参数  
        BitMatrix bitMatrix = null;  
        try {  
            bitMatrix = new MultiFormatWriter().encode(new String(content.getBytes(charset==null?"UTF-8":charset), "ISO-8859-1"), BarcodeFormat.QR_CODE, width, height, hints);  
        } catch (Exception e) {  
            System.out.println("编码待生成二维码图片的文本时发生异常,堆栈轨迹如下");  
            e.printStackTrace();  
            return false;  
        }  
        //生成的二维码图片默认背景为白色,前景为黑色,但是在加入logo图像后会导致logo也变为黑白色,至于是什么原因还没有仔细去读它的源码  
        //所以这里对其第一个参数黑色将ZXing默认的前景色0xFF000000稍微改了一下0xFF000001,最终效果也是白色背景黑色前景的二维码,且logo颜色保持原有不变  
        MatrixToImageConfig config = new MatrixToImageConfig(0xFF000001, 0xFFFFFFFF);  
        //这里要显式指定MatrixToImageConfig,否则还会按照默认处理将logo图像也变为黑白色(如果打算加logo的话,反之则不须传MatrixToImageConfig参数)  
        try {  
            MatrixToImageWriter.writeToFile(bitMatrix, imagePath.substring(imagePath.lastIndexOf(".") + 1), new File(imagePath), config);  
        } catch (IOException e) {  
            System.out.println("生成二维码图片[" + imagePath + "]时遇到异常,堆栈轨迹如下");  
            e.printStackTrace();  
            return false;  
        }  
        //此时二维码图片已经生成了,只不过没有logo头像,所以接下来根据传入的logoPath参数来决定是否加logo头像  
        if(null == logoPath){  
            return true;  
        }else{  
            //如果此时最终生成的二维码不是我们想要的,那么可以扩展MatrixToImageConfig类(反正ZXing提供了源码)  
            //扩展时可以重写其writeToFile方法,令其返回toBufferedImage()方法所生成的BufferedImage对象(尽管这种做法未必能解决为题,故需根据实际情景测试)  
            //然后替换这里overlapImage()里面的第一行BufferedImage image = ImageIO.read(new File(imagePath));  
            //即private static void overlapImage(BufferedImage image, String imagePath, String logoPath)  
            try {  
                //这里不需要判断logoPath是否指向了一个具体的文件,因为这种情景下overlapImage会抛IO异常  
                overlapImage(imagePath, logoPath);  
                return true;  
            } catch (IOException e) {  
                System.out.println("为二维码图片[" + imagePath + "]添加logo头像[" + logoPath + "]时遇到异常,堆栈轨迹如下");  
                e.printStackTrace();  
                return false;  
            }  
        }  
    }  
      
      
    /** 
     * 解析二维码 
     * @param imagePath 二维码图片存放路径(含文件名) 
     * @param charset   解码二维码内容时采用的字符集(传null时默认采用UTF-8编码) 
     * @return 解析成功后返回二维码文本,否则返回空字符串 
     */  
    public static String decodeQRCodeImage(String imagePath, String charset) {  
        BufferedImage image = null;  
        try {  
            image = ImageIO.read(new File(imagePath));  
        } catch (IOException e) {  
            e.printStackTrace();  
            return "";  
        }  
        if(null == image){  
            System.out.println("Could not decode QRCodeImage");  
            return "";  
        }  
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));  
        Map<DecodeHintType, String> hints = new HashMap<DecodeHintType, String>();  
        hints.put(DecodeHintType.CHARACTER_SET, charset==null ? "UTF-8" : charset);  
        com.google.zxing.Result result = null;  
        try {  
            result = new MultiFormatReader().decode(bitmap, hints);  
            return result.getText();  
        } catch (NotFoundException e) {  
            System.out.println("二维码图片[" + imagePath + "]解析失败,堆栈轨迹如下");  
            e.printStackTrace(); 
            return "";  
            
        }  
    }  
    
    /**
     * 给图片添加文字水印
     * 
     * @param pressText 水印文字
     * @param srcImageFile 源图像流
     * @param fontName  水印的字体名称
     * @param fontStyle 水印的字体样式
     * @param color   水印的字体颜色
     * @param fontSize 水印的字体大小
     * @param x 修正值
     * @param y   修正值
     * @param alpha 透明度：alpha 必须是范围 [0.0, 1.0] 之内（包含边界值）的一个浮点数字
     * @param flag 是否累计换回高度   默认false 不增加换行
     * @return
     */
    public final static BufferedImage pressText(String pressText, BufferedImage srcImageFile, String fontName, int fontStyle,
            int fontSize, Color color, int x, int y, float alpha, Boolean flag, Boolean center)
    {
        try
        {
        	Image src =	srcImageFile;
            int width = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, width, height, null);
            g.setColor(color);

            g.setFont(new Font(fontName, fontStyle, fontSize));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,alpha));
            
            int font_width = WIDTH+200;
            String lengthString = getLengthString(pressText,fontSize,font_width);
            if (lengthString.indexOf(",")>0) {
            	String[] split = lengthString.split(",");
            	if (split!=null && split.length>0) {
            		
					for (int i = 0; i < split.length; i++) {
						if (i == 0 ) {
							font_height = 0;
						}
						g.drawString(split[i], (width - (getLength(split[i]) * fontSize)) / 2 + x, (height - fontSize) / 2 + y+font_height);
						font_height+=fontSize;
					}
				}
			}else{
				// 在指定坐标绘制水印文字
                if (center) {
                    if (flag) {
                        g.drawString(pressText, (width - (getLength(pressText) * fontSize)) / 2 + x, (height - fontSize) / 2 + y+font_height);
                    } else {
                        g.drawString(pressText, (width - (getLength(pressText) * fontSize)) / 2 + x, (height - fontSize) / 2 + y);
                    }
                } else {
                    if (flag) {
                        g.drawString(pressText, x, y+font_height);
                    } else {
                        g.drawString(pressText, x, y);
                    }
                }

			}
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.dispose();
            //生成图片
//            ImageIO.write((BufferedImage) image, formatType, new File(destImageFile));// 输出到文件流
            return (BufferedImage) image;
           
        } catch (Exception e)
        {
            e.printStackTrace();
        }
		return null;
    }
    
    
    /**
     * 给图片添加文字水印
     * 
     * @param pressText 水印文字
     * @param srcImageFile 源图像流
     * @param fontName  水印的字体名称
     * @param fontStyle 水印的字体样式
     * @param color   水印的字体颜色
     * @param fontSize 水印的字体大小
     * @param x 修正值
     * @param y   修正值
     * @param alpha 透明度：alpha 必须是范围 [0.0, 1.0] 之内（包含边界值）的一个浮点数字
     * @param formatType  目标格式
     * @param fontSize 是否累计换回高度   默认false 不增加换行
     * @return
     */
//    public final static BufferedImage pressText(String pressText,BufferedImage  srcImageFile, String fontName, int fontStyle, Color color,
//            int fontSize, int x, int y, float alpha,String formatType)
//    {
//        try
//        {
//        	Image src =	srcImageFile;
//            int width = src.getWidth(null);
//            int height = src.getHeight(null);
//            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//            Graphics2D g = image.createGraphics();
//            g.drawImage(src, 0, 0, width, height, null);
//            g.setColor(color);
//            g.setFont(new Font(fontName, fontStyle, fontSize));
//            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,alpha));
//			g.drawString(pressText, (width - (getLength(pressText) * fontSize)) / 2 + x, (height - fontSize) / 2 + y);
//            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//            g.dispose();
//            return (BufferedImage) image;
//        } catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//		return null;
//    }
    /**
     * 给图片添加图片水印
     * 
     * @param pressImg
     *            水印图片
     * @param srcImageFile
     *            源图像地址
     * @param destImageFile
     *            目标图像地址
     * @param x
     *            修正值。 默认在中间
     * @param y
     *            修正值。 默认在中间
     * @param alpha
     *            透明度：alpha 必须是范围 [0.0, 1.0] 之内（包含边界值）的一个浮点数字
     * @param formatType
     *              目标格式
     */ 
    public final static void pressImage(String pressImg, String srcImageFile,
            String destImageFile, int x, int y, float alpha,String formatType)
    {
        try
        {
            File img = new File(srcImageFile);
            Image src = ImageIO.read(img);
            int wideth = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(wideth, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, wideth, height, null);
            // 水印文件
            Image src_biao = ImageIO.read(new File(pressImg));
            int wideth_biao = src_biao.getWidth(null);
            int height_biao = src_biao.getHeight(null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,alpha));
            g.drawImage(src_biao, (wideth - wideth_biao) / 2,(height - height_biao) / 2, wideth_biao, height_biao, null);
            // 水印文件结束
            g.dispose();
            ImageIO.write((BufferedImage) image, formatType,
                    new File(destImageFile));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    /**
     * 计算text的长度（一个中文算两个字符）
     * 
     * @param text
     * @return
     */
    public final static int getLength(String text)
    {
        int length = 0;
        for (int i = 0; i < text.length(); i++)
        {
            if (new String(text.charAt(i) + "").getBytes().length > 1)
            {
                length += 2;
            } else
            {
                length += 1;
            }
        }
        return length / 2;
    }
    
    /**
     * @param text  截断字符串
     * @param Length  多长截取  
     * @return
     */
    public final static String getLengthString(String text, int fontSize,int Length)
    {
        int length = 0;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < text.length(); i++)
        {
			String sbText = new String(text.charAt(i) + "");
			if (length >= Length) {
				sb.append(sbText + ",");
				length = 0;
			} else {
				sb.append(sbText);
			}

			if (sbText.getBytes().length > 1) {
				length += 2 * fontSize;
			} else {
				length += 1 * fontSize;
			}
		}
        return sb.toString();
    }
    
	/**
	 * 图片水印
	 * 
	 * @param inputImage  待处理图像
	 * @param markImage  水印图像
	 * @param x  水印位于图片左上角的 x 坐标值
	 * @param y  水印位于图片左上角的 y 坐标值
	 * @param alpha  水印透明度 0.1f ~ 1.0f
	 * */
    public static BufferedImage waterMark(BufferedImage inputImage,BufferedImage markImage, int x, int y, float alpha) {
        BufferedImage image = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.drawImage(inputImage, 0, 0, null);
        // 加载水印图像
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,alpha));
        g.drawImage(markImage, x, y, null);
        g.dispose();
        return image;
    }
    
    /**
     * 文字水印
     * 
     * @param inputImage
     *            待处理图像
     * @param text
     *            水印文字
     * @param font
     *            水印字体信息
     * @param color
     *            水印字体颜色
     * @param x
     *            水印位于图片左上角的 x 坐标值
     * @param y
     *            水印位于图片左上角的 y 坐标值
     * @param alpha
     *            水印透明度 0.1f ~ 1.0f
     */
    public static BufferedImage textMark(BufferedImage inputImage, String text, Font font,Color color, int x, int y, float alpha) {
        Font dfont = (font == null) ? new Font("宋体", 20, 13) : font;
        BufferedImage image = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.drawImage(inputImage, 0, 0, null);
        g.setColor(color);
        g.setFont(dfont);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        g.drawString(text, x, y);
        g.dispose();
        return image;
    }  
    
    /** 
     * 插入LOGO  
     * @param source  二维码图片 
     * @param logoPath   LOGO图片地址 
     * @param needCompress    是否压缩 
     * @throws Exception 
     */  
    private static void insertImage(BufferedImage source, String logoPath, boolean needCompress) throws Exception {  
        File file = new File(logoPath);  
        if (!file.exists()) {  
            throw new Exception("logo file not found.");  
        }  
        Image src = ImageIO.read(new File(logoPath));  
        int width = src.getWidth(null);  
        int height = src.getHeight(null);  
        if (needCompress) { // 压缩LOGO  
            if (width > LOGO_WIDTH) {  
                width = LOGO_WIDTH;  
            }  
            if (height > LOGO_HEIGHT) {  
                height = LOGO_HEIGHT;  
            }  
            Image image = src.getScaledInstance(width, height, Image.SCALE_SMOOTH);  
            BufferedImage tag = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);  
            Graphics g = tag.getGraphics();  
            g.drawImage(image, 0, 0, null); // 绘制缩小后的图  
            g.dispose();  
            src = image;  
        }  
        // 插入LOGO  
        Graphics2D graph = source.createGraphics();  
        int x = (QRCODE_SIZE - width) / 2;  
        int y = (QRCODE_SIZE - height) / 2;  
        graph.drawImage(src, x, y, width, height, null);  
        Shape shape = new RoundRectangle2D.Float(x, y, width, width, 6, 6);  
        graph.setStroke(new BasicStroke(3f));  
        graph.draw(shape);  
        graph.dispose();  
    }  
    
    /**
     * 创建二维码 
     * @param content	二维码的内容
     * @param logoPath  镶嵌在二维码中间的图片地址
     * @param size		二维码大小
     * @param needCompress  是否压缩图片
     * @return
     * @throws Exception
     */
    public static BufferedImage createImage(String content, String logoPath,int size, boolean needCompress) throws Exception {  
        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();  
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hints.put(EncodeHintType.CHARACTER_SET, CHARSET);  
        hints.put(EncodeHintType.MARGIN, 1);  
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size,hints);  
        int width = bitMatrix.getWidth();  
        int height = bitMatrix.getHeight();  
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);  
        for (int x = 0; x < width; x++) {  
            for (int y = 0; y < height; y++) {  
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);  
            }  
        }  
        if (logoPath == null || "".equals(logoPath)) {  
            return image;  
        }  
        // 插入图片  
         insertImage(image, logoPath, needCompress);  
        return image;  
    } 
    
    
    /** 
     * 生成二维码 
     * @param content   二维码内容 
     * @param charset   编码二维码内容时采用的字符集(传null时默认采用UTF-8编码) 
     * @param imagePath 二维码图片存放路径(含文件名) 
     * @param width     生成的二维码图片宽度 
     * @param height    生成的二维码图片高度 
     * @param logoPath  logo头像存放路径(含文件名,若不加logo则传null即可) 
     * @return 生成二维码结果(true or false) 
     */  
    public static BufferedImage createImage(String content, String charset, String imagePath, int width, int height, String logoPath) {  
        Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();  
        //指定编码格式  
        //hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");  
        //指定纠错级别(L--7%,M--15%,Q--25%,H--30%)  
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        //编码内容,编码类型(这里指定为二维码),生成图片宽度,生成图片高度,设置参数  
        BitMatrix bitMatrix = null;  
        try {  
            bitMatrix = new MultiFormatWriter().encode(new String(content.getBytes(charset==null?"UTF-8":charset), "ISO-8859-1"), BarcodeFormat.QR_CODE, width, height, hints);  
        } catch (Exception e) {  
            System.out.println("编码待生成二维码图片的文本时发生异常,堆栈轨迹如下");  
            e.printStackTrace();  
        }  
        //生成的二维码图片默认背景为白色,前景为黑色,但是在加入logo图像后会导致logo也变为黑白色,至于是什么原因还没有仔细去读它的源码  
        //所以这里对其第一个参数黑色将ZXing默认的前景色0xFF000000稍微改了一下0xFF000001,最终效果也是白色背景黑色前景的二维码,且logo颜色保持原有不变  
        MatrixToImageConfig config = new MatrixToImageConfig(0xFF000001, 0xFFFFFFFF);  
        //这里要显式指定MatrixToImageConfig,否则还会按照默认处理将logo图像也变为黑白色(如果打算加logo的话,反之则不须传MatrixToImageConfig参数)  
        try {  	
            MatrixToImageWriter.writeToFile(bitMatrix, imagePath.substring(imagePath.lastIndexOf(".") + 1), new File(imagePath), config);  
        } catch (IOException e) {  
            System.out.println("生成二维码图片[" + imagePath + "]时遇到异常,堆栈轨迹如下");  
            e.printStackTrace();  
        }  
        //此时二维码图片已经生成了,只不过没有logo头像,所以接下来根据传入的logoPath参数来决定是否加logo头像  
        if(null == logoPath){  
        }else{  
            //如果此时最终生成的二维码不是我们想要的,那么可以扩展MatrixToImageConfig类(反正ZXing提供了源码)  
            //扩展时可以重写其writeToFile方法,令其返回toBufferedImage()方法所生成的BufferedImage对象(尽管这种做法未必能解决为题,故需根据实际情景测试)  
            //然后替换这里overlapImage()里面的第一行BufferedImage image = ImageIO.read(new File(imagePath));  
            //即private static void overlapImage(BufferedImage image, String imagePath, String logoPath)  
            try {  
                //这里不需要判断logoPath是否指向了一个具体的文件,因为这种情景下overlapImage会抛IO异常  
            	  BufferedImage image = ImageIO.read(new File(imagePath));  
                  int logoWidth = image.getWidth()/5;   //设置logo图片宽度为二维码图片的五分之一  
                  int logoHeight = image.getHeight()/5; //设置logo图片高度为二维码图片的五分之一  
                  int logoX = (image.getWidth()-logoWidth)/2;   //设置logo图片的位置,这里令其居中  
                  int logoY = (image.getHeight()-logoHeight)/2; //设置logo图片的位置,这里令其居中  
                  Graphics2D graphics = image.createGraphics();  
                  graphics.drawImage(ImageIO.read(new File(logoPath)), logoX, logoY, logoWidth, logoHeight, null);  
                  graphics.dispose();  
                  return image;  
//                  ImageIO.write(image, imagePath.substring(imagePath.lastIndexOf(".") + 1), new File(imagePath)); 
            } catch (IOException e) {  
                System.out.println("为二维码图片[" + imagePath + "]添加logo头像[" + logoPath + "]时遇到异常,堆栈轨迹如下");  
                e.printStackTrace();  
            }  
        }
		return null;  
    }  
    
    /**
     * 缩放Image，此方法返回源图像按给定宽度、高度限制下缩放后的图像
     * @param inputImage
     * @param newWidth：压缩后宽度
     * @param newHeight：压缩后高度
     * @throws java.io.IOException
     * return 
     */
    public static BufferedImage scaleByPercentage(BufferedImage inputImage, int newWidth, int newHeight) throws Exception {
        //获取原始图像透明度类型
        int type = inputImage.getColorModel().getTransparency();
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        //开启抗锯齿
        RenderingHints renderingHints=new RenderingHints(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        //使用高质量压缩
        renderingHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        BufferedImage img = new BufferedImage(newWidth, newHeight, type);
        Graphics2D graphics2d =img.createGraphics();
        graphics2d.setRenderingHints(renderingHints);        
        graphics2d.drawImage(inputImage, 0, 0, newWidth, newHeight, 0, 0, width, height, null);
        graphics2d.dispose();
        return img;
    }

    /**
     * 直播间 分享
     * @param user_head_portrait 头像
     * @param userName 分享者 姓名
     * @param lecturer_name 老师名称
     * @param qr_code_content 二维码分享链接
     * @return
     * @throws Exception
     */
    public static BufferedImage createLivePng(String user_head_portrait,String userName,String lecturer_name,String qr_code_content) throws Exception{
        if(userName.length()>9){
            userName = userName.substring(0,9)+"...";
        }
        if(lecturer_name.length()>9){
            lecturer_name = lecturer_name.substring(0,9)+"...";
        }
        //花图框
        BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        //镶嵌背景图  七牛云地址  不需要变
        bi = convertBG(bi,MiscUtils.getConfigByKey("back_ground_url"));
        //二维码的长宽
        int qr_code_size= 70*3;
        //用户头像的长宽
        int head_portrait_size= 36*3;

        //二维码
        BufferedImage qrImage = createImage(qr_code_content, "", qr_code_size, false);

        //合成二维码后的图片
        BufferedImage waterMark = waterMark(bi, qrImage, 123*3, HEIGHT-44*3-90*3, 1.0f);
        //长按图片识别二维码进入直播间
        BufferedImage pressText1 = pressText("长按识别二维码进入直播间", waterMark, FONT_NAME, 1, 36, Color.white, 0,260*3, 1.0f, false, true);

        //
        BufferedImage pressText2 = pressText(userName, pressText1, FONT_NAME, 1,42, new Color(13,13,13), 363, (36+14)*3, 1.0f, false, false);
        //
        BufferedImage pressText3 = pressText("推荐一个不错的直播间", pressText2, FONT_NAME, 1, 36, new Color(167,167,167), 363, (36+14+18)*3, 1.0f, false, false);
        //
        BufferedImage pressText4 = pressText(lecturer_name+"的直播间", pressText3, FONT_NAME, 1, 54, new Color(90,210,161), 0, -HEIGHT/4+15, 1.0f, true, true);

        //用户头像
        BufferedImage url = getUrl(user_head_portrait);
        BufferedImage convertImage= scaleByPercentage(url, head_portrait_size,head_portrait_size);
        convertImage = convertCircular(convertImage);
//        int height= (int) (HEIGHT*0.03);
        BufferedImage waterMark2 = waterMark(pressText4, convertImage, 77*3, 36*3, 1.0f);

        return waterMark2;
    }

    /**
     * 生成 分销  图
     * @return
     * @throws Exception
     */
    public static BufferedImage createRoomDistributerPng(String user_head_portrait,String userName,String qr_code_content,Double profit_share_rate) throws Exception{
        if(userName.length()>9){
            userName = userName.substring(0,8)+"...";
        }
        BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        //镶嵌背景图  七牛云地址
        bi = convertBG(bi,MiscUtils.getConfigByKey("back_ground_url"));
        //二维码的长宽
        int qr_code_size= 70*3;
        //用户头像的长宽
        int head_portrait_size= 36*3;

        //二维码
        BufferedImage qrImage = createImage(qr_code_content, "", qr_code_size, false);

        //合成二维码后的图片
        BufferedImage waterMark = waterMark(bi, qrImage, 123*3, HEIGHT-44*3-90*3, 1.0f);
        //长按图片识别二维码进入直播间
        BufferedImage pressText1 = pressText("长按识别二维码进入即可成为直播间分销员", waterMark, FONT_NAME, 1, 36, Color.white, 0,260*3, 1.0f, false, true);

        //
        BufferedImage pressText2 = pressText(userName, pressText1, FONT_NAME, 1,42, new Color(13,13,13), 363, 108+42, 1.0f, false, false);
        //
        BufferedImage pressText3 = pressText("直播间分销员邀请", pressText2, FONT_NAME, 1, 36, new Color(167,167,167), 363, 108+42+54, 1.0f, false, false);
        //
        BufferedImage pressText4 = pressText(userName+"的直播间", pressText3, FONT_NAME, 1, 54, new Color(90,210,161), 0, -HEIGHT/4-60-54, 1.0f, true, true);
        //
        BufferedImage pressText5 = pressText("成功推荐用户即可获得"+profit_share_rate+"%的提成", pressText4, FONT_NAME, 1, 48, new Color(131,131,131), 0, -HEIGHT/4+60, 1.0f, true,false);

        //用户头像
        BufferedImage url = getUrl(user_head_portrait);
        BufferedImage  convertImage= scaleByPercentage(url, head_portrait_size,head_portrait_size);
        convertImage = convertCircular(convertImage);
//        int height= (int) (HEIGHT*0.03);
        BufferedImage waterMark2 = waterMark(pressText5, convertImage, 77*3, 108, 1.0f);

        return waterMark2;
    }


    /**
     * 分享课程
     * @param user_head_portrait 分享者 用户头像
     * @param userName  分享者名称
     * @param course_name 课程名称
     * @param qr_code_content 二维码链接
     * @param time 时间
     * @return 分会图片流
     * @throws Exception
     */
    public static BufferedImage createCoursePng(String user_head_portrait,String userName,String course_name,String qr_code_content,Long time) throws Exception{
        if(userName.length()>9){
            userName = userName.substring(0,8)+"...";
        }
        if(course_name.length()>9){
            course_name = course_name.substring(0,8)+"...";
        }

        BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        //镶嵌背景图  七牛云地址
        bi = convertBG(bi,MiscUtils.getConfigByKey("back_ground_url"));
        //二维码的长宽
        int qr_code_size= 70*3;
        //用户头像的长宽
        int head_portrait_size= 36*3;
        //二维码
        //二维码
        BufferedImage qrImage = createImage(qr_code_content, "", qr_code_size, false);

        //合成二维码后的图片
        BufferedImage waterMark = waterMark(bi, qrImage, 123*3, HEIGHT-44*3-90*3, 1.0f);
        //长按图片识别二维码进入直播间
        BufferedImage pressText1 = pressText("长按识别二维码进入直播间", waterMark, FONT_NAME, 1, 36, Color.white, 0,260*3, 1.0f, false, true);

        //
        BufferedImage pressText2 = pressText(userName, pressText1, FONT_NAME, 1,42, new Color(13,13,13), 363, 108+42, 1.0f, false, false);
        //
        BufferedImage pressText3 = pressText("推荐一个不错的课程", pressText2, FONT_NAME, 1, 36, new Color(167,167,167), 363, 108+42+54, 1.0f, false, false);
        //
        BufferedImage pressText4 = pressText(course_name, pressText3, FONT_NAME, 1, 54, new Color(90,210,161), 0, -HEIGHT/4-54, 1.0f, true, true);

        String format = new SimpleDateFormat("yyyy年MM月dd日 HH:MM").format(new Date(time));

        BufferedImage pressText5 = pressText("直播时间:"+format, pressText4, FONT_NAME, 1, 48, new Color(131,131,131), 0, -HEIGHT/4+60, 1.0f, false,false);

        //用户头像
        BufferedImage url = getUrl(user_head_portrait);
        BufferedImage  convertImage= scaleByPercentage(url, head_portrait_size,head_portrait_size);
        convertImage = convertCircular(convertImage);
//        int height= (int) (HEIGHT*0.03);
        BufferedImage waterMark2 = waterMark(pressText5, convertImage, 77*3, 108, 1.0f);
    	return waterMark2;
    }

     
    
    
    /**
     * 通过网络获取图片
     * @param url
     * @return
     */
    public static BufferedImage getUrl(String url){
        try {
			URL urlObj = new URL(url);  
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
            conn.disconnect();  
            return bufImg;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
        return null;
    	
    }
    
    /** 
     * get方法提交 
     *  
     * @param url 
     *            String 访问的URL 
     * @param /param
     *            String 提交的内容 
     * @param repType  返回类型 
     * @return String 
     * */  
    public static byte[] getRequest(String url, String repType) {  
        String result = "";  
        byte[] resByt = null;  
        try {  
            URL urlObj = new URL(url);
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
  
            PrintWriter out = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"), true);  
            if ("image/jpeg".equals(repType)) {  
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();  
                BufferedImage bufImg = ImageIO.read(conn.getInputStream());  
                
                ImageIO.write(bufImg, "jpg", outputStream);  
                resByt = outputStream.toByteArray();  
                outputStream.close();  
            } else {  
                // 取得输入流，并使用Reader读取  
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));  
                System.out.println("=============================");  
                System.out.println("Contents of get request");  
                System.out.println("=============================");  
                String lines = null;  
                while ((lines = reader.readLine()) != null) {  
                    System.out.println(lines);  
                    result += lines;  
                    result += "\r";  
                }  
                resByt = result.getBytes();  
                reader.close();  
            }  
            out.print(resByt);  
            out.flush();  
            out.close();  
            // 断开连接  
            conn.disconnect();  
            System.out.println("=============================");  
            System.out.println("Contents of get request ends");  
            System.out.println("=============================");  
        } catch (MalformedURLException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
  
        return resByt;  
    }  
    
    
    /**
     * 将图像转换为 圆形
     * @param  /url 用户头像地址
     * @return
     * @throws IOException
     */
    public static BufferedImage convertCircular(BufferedImage bi1) throws IOException{
//    	BufferedImage bi1 = ImageIO.read(new File(url));
    	//这种是黑色底的
//    	BufferedImage bi2 = new BufferedImage(bi1.getWidth(),bi1.getHeight(),BufferedImage.TYPE_INT_RGB); 
    	//透明底的图片
    	BufferedImage bi2 = new BufferedImage(bi1.getWidth(),bi1.getHeight(),BufferedImage.TYPE_4BYTE_ABGR); 
    	Ellipse2D.Double shape = new Ellipse2D.Double(0,0,bi1.getWidth(),bi1.getHeight());
    	Graphics2D g2 = bi2.createGraphics();
    	g2.setClip(shape); 
    	// 使用 setRenderingHint 设置抗锯齿
    	g2.drawImage(bi1,0,0,null); 
    	//设置颜色
    	g2.setBackground(Color.green);
    	g2.dispose();
//    	ImageIO.write(bi2, "png", new File("C:/Users/Administrator/Desktop/61.png")); 
    	return bi2;
    }
    
    
	/**
	 * 合成背景图
	 * @param bi 对应的文件
	 * @param backGroundUrl 背景图片路径
	 * @return
	 * @throws IOException
	 */
	private static BufferedImage convertBG(BufferedImage bi ,String backGroundUrl) throws IOException {
        Graphics2D g = bi.createGraphics();
        // 水印 背景 文件 
//      Image src_biao = ImageIO.read(new File(backGroundUrl));
        Image src_biao = getUrl(backGroundUrl);
        g.drawImage(src_biao, 0, 0, WIDTH, HEIGHT, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,1.0f));
        // 水印文件结束
        g.dispose();
        // 合成水印 的图片
        return waterMark(bi, (BufferedImage) bi, 0, 0, 1.0f);
        
	}
	
	
	
	/**
	 * 真实图片转换为  文件流
	 * @param filePath
	 * @return
	 */
	@SuppressWarnings("unused")
	private static BufferedImage convertImage(String filePath) {
        // 水印 背景 文件 
		try {
			Image src_biao = ImageIO.read(new File(filePath));
			return (BufferedImage) src_biao;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
    
	
	
	public static void main(String[] args) throws Exception{
//字体
//        GraphicsEnvironment eq = GraphicsEnvironment.getLocalGraphicsEnvironment();
//        String[] fontNames = eq.getAvailableFontFamilyNames();
//        for(String fontName : fontNames){
//            System.out.println(fontName);
//        }
//
//		//生成二维码
//		 encodeQRCodeImage("http://www.baidu.com", null, "C:/Users/Administrator/Desktop/myQRCodeImage.png", 300, 300,"C:/Users/Administrator/Desktop/5.png");
//		 System.out.println(decodeQRCodeImage("C:/Users/Administrator/Desktop/myQRCodeImage.png",  null));// TODO

		try {
		 	//通过网络  用户头像
			String user_head_portrait="http://120.24.78.189:9090/app-server-file/pic/read_image?name=000093_1479899539822.jpg&proto=1";
	    	//用户名称
	    	String userName= "谷子和姜";
	    	//二维码内容
	    	String qr_code_content="www.baidu.com";
	    	long time = 1488160472302l;
//	    	BufferedImage createRoomDistributerPng = createRoomDistributerPng(user_head_portrait, userName, qr_code_content, 2.0);
//            BufferedImage createCoursePng = createCoursePng(user_head_portrait, userName,"我的课程名字", qr_code_content, time);
            BufferedImage createLivePng = createLivePng(user_head_portrait, userName,"老师名字", qr_code_content);
	    	  //生成的图片位置
//	    	String imagePath1= "C:/Users/Administrator/Desktop/RoomDistributerPng1.png";
//            String imagePath2= "C:/Users/Administrator/Desktop/CoursePng.png";
            String imagePath3= "C:/Users/Administrator/Desktop/LivePng3.png";
//	        ImageIO.write(createRoomDistributerPng, imagePath1.substring(imagePath1.lastIndexOf(".") + 1), new File(imagePath1));
//            ImageIO.write(createCoursePng, imagePath2.substring(imagePath2.lastIndexOf(".") + 1), new File(imagePath2));
            ImageIO.write(createLivePng, imagePath3.substring(imagePath3.lastIndexOf(".") + 1), new File(imagePath3));

//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            ImageIO.write(createRoomDistributerPng, "png", baos);
//            byte[] bytes = baos.toByteArray();
//            QiNiuUpUtils.uploadByIO(bytes,userName);
//            BASE64Encoder encoder = new BASE64Encoder();
//            BASE64Decoder decoder = new BASE64Decoder();
//            System.out.println(encoder.encodeBuffer(bytes));
//            byte[] b = decoder.decodeBuffer( encoder.encodeBuffer(bytes));
//            for (int i = 0; i < b.length; ++i) {
//                if (b[i] < 0) {// 调整异常数据
//                    b[i] += 256;
//                }
//            }// 生成jpeg图片
//            String imgFilePath = "C:/Users/Administrator/Desktop/test22.png";//新生成的图片
//            OutputStream out = new FileOutputStream(imgFilePath);
//            out.write(b);
//            out.flush();
//            out.close();
            System.out.println("ok");
		} catch (Exception e) {
			e.printStackTrace();
		}

    }
}  

