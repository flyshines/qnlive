package qingning.server.rpc;

import java.util.Map;

/**
 * Created by Administrator on 2017/9/22.
 */
public interface LecturerModuleServer {
    Map<String,Object> findLectureByLectureId(String lecture_id);

}
