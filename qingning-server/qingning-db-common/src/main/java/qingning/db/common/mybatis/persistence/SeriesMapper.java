package qingning.db.common.mybatis.persistence;

import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;

import java.util.List;
import java.util.Map;

public interface SeriesMapper {
    int insertSeries(Map<String,Object> record);
    int updateSeries(Map<String,Object> record);
    int increaseSeriesStudent(String series_id);
    Map<String,Object> findSeriesBySeriesId(String series_id);
    List<Map<String,Object>> findLastestFinishSeries(Map<String,Object> record);
    List<Map<String,Object>> findSeriesBySearch(Map<String,Object> record);
    int increaseSeriesCourse(String series_id);

    /**根据讲师ID获取该讲师的所有系列课程
     * @param param
     * @param page
     * @return
     */
    PageList<Map<String,Object>> selectSeriesListByLecturerId(Map<String, Object> param, PageBounds page);
}