package qingning.manage.server.controller;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.CSVUtils;
import qingning.common.util.Constants;import qingning.server.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

@RestController
public class ManageController extends AbstractController {

    /**
     * 获取已购买的单品课程
     * @param shopId
     * @param lastCourseId
     * @param pageCount
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/user/my_course/single/list", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity mySingleCourseList(
            @RequestParam(value = "shop_id", defaultValue = "") String shopId,
            @RequestParam(value = "last_course_id", defaultValue = "") String lastCourseId,
            @RequestParam(value = "page_count", defaultValue = "20") long pageCount,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ManageServer", "mySingleCourseList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id",shopId);
        param.put("last_course_id",lastCourseId);
        param.put("page_count",pageCount);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }


    /**
     * 后台_搜索banner列表
     * @param bannerName
     * @param status
     * @param bannerType
     * @param pageCount
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/banner/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getBannerListBySearch(
            @RequestParam(value="banner_name", defaultValue="") String bannerName,
            @RequestParam(value="status", defaultValue="-1") int status,
            @RequestParam(value="banner_type", defaultValue="-1") int bannerType,
            @RequestParam(value="page_count", defaultValue="20") long pageCount,
            @RequestParam(value="page_num", defaultValue="1") long pageNum,
            @RequestHeader(value="access_token", defaultValue="") String accessToken,
            @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ManageServer", "getBannerListBySearch", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("banner_name", bannerName);
        param.put("status", status);
        param.put("banner_type", bannerType);
        param.put("page_count", pageCount);
        param.put("page_num", pageNum);
        requestEntity.setParam(param);

        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    //TODO
    /**
     * 获取提现记录-后台-财务
     * @param page_count
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/sys/withdraw/finance/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getWithdrawListFinance(
            @RequestParam(value="page_count", defaultValue="20") String page_count,
            @RequestParam(value="page_num", defaultValue="1") String page_num,
            @RequestParam(value="user_name",defaultValue="") String user_name,
            @RequestParam(value="user_id",defaultValue="") String user_id,
            @RequestParam(value="status",defaultValue="") String status,
            @RequestHeader(value="access_token", defaultValue = "") String accessToken,
            @RequestHeader(value="version",defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ManageServer", "getWithdrawListFinance", accessToken, version);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("page_count", page_count);
        paramMap.put("page_num", page_num);
        if(StringUtils.isNotEmpty(user_name))
            paramMap.put("user_name", user_name);
        if(StringUtils.isNotEmpty(user_id))
            paramMap.put("user_id", user_id);
        if(StringUtils.isNotEmpty(status))
            paramMap.put("status", status);
        requestEntity.setParam(paramMap);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


//TODO
    /**
     * 获取提现记录-后台
     * @param page_count
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/sys/withdraw/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getWithdrawListAll(
            @RequestParam(value="page_count", defaultValue="20") String page_count,
            @RequestParam(value="page_num", defaultValue="1") String page_num,
            @RequestParam(value="user_name",defaultValue="") String user_name,
            @RequestParam(value="user_id",defaultValue="") String user_id,
            @RequestParam(value="status",defaultValue="") String status,
            @RequestHeader(value="access_token", defaultValue = "") String accessToken,
            @RequestHeader(value="version",defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ManageServer", "getWithdrawListAll", accessToken, version);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("page_count", page_count);
        paramMap.put("page_num", page_num);
        if(StringUtils.isNotEmpty(user_name))
            paramMap.put("user_name", user_name);
        if(StringUtils.isNotEmpty(user_id))
            paramMap.put("user_id", user_id);
        if(StringUtils.isNotEmpty(status))
            paramMap.put("status", status);
        requestEntity.setParam(paramMap);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 后台_处理提现申请
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/user/withdraw/result", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity getWithdrawList(
            HttpEntity<Object> entity,
            @RequestHeader(value="access_token", defaultValue="") String accessToken,
            @RequestHeader(value="version",defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ManageServer", "handleWithDrawResult", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 获取订单记录-后台
     * @param page_count
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/sys/order/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getOrderListAll(
            @RequestParam(value="page_count", defaultValue="20") String page_count,
            @RequestParam(value="page_num", defaultValue="1") String page_num,
            @RequestParam(value="user_name",defaultValue="") String user_name,
            @RequestParam(value="user_id",defaultValue="") String user_id,
            @RequestParam(value="order_id",defaultValue="") String order_id,
            @RequestParam(value="pre_pay_no",defaultValue="") String pre_pay_no,
            @RequestParam(value="start_time",defaultValue="") Long start_time,
            @RequestParam(value="end_time",defaultValue="") Long end_time,
            @RequestHeader(value="access_token", defaultValue = "") String accessToken,
            @RequestHeader(value="version",defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ManageServer", "getOrderListAll", accessToken, version);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("page_count", page_count);
        paramMap.put("page_num", page_num);
        if(StringUtils.isNotEmpty(user_name)) {
            paramMap.put("user_name", user_name);
        }
        if(StringUtils.isNotEmpty(user_id)) {
            paramMap.put("user_id", user_id);
        }
        if(StringUtils.isNotEmpty(order_id)) {
            paramMap.put("order_id", order_id);
        }
        if(StringUtils.isNotEmpty(pre_pay_no)) {
            paramMap.put("pre_pay_no", pre_pay_no);
        }
        if(start_time!=null)
            paramMap.put("start_time", new Date(Long.valueOf(start_time)));
        if(end_time!=null)
            paramMap.put("end_time", new Date(Long.valueOf(end_time)));
        requestEntity.setParam(paramMap);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 导出订单记录-后台
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value="/sys/order/export",method=RequestMethod.GET)
    public void exportOrderListAll(HttpServletResponse resp, HttpServletRequest req,
                                   @RequestParam(value="page_count", defaultValue="2000") String page_count,
                                   @RequestParam(value="page_num", defaultValue="1") String page_num,
                                   @RequestParam(value="user_name",defaultValue="") String user_name,
                                   @RequestParam(value="user_id",defaultValue="") String user_id,
                                   @RequestParam(value="order_id",defaultValue="") String order_id,
                                   @RequestParam(value="pre_pay_no",defaultValue="") String pre_pay_no,
                                   @RequestParam(value="start_time",defaultValue="") Long start_time,
                                   @RequestParam(value="end_time",defaultValue="") Long end_time,
                                   @RequestParam(value="access_token", defaultValue = "") String accessToken,
                                   @RequestHeader(value="version",defaultValue="") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("ManageServer", "exportOrderListAll", accessToken, version);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("page_count", page_count);
        paramMap.put("page_num", page_num);
        if(StringUtils.isNotEmpty(user_name)) {
            paramMap.put("user_name", user_name);
        }
        if(StringUtils.isNotEmpty(user_id)) {
            paramMap.put("user_id", user_id);
        }
        if(StringUtils.isNotEmpty(order_id)) {
            paramMap.put("order_id", order_id);
        }
        if(StringUtils.isNotEmpty(pre_pay_no)) {
            paramMap.put("pre_pay_no", pre_pay_no);
        }
        if(start_time!=null)
            paramMap.put("start_time", new Date(Long.valueOf(start_time)));
        if(end_time!=null)
            paramMap.put("end_time", new Date(Long.valueOf(end_time)));
        requestEntity.setParam(paramMap);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        List<Map<String, Object>> exportCourseList = new ArrayList<>();
        Map<String,Object> res = (Map<String,Object>)responseEntity.getReturnData();
        if(res.get("list") instanceof List){
            exportCourseList = (List<Map<String, Object>>)res.get("list");
        }

        LinkedHashMap<String,String> headMap = new LinkedHashMap<>();
        headMap.put("order_id", "订单ID");
        headMap.put("pre_pay_no", "微信订单ID");
        headMap.put("user_id", "收益人ID");
        headMap.put("resume_id", "消费者ID");
        headMap.put("distributer_id", "分销者ID");
        headMap.put("user_amount", "收益");
        headMap.put("amount", "订单总额");
        headMap.put("nick_name", "收益人昵称");
        headMap.put("profit_type", "收益来源");
        headMap.put("resume_user", "消费者昵称");
        headMap.put("distributer_user", "分销者昵称");
        headMap.put("create_time", "创建时间");

        File file = CSVUtils.createCSVFile(exportCourseList, headMap, null, "订单记录");
        CSVUtils.exportFile(resp, file.getName(), file);
    }

    /**
     * 后台_课程搜索查询
     * @param searchParam
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/course/searcher", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getCourseListBySearch(
            @RequestParam(value="search_param", defaultValue="") String searchParam,
            @RequestHeader(value="access_token", defaultValue="") String accessToken,
            @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getCourseListBySearch", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("search_param", searchParam);
        requestEntity.setParam(param);

        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 管理后台登录
     * @param entity
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/sys/login", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity adminUserLogin(
            HttpEntity<Object> entity) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "adminUserLogin", null, null);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 后台_获取所有分类列表
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/classify/list", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity getClassifyList(
            @RequestHeader(value = "access_token", defaultValue = "") String accessToken,
            @RequestHeader(value="version", defaultValue="") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getClassifyList", accessToken, version);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 后台_编辑分类信息
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/classify/edit", method = RequestMethod.PUT)
    public @ResponseBody
    ResponseEntity editClassify(
            @RequestHeader(value = "access_token", defaultValue = "") String accessToken,
            HttpEntity<Object> entity,
            @RequestHeader(value="version", defaultValue="") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "editClassify", accessToken, version);
        requestEntity.setParam(entity.getBody());
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 后台_添加分类信息
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/classify/add", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity addClassify(
            @RequestHeader(value = "access_token", defaultValue = "") String accessToken,
            HttpEntity<Object> entity,
            @RequestHeader(value="version", defaultValue="") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "addClassify", accessToken, version);
        requestEntity.setParam(entity.getBody());
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 获取分类信息
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/classifyInfo", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity classifyInfo(
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "classifyInfo", accessToken, null);
        return this.process(requestEntity, serviceManger, message);
    }

}
