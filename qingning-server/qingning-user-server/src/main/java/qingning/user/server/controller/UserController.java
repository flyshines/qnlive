package qingning.user.server.controller;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.CSVUtils;
import qingning.common.util.Constants;
import qingning.server.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

@RestController
public class UserController extends AbstractController {

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
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "mySingleCourseList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id",shopId);
        param.put("last_course_id",lastCourseId);
        param.put("page_count",pageCount);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 获取已购买的系列课程
     * @param shopId
     * @param lastCourseId
     * @param pageCount
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/user/my_course/series/list", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity mySeriesCourseList(
            @RequestParam(value = "shop_id", defaultValue = "") String shopId,
            @RequestParam(value = "last_course_id", defaultValue = "") String lastCourseId,
            @RequestParam(value = "page_count", defaultValue = "20") long pageCount,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "mySeriesCourseList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id",shopId);
        param.put("last_course_id",lastCourseId);
        param.put("page_count",pageCount);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    //TODO 罗斯 待统一所有消费记录
    /**消费记录
     * @param page_count
     * @param position
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/user/record/consume", method = RequestMethod.GET)
    public
    @ResponseBody ResponseEntity getUserConsumeRecords(
            @RequestParam(value = "page_count", defaultValue = "20") String page_count,
            @RequestParam(value = "position", defaultValue = "") String position,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "getUserConsumeRecords", accessToken, version);
        Map<String, Object> parMap = new HashMap<>();
        parMap.put("page_count", page_count);
        parMap.put("position", position);
        requestEntity.setParam(parMap);
        return this.process(requestEntity, serviceManger, message);
    }
    //TODO
    /**消费记录-saas后台(本店铺所有消费记录)
     * @param page_count
     * @param position
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/user/record/consume/saas", method = RequestMethod.GET)
    public
    @ResponseBody ResponseEntity getUserConsumeRecordsForSaaS(
            @RequestParam(value = "shop_id", defaultValue = "") String shopId,
            @RequestParam(value = "page_count", defaultValue = "20") String page_count,
            @RequestParam(value = "position", defaultValue = "") String position,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "getUserConsumeRecords", accessToken, version);
        Map<String, Object> parMap = new HashMap<>();
        parMap.put("page_count", page_count);
        parMap.put("position", position);
        if(shopId==null|| StringUtils.isEmpty(shopId)){
            throw new QNLiveException("000100");
        }
        parMap.put("shop_id", shopId);
        requestEntity.setParam(parMap);
        return this.process(requestEntity, serviceManger, message);
    }
    //TODO
    /**消费记录-saas收入
     * @param page_count
     * @param position
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/user/record/income/details", method = RequestMethod.GET)
    public
    @ResponseBody ResponseEntity getUserIncomeRecords(
            @RequestParam(value = "page_count", defaultValue = "20") String page_count,
            @RequestParam(value = "position", defaultValue = "") String position,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "getUserIncomeRecords", accessToken, version);
        Map<String, Object> parMap = new HashMap<>();
        parMap.put("page_count", page_count);
        parMap.put("position", position);
        requestEntity.setParam(parMap);
        return this.process(requestEntity, serviceManger, message);
    }

    //TODO
    /**
     * 获取用户可提现金额
     * @param access_token 后台证书
     * @param version 版本
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/user/gains",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity  userGains(
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "userGains", access_token, version);
        Map<String, Object> parMap = new HashMap<>();
        requestEntity.setParam(parMap);
        return this.process(requestEntity, serviceManger, message);
    }

    //TODO
    /**
     * 发起提现申请
     * @param entity
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/user/withdraw", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity addWithdraw(
            HttpEntity<Object> entity,
            @RequestHeader(value="access_token") String accessToken,
            @RequestHeader(value="version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "createWithdraw", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    //TODO
    /**
     * 获取提现记录-客户度
     * @param page_count
     * @param createTime
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/user/withdraw/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getWithdrawList(
            @RequestParam(value="page_count", defaultValue="10") String page_count,
            @RequestParam(value="create_time", defaultValue="0") String createTime,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "getWithdrawList", accessToken, version);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("page_count", page_count);
        paramMap.put("create_time", createTime);

        requestEntity.setParam(paramMap);
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
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "getWithdrawListFinance", accessToken, version);
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
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "getWithdrawListAll", accessToken, version);
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
     * 获取提现记录-saas端
     * @param page_count
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/saas/withdraw/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getWithdrawListSaaS(
            @RequestParam(value="page_count", defaultValue="10") String page_count,
            @RequestParam(value="page_num", defaultValue="1") String page_num,
            @RequestParam(value="user_name",defaultValue="") String user_name,
            @RequestParam(value="user_id",defaultValue="") String user_id,
            @RequestParam(value="status",defaultValue="") String status,
            @RequestHeader(value="access_token", defaultValue = "") String accessToken,
            @RequestHeader(value="version",defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "getWithdrawListSaaS", accessToken, version);
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
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "handleWithDrawResult", accessToken, version);
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
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "getOrderListAll", accessToken, version);
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
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "exportOrderListAll", accessToken, version);
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



}
