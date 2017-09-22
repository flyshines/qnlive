package qingning.server.rpc;

import java.util.List;
import java.util.Map;

/**
 * Created by Rouse on 2017/9/22.
 */
public interface SeriesModuleServer {
    /**
     * 根据系列id获得该系列的所有学员列表
     * @param seriesId
     * @return
     */
    List<Map<String, Object>> findSeriesStudentListBySeriesId(String seriesId);

    /**
     * 根据系列id查询系列
     * @param series_id
     * @return
     */
    Map<String,Object> findSeriesBySeriesId(String series_id);
}
