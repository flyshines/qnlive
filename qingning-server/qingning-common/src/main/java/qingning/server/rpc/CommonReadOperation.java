package qingning.server.rpc;

import qingning.common.entity.RequestEntity;

public interface CommonReadOperation {
	Object invoke(RequestEntity requestEntity) throws Exception;
}
