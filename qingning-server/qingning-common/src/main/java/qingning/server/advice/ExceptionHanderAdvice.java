package qingning.server.advice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import qingning.common.entity.MessageEntity;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.MiscUtils;

import java.lang.reflect.InvocationTargetException;

@ControllerAdvice
public class ExceptionHanderAdvice {
	private static Log log = LogFactory.getLog(ExceptionHanderAdvice.class);
	@Autowired
	private MessageEntity message;
	@ExceptionHandler(value=Exception.class)
	public @ResponseBody ResponseEntity handException(Exception exception){
		ResponseEntity responseEntity = new ResponseEntity();
		if(exception instanceof InvocationTargetException){
			exception = (Exception)((InvocationTargetException)exception).getTargetException();
		}
		//-Ddebug=Y
		boolean debug = "Y".equalsIgnoreCase(System.getProperty("debug"));
		if(exception instanceof QNLiveException){
			QNLiveException qnLiveException = (QNLiveException)exception;
			responseEntity.setCode(qnLiveException.getCode());
			String msg = message.getMessages(qnLiveException.getCode());
			String field = qnLiveException.getField();
			if(debug && !MiscUtils.isEmpty(field)) {
				msg=msg+"("+field+")";
			}
			responseEntity.setMsg(msg);
		}else{
			log.error(exception.getMessage());
			responseEntity.setCode("000099");
			responseEntity.setMsg(message.getMessages("000099"));
		}
		return responseEntity;
	}
}
