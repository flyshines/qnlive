package qingning.common.entity;

public class QNLiveException extends Exception {
	private static final long serialVersionUID = 1L;
	private String code;
	private String field;
	
	public QNLiveException(String code){
		this.code=(code==null?"":code);
	}

	public QNLiveException(String code, String field){
		this.code=(code==null?"":code);
		this.field=(field==null?"":field);
	}
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}

	public String getField() {
		return field;
	}
}
