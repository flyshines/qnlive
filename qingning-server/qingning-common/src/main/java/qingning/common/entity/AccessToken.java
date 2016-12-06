package qingning.common.entity;

/**
 * Created by loovee on 2016/12/6.
 */
public class AccessToken {
    String token;
    Integer ExpiresIn;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getExpiresIn() {
        return ExpiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        ExpiresIn = expiresIn;
    }
}
