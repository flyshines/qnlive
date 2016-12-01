package qingning.common.util;

/**
 * Created by loovee on 2016/11/28.
 */
public class AccessTokenUtil {

    public static String generateAccessToken(String userId, String last_login_time){
        //accessToken格式：[x]{userid}[最后一次登录时间]。 [x取0或1]
        long last_login_time_long = Long.parseLong(last_login_time);

        //1.[x] = 最后一次登录时间%2
        long x = last_login_time_long % 2;
        char[] userIdArray = userId.toCharArray();

        StringBuilder transferResult = new StringBuilder();
        //2.针对x不同进行不同的操作
        if(x == 0){
            transferResult.append(0);

            //3.对userId进行相关操作
            for(char userIdChar : userIdArray){
                if(Character.isDigit(userIdChar)){
                    Integer userIdCharTransferNum = Integer.valueOf(userIdChar + "");
                    transferResult.append(9 - userIdCharTransferNum);
                }else{
                    if(Character.isUpperCase(userIdChar)){
                        int temp = 'Z' - userIdChar;
                        temp = temp + 65;
                        char transferResultTemp = (char) temp;
                        transferResult.append(transferResultTemp);
                    }else {
                        int temp = 'z' - userIdChar;
                        temp = temp + 97;
                        char transferResultTemp = (char) temp;
                        transferResult.append(transferResultTemp);
                    }
                }
            }

            //4.对时间进行操作
            long xorTime = last_login_time_long ^ 1111111111111L;
            transferResult.append(xorTime);

        }else if(x == 1){
            transferResult.append(1);

            //3.对userId进行相关操作
            for(char userIdChar : userIdArray){
                if(Character.isDigit(userIdChar)){
                    transferResult.append(userIdChar+"");
                    continue;
                }else {
                    if(Character.isUpperCase(userIdChar)){
                        userIdChar = Character.toLowerCase(userIdChar);
                        int temp = 'z' - userIdChar;
                        temp = temp + 97;
                        char transferResultTemp = (char) temp;
                        transferResult.append(transferResultTemp);
                    }else {
                        userIdChar = Character.toUpperCase(userIdChar);
                        int temp = 'Z' - userIdChar;
                        temp = temp + 65;
                        char transferResultTemp = (char) temp;
                        transferResult.append(transferResultTemp);
                    }
                }
            }

            //4.对时间进行操作
            long xorTimePre = (last_login_time_long + last_login_time_long);
            long xorTime = 0L;
            if((xorTimePre + "").length() == 13){
                xorTime = xorTimePre ^ 1111111111111L;
            }else{
                xorTime = xorTimePre ^ 11111111111111L;
            }

            transferResult.append(xorTime);
        }

        return transferResult.toString();
    }

    public static boolean validateAccessToken(String accessToken){
        Character firstCharacter = accessToken.charAt(0);
        //对第一位进行校验
        if(firstCharacter.equals('0') || firstCharacter.equals('1')){

            //取出时间
            String preProcessTime = accessToken.substring(33);
            long postProcessTime = 0L;

            //还原时间
            if(firstCharacter.equals("0")){
                if(preProcessTime.length() == 13){
                    postProcessTime = Long.parseLong(preProcessTime) ^ 1111111111111L;
                }else{
                    postProcessTime = Long.parseLong(preProcessTime) ^ 11111111111111L;
                }

                //对时间进行取mod操作，检验取mod结果和第一位字符是否相同
                if(postProcessTime % 2 == 0){
                    return true;
                }else {
                    return false;
                }

            }else if(firstCharacter.equals("1")){
                if(preProcessTime.length() == 13){
                    postProcessTime = Long.parseLong(preProcessTime) ^ 1111111111111L;
                }else{
                    postProcessTime = Long.parseLong(preProcessTime) ^ 11111111111111L;
                }

                postProcessTime = postProcessTime / 2;

                //对时间进行取mod操作，检验取mod结果和第一位字符是否相同
                if(postProcessTime % 2 == 1){
                    return true;
                }else {
                    return false;
                }

            }else {
                return false;
            }

        }else{
            return false;
        }
    }


    public static void main(String[] args) {

        System.out.println(generateAccessToken("Z", "1234567896543"));
    }
}
