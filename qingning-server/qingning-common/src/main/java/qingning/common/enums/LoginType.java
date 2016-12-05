package qingning.common.enums;

public enum LoginType {
    WEIXIN_LOGIN (0), QQ_LOGIN (1), PHONE_LOGIN (2);

    private Integer type;

    private LoginType(Integer type) {
        this.type = type;
    }

    public Integer getType(){
        return type;
    }
}
