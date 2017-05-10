package qingning.server;

/**
 * Created by DavidGHS on 2017/3/21.
 * 发送短信
 * 短息那工具
 */
public interface MessageService {


    /*
     * 发送短信(梦网)
     * phone:手机号码
     * content:短信内容
     */
    public boolean sendMessageWithDream(String phone, String content,String appName);



}
