package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface UserGainsMapper {
    void insertUserGains(List<Map<String ,Object>> list);

    /**
     * 批量插入
     * @param insertGainsList
     * @return
     */
	int insertUserGainsByList(List<Map<String, Object>> insertGainsList);

}