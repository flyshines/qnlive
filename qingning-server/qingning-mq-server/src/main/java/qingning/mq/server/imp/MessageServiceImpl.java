package qingning.mq.server.imp;


import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import qingning.common.util.SendMsgUtil;
import qingning.server.MessageService;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;


/**
 * Created by loovee1 on 2016/4/13.
 */

public class MessageServiceImpl implements MessageService {

    private static final Logger logger = Logger.getLogger(MessageServiceImpl.class);



    
    public boolean sendMessageWithDream(String phone, String content,String appName) {
        String result = SendMsgUtil.sendMsgCode(phone, content,appName);
        logger.info("【梦网】（" + phone + "）发送短信内容（" + content + "）返回结果：" + result);
        if(!"success".equalsIgnoreCase(SendMsgUtil.validateCode(result))){
            return false;
        }
        return true;
    }


    public static void main(String[] args) {
        new MessageServiceImpl().sendMessageWithDream("18676365713","梦网测试成功!","qnlive");
    }


}
