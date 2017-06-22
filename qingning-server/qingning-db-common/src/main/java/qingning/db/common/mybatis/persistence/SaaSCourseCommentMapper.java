package qingning.db.common.mybatis.persistence;


import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;

import java.util.Map;

public interface SaaSCourseCommentMapper {
    int deleteByPrimaryKey(String commentId);

    int insert(Map<String, Object> record);

    Map<String, Object> selectByPrimaryKey(String commentId);

    int updateByPrimaryKey(Map<String, Object> record);

    /**
     * 根据店铺ID获取评论列表
     *
     * @param param
     * @param page
     * @return
     */
    PageList<Map<String, Object>> selectCommentByShop(Map<String, Object> param, PageBounds page);

}
