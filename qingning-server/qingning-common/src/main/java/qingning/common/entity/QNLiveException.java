package qingning.common.entity;

public class QNLiveException extends Exception {
	private static final long serialVersionUID = 1L;
	private String code;
	
	public QNLiveException(String code){
		this.code=(code==null?"":code);
	}
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}	
}
