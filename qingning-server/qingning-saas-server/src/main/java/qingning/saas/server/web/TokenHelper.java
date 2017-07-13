package qingning.saas.server.web;

import qingning.common.util.AccessTokenUtil;

import java.util.Date;

/**
 * Created by Administrator on 2017/7/12.
 */
public class TokenHelper {
    public static void main(String args[]){

        String access_token = AccessTokenUtil.generateAccessToken("00000edcb978fbd94554a4a948d0cc2e56a5", String.valueOf(new Date().getTime()));
        System.out.print(access_token);

    }
}
