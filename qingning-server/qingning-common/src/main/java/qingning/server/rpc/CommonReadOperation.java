package qingning.server.rpc;

import qingning.common.entity.RequestEntity;

public interface CommonReadOperation {
	Object invokeProcess(RequestEntity requestEntity) throws Exception;
}
