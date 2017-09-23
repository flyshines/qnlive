package qingning.shop.server.controller;


import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.wxEncrypt.WXBizMsgCrypt;
import qingning.server.AbstractController;

@RestController
public class ShopController extends AbstractController {

    private WXBizMsgCrypt cryptUtil = null;


    /**
     * 创建课程
     * @param entity
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/shop/lecturer/courses", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity createCourse(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "createCourse", accessToken, version);
        requestEntity.setParam(entity.getBody());
        return this.process(requestEntity, serviceManger, message);
    }


}
