package qingning.shop.server.controller;


import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.common.util.wxEncrypt.WXBizMsgCrypt;
import qingning.server.AbstractController;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ShopController extends AbstractController {

    private WXBizMsgCrypt cryptUtil = null;



    /**
     * 开通店铺
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/open", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity openShop(@RequestHeader(value = "access_token") String accessToken,
                            @RequestHeader(value = "version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopOpen", accessToken, version);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * 微信扫码 获取扫码链接
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/wechat/login/rqcode", method = RequestMethod.GET)
    public void wechatLogin(HttpServletResponse resp) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "wechatLogin", null, null);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

        Object redirectUrl = resultMap.get("redirectUrl");
        if (redirectUrl != null) {
            resp.sendRedirect(redirectUrl.toString());
        }
    }

    /**
     * 店铺-店铺信息
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/info", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity shopInfo(@RequestHeader(value = "access_token") String accessToken,
                            @RequestHeader(value = "version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopInfo", accessToken, version);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * 店铺-店铺设置
     *
     * @param entity
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/edit", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity shopEdit(
            HttpEntity<Object> entity,
            @RequestHeader(value = "access_token") String accessToken,
            @RequestHeader(value = "version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopEdit", accessToken, version);
        if(((Map)entity.getBody()).isEmpty()){
            throw new QNLiveException("000100");
        }
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * PC_店铺-轮播图列表
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity shopBannerList(
            @RequestHeader("access_token") String accessToken,
            @RequestParam(value = "page_size", defaultValue = "10") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopBannerList", accessToken, version);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("page_count", pageSize);
        paramCode.put("page_num", pageNum);
        requestEntity.setParam(paramCode);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * PC_店铺-添加轮播图
     *
     * @param entity
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/add", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity shopBannerAdd(
            HttpEntity<Object> entity,
            @RequestHeader(value = "access_token") String accessToken,
            @RequestHeader(value = "version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopBannerAdd", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * PC_店铺-轮播图编辑
     *
     * @param accessToken
     * @param entity
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/{banner_id}/edit", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity shopBannerEdit(
            HttpEntity<Object> entity,
            @PathVariable("banner_id") String bannerId,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopBannerEdit", accessToken, version);
        Map<String,String> param = (Map)entity.getBody();
        param.put("banner_id",bannerId);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * PC_店铺-轮播图详情
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/{banner_id}/info", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity shopBannerInfo(
            @PathVariable(value = "banner_id") String bannerId,
            @RequestHeader(value = "access_token") String accessToken,
            @RequestHeader(value = "version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopBannerInfo", accessToken, version);
        Map<String,String> param = new HashMap<>();
        param.put("banner_id",bannerId);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * PC_店铺-删除店铺轮播图
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/remove", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity bannerRemove(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "bannerRemove", accessToken, version);
        requestEntity.setParam(entity.getBody());
        return this.process(requestEntity, serviceManger, message);
    }

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

    /**
     * 编辑课程
     * @param entity
     * @param course_id
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/shop/lecturer/courses/{course_id}", method = RequestMethod.PUT)
    public
    @ResponseBody ResponseEntity updateCourse(
            HttpEntity<Object> entity,
            @PathVariable("course_id") String course_id,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "updateCourse", accessToken, version);
        ((Map<String,Object>)entity.getBody()).put("course_id", course_id);
        requestEntity.setParam(entity.getBody());
        return this.process(requestEntity, serviceManger, message);
    }
  /**
     * PC_店铺-轮播图上下架
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/{banner_id}/updown/{type}", method = RequestMethod.PUT)
    public @ResponseBody ResponseEntity shopBannerUpdown(
            @PathVariable("banner_id") String bannerId,
            @PathVariable("type") String type,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopBannerUpdown", accessToken, version);
        Map<String,String> param = new HashMap<>();
        param.put("banner_id",bannerId);
        param.put("type",type);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * PC_店铺-添加课程（音频，视频，图文）
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/single/add", method = RequestMethod.POST)
    public  @ResponseBody ResponseEntity addShopSingleVideo(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "addSingleGoods", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * PC_店铺-系列-添加子课程
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/series/course/add", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity addSeriesCourseChild(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "addSeriesCourseChild", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * PC_店铺-单品-编辑
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/single/edit", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity editShopSingleVideo(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "editSingleGoods", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * PC_店铺-用户列表
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/user/list", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getUserList(
            @RequestHeader("access_token") String accessToken,
            @RequestParam(value = "page_size", defaultValue = "10") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestParam(value = "type",defaultValue = "") String type,
            @RequestParam(value = "nick_name",defaultValue = "") String keyword,
            @RequestParam(value = "phone",defaultValue = "") String phone,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "getUserList", accessToken, version);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("page_size", pageSize);
        paramCode.put("page_num", pageNum);
        if(StringUtils.isNotEmpty(phone))
            paramCode.put("phone", phone);
        if(StringUtils.isNotEmpty(type))
            paramCode.put("type", type);
        if(StringUtils.isNotEmpty(keyword))
            paramCode.put("nick_name", keyword);
        requestEntity.setParam(paramCode);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * PC_店铺-评论列表（用户留言）
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/message/list", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getMessageList(
            @RequestHeader("access_token") String accessToken,
            @RequestParam(value = "page_size", defaultValue = "10") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestParam(value = "nick_name",defaultValue = "") String keyword,
            @RequestParam(value = "course_name",defaultValue = "") String courseName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "getMessageList", accessToken, version);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("page_size", pageSize);
        paramCode.put("page_num", pageNum);
        if(StringUtils.isNotEmpty(courseName))
            paramCode.put("course_name", courseName);
        if(StringUtils.isNotEmpty(keyword))
            paramCode.put("nick_name", keyword);
        requestEntity.setParam(paramCode);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * PC_店铺-用户反馈
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/feedback/list", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getFeedbackList(
            @RequestHeader("access_token") String accessToken,
            @RequestParam(value = "page_size", defaultValue = "10") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestParam(value = "nick_name",defaultValue = "") String keyword,
            @RequestParam(value = "comment",defaultValue = "") String comment,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "getFeedbackList", accessToken, version);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("page_size", pageSize);
        paramCode.put("page_num", pageNum);
        if(StringUtils.isNotEmpty(comment))
            paramCode.put("comment", comment);
        if(StringUtils.isNotEmpty(keyword))
            paramCode.put("nick_name", keyword);
        requestEntity.setParam(paramCode);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * PC_店铺-单品上下架
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/single/{course_id}/updown/{type}", method = RequestMethod.PUT)
    public @ResponseBody ResponseEntity goodsSingleUpdown(
            @PathVariable("course_id") String courseId,
            @PathVariable("type") String type,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "goodsSingleUpdown", accessToken, version);
        Map<String,String> param = new HashMap<>();
        param.put("course_id",courseId);
        param.put("course_updown",type);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * PC_店铺-商品-系列-列表
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/series/list", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getSeriesList(
            @RequestHeader("access_token") String accessToken,
            @RequestParam(value = "page_size", defaultValue = "10") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestParam(value = "status",defaultValue = "") String status,
            @RequestParam(value = "keyword",defaultValue = "") String keyword,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "getSeriesList", accessToken, version);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("page_size", pageSize);
        paramCode.put("page_num", pageNum);
        if(StringUtils.isNotEmpty(status))
            paramCode.put("status", status);
        if(StringUtils.isNotEmpty(keyword))
            paramCode.put("keyword", keyword);
        requestEntity.setParam(paramCode);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * PC_店铺-商品-系列-详情
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/series/{series_id}/info", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getSeriesDetailInfo(
            @PathVariable("series_id") String series_id,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "getSeriesInfo", accessToken, version);
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("series_id", series_id);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * PC_店铺-商品-系列-子课程列表
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/series/{series_id}/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getSeriesCourseList(
            @RequestHeader("access_token") String accessToken,
            @PathVariable(value = "series_id") String series_id,
            @RequestParam(value = "page_size", defaultValue = "10") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "getSeriesCourseList", accessToken, version);
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("series_id", series_id);
        param.put("page_size", pageSize);
        param.put("page_num", pageNum);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * PC_店铺-轮播图-选择跳转的课程
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/course/{type}/list", method = RequestMethod.GET)
    public  @ResponseBody ResponseEntity getBannerCourseList(
            @RequestHeader("access_token") String accessToken,
            @PathVariable(value = "type" ) String type,
            @RequestParam(value = "page_size", defaultValue = "10") long pageSize,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "getBannerCourseList", accessToken, version);
        Map<String,Object> param = new HashMap<>();
        param.put("type",type);
        param.put("page_size", pageSize);
        param.put("page_num", pageNum);
        param.put("keyword", keyword);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * PC_店铺-获取余额信息
     * @param access_token 后台证书
     * @param version 版本
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/user/gains",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity  userGains(
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "userGains", access_token, version);
        Map<String, Object> parMap = new HashMap<>();
        requestEntity.setParam(parMap);
        return this.process(requestEntity, serviceManger, message);
    }
    /**PC_店铺-获取订单明细
     * @param pageSize
     * @param pageNum
     * @param keyword       用户名称
     * @param courseName    商品名称
     * @param orderType     订单类型（1:分销订单，2:普通订单）
     * @param goodsType     商品类型(0:单品 1:打赏 2:系列)
     * @param user_id       用户ID
     * @param goods_id      商品ID
     * @param access_token
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/gains/order/list",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity  gainsOrdersList(
            @RequestParam(value = "page_size", defaultValue = "10") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestParam(value = "nick_name",defaultValue = "") String keyword,
            @RequestParam(value = "goods_name",defaultValue = "") String courseName,
            @RequestParam(value = "order_type",defaultValue = "") String orderType,
            @RequestParam(value = "goods_type",defaultValue = "") String goodsType,
            @RequestParam(value = "user_id",defaultValue = "") String user_id,
            @RequestParam(value = "goods_id",defaultValue = "") String goods_id,
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "gainsOrdersList", access_token, version);
        Map<String, Object> parMap = new HashMap<>();
        parMap.put("page_size",pageSize);
        parMap.put("page_num",pageNum);
        if(StringUtils.isNotEmpty(keyword))
            parMap.put("nick_name",keyword);
        if(StringUtils.isNotEmpty(courseName))
            parMap.put("course_name",courseName);
        if(StringUtils.isNotEmpty(orderType))
            parMap.put("order_type",orderType);
        if(StringUtils.isNotEmpty(goodsType))
            parMap.put("profit_type",goodsType);
        if(StringUtils.isNotEmpty(user_id))
            parMap.put("user_id",user_id);
        if(StringUtils.isNotEmpty(goods_id))
            parMap.put("goods_id",goods_id);
        requestEntity.setParam(parMap);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * PC_店铺-上架知享
     * @param accessToken  token
     * @param shelves_id  上架id
     * @param shelves_type 上架类型  0课程 1系列
     * @param classify_id 分类id
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/qnsharing/shelves", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity shelvesQNSharing(
            @RequestParam(value = "shelves_id", defaultValue = "") String shelves_id,
            @RequestParam(value = "shelves_type", defaultValue = "1") int shelves_type,
            @RequestParam(value = "classify_id") Integer classify_id,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shelvesQNSharing", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shelves_id",shelves_id);
        param.put("shelves_type",shelves_type);
        param.put("classify_id",classify_id);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }


    /**
     * PC_店铺-讲师列表
     * @param page_size 每页大小（默认10条）
     * @param page_num 页码（默认第一页）
     * @param lecturer_name 讲师名称
     * @param lecturer_identity 讲师身份 讲师身份  0:普通讲师  1:签约讲师
     * @param create_from 创建来源  0:saas 1:内部
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shops", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getShops(
            @RequestParam(value = "page_size", defaultValue = "10") long page_size,
            @RequestParam(value = "page_num", defaultValue = "1") long page_num,
            @RequestParam(value = "lecturer_name", defaultValue = "") String lecturer_name,
            @RequestParam(value = "lecturer_identity", defaultValue = "") String lecturer_identity,
            @RequestParam(value = "create_from", defaultValue = "") String create_from,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "getShops", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("page_size",page_size);
        param.put("page_num",page_num);
        if(!MiscUtils.isEmpty(lecturer_name)){
            param.put("lecturer_name","%"+lecturer_name+"%");
        }
        if(!MiscUtils.isEmpty(lecturer_identity)){
            param.put("lecturer_identity",lecturer_identity);
        }
        if(!MiscUtils.isEmpty(create_from)){
            param.put("create_from",create_from);
        }
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * PC_店铺-讲师详情
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/lecturer/info", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getShops(
            @RequestParam(value = "lecturer_id", defaultValue = "") String lecturer_id,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "lecturerInfo", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("lecturer_id",lecturer_id);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * PC_店铺-开通知享
     * @param open_code 邀请码(当前邀请码：137258)
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/sharing/open", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity sharingOpen(
            @RequestParam(value = "open_code") String open_code,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "sharingOpen", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("open_code",open_code);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }


    /**
     * H5_店铺-获取店铺轮播列表
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/list/{shop_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getShopBannerList(
            @PathVariable("shop_id") String shopId,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "queryShopBannerList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id", shopId);
        requestEntity.setParam(param);

        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * H5_店铺-获取店铺系列课程列表
     * @param shopId
     * @param pageCount
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/course/series/list/{shop_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getShopSeriesList(
            @PathVariable("shop_id") String shopId,
            @RequestParam(value="last_series_id", defaultValue="0")String lastSeriesId,
            @RequestParam(value = "page_count", defaultValue = "20") long pageCount,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "findShopSeriesList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id", shopId);
        param.put("last_series_id", lastSeriesId);
        param.put("page_count", pageCount);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * H5_店铺-获取店铺单品直播课程列表
     * @param shopId
     * @param pageCount
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    //TODO 待完善
    @RequestMapping(value = "/shop/course/live_single/list/{shop_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getShopLiveSingleList(
            @PathVariable("shop_id") String shopId,
            @RequestParam(value="last_update_time", defaultValue="0")long lastUpdateTime,
            @RequestParam(value="readed_count", defaultValue="0")long readedCount,
            @RequestParam(value = "page_count", defaultValue = "20") long pageCount,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "findShopLiveSingleList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id", shopId);
        param.put("last_update_time", lastUpdateTime);
        param.put("readed_count", readedCount);
        param.put("page_count", pageCount);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * H5_店铺-获取店铺单品课程（直播除外）列表
     * @param shopId
     * @param lastSingleId
     * @param pageCount
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/course/single/list/{shop_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getShopSingleList(
            @PathVariable("shop_id") String shopId,
            @RequestParam(value="last_single_id", defaultValue="0")String lastSingleId,
            @RequestParam(value = "page_count", defaultValue = "20") long pageCount,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "findShopSingleCourseList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id", shopId);
        param.put("last_single_id", lastSingleId);
        param.put("page_count", pageCount);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
}
