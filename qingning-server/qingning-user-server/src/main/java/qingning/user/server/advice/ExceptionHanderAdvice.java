package qingning.user.server.advice;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import qingning.common.entity.QNLiveException;
import qingning.common.entity.MessageEntity;
import qingning.common.entity.ResponseEntity;

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
		
		if(exception instanceof QNLiveException){
			QNLiveException qnLiveException = (QNLiveException)exception;
			responseEntity.setCode(qnLiveException.getCode());
			responseEntity.setMsg(message.getMessages(qnLiveException.getCode()));
		}else{
			log.error(exception.getMessage());
			responseEntity.setCode("000099");
			responseEntity.setMsg(message.getMessages("000099"));
		}
		return responseEntity;
	}
}
