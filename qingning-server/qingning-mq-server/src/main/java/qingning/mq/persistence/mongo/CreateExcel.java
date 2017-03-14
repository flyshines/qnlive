//package qingning.mq.persistence.mongo;
//
//import com.mongodb.DBObject;
//import com.mongodb.MongoClient;
//import com.mongodb.MongoCredential;
//import com.mongodb.client.*;
//import org.apache.poi.hssf.usermodel.HSSFRow;
//import org.apache.poi.hssf.util.HSSFColor;
//import org.apache.poi.xssf.usermodel.*;
//import org.bson.Document;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.List;
//import java.util.Objects;
//import java.util.Spliterator;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * Created by 宫洪深 on 2017/3/8.
// * 生成excel表格进行数据统计
// */
//public class CreateExcel {
//
//
//    static String DEVICE_ACTIVE_DB = "DEVICE_ACTIVE_DB";
//    static String USER_REGISTRY_DB = "USER_REGISTRY_DB";
//    public static void main(String[] args){
//        XSSFWorkbook workbook = null;
//        workbook = new XSSFWorkbook();
//        //获取List size作为excel行数
//        XSSFSheet sheet = workbook.createSheet("首次注册");
//        XSSFSheet sheet2 = workbook.createSheet("激活(未授权)");
//
//        //创建第一栏
//        XSSFRow headRow = sheet.createRow(0);
//        XSSFRow headRow2 = sheet2.createRow(0);
//        String[] titleArray = {"UID", "手机号", "性别", "IMEI：android", "IP",
//                "时间","UDID：IOS","平台","设备型号","设备品牌","屏幕大小",
//                "操作系统名称版本","坐标","城市","省份","国家","渠道","版本",
//                "当前网络状态","网络运营商","是否微信授权","是否创建直播间","是否关注微信服务号"};
//        String[] titleArray2 = { "IMEI：android", "IP",
//                "时间","UDID：IOS","平台","设备型号","设备品牌","设备类型","屏幕大小",
//                "操作系统名称版本","坐标","城市","省份","国家","渠道","版本",
//                "当前网络状态","网络运营商","是否微信授权","是否创建直播间","是否关注微信服务号"};
//
//        for(int m=0;m<=titleArray.length-1;m++) {
//            XSSFCell cell = headRow.createCell(m);
//            cell.setCellType(XSSFCell.CELL_TYPE_STRING);
//            sheet.setColumnWidth(m, 6000);
//            XSSFCellStyle style = workbook.createCellStyle();
//            XSSFFont font = workbook.createFont();
//            font.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
//            short color = HSSFColor.RED.index;
//            font.setColor(color);
//            style.setFont(font);
//            //填写数据
//            cell.setCellStyle(style);
//            cell.setCellValue(titleArray[m]);
//        }
//        for(int m=0;m<=titleArray2.length-1;m++) {
//            XSSFCell cell = headRow2.createCell(m);
//            cell.setCellType(XSSFCell.CELL_TYPE_STRING);
//            sheet2.setColumnWidth(m, 6000);
//            XSSFCellStyle style = workbook.createCellStyle();
//            XSSFFont font = workbook.createFont();
//            font.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
//            short color = HSSFColor.RED.index;
//            font.setColor(color);
//            style.setFont(font);
//            //填写数据
//            cell.setCellStyle(style);
//            cell.setCellValue(titleArray2[m]);
//        }
//
//        MongoCursor<Document> mongoCursor;
//        MongoDB mongoDB = new MongoDB();//创建mongodb对象
//        MongoClient mongoClient = mongoDB.getMongoClient("");//获取链接
//        for(String dataBase : mongoClient.listDatabaseNames() ){
//            Pattern devicePattern = Pattern.compile(DEVICE_ACTIVE_DB); // 编译正则表达式
//            Pattern userPattern = Pattern.compile(USER_REGISTRY_DB); // 编译正则表达式
//            Matcher matcher = devicePattern.matcher(dataBase); // 查找字符串中是否有匹配正则表达式的字符/字符串
//            Matcher matcher1 = userPattern.matcher(dataBase); // 查找字符串中是否有匹配正则表达式的字符/字符串
//            boolean devic = matcher.find();//激活
//            boolean user = matcher1.find();//首次注册
//            if(user){
//                MongoDatabase mongoDatabase = mongoClient.getDatabase(dataBase);//关联库
//                int index = 0;
//                for(String collectionName : mongoDatabase.listCollectionNames()) {//进行表判断
//                    String regEx = "USER_REGISTRY_COLL.*";
//                    Pattern pattern = Pattern.compile(regEx); // 编译正则表达式
//                    matcher = pattern.matcher(collectionName); // 查找字符串中是否有匹配正则表达式的字符/字符串
//                    boolean rs = matcher.find();
//                    if (rs) {
//                        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);//关联表
//                        FindIterable<Document> findIterable = collection.find();
//                        mongoCursor = findIterable.iterator();
//                        index = createExcel(mongoCursor,index,dataBase,sheet,titleArray);
//                    }
//                }
//            }else if(devic){
//                MongoDatabase mongoDatabase = mongoClient.getDatabase(dataBase);//关联库
//                int index = 0;
//                for(String collectionName : mongoDatabase.listCollectionNames()) {//进行表判断
//                    String regEx = "DEVICE_ACTIVE_DETAILS_COLL.*";
//                    Pattern pattern = Pattern.compile(regEx); // 编译正则表达式
//                    matcher = pattern.matcher(collectionName); // 查找字符串中是否有匹配正则表达式的字符/字符串
//                    boolean rs = matcher.find();
//                    if (rs) {
//                        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);//关联表
//                        FindIterable<Document> findIterable = collection.find();
//                        mongoCursor = findIterable.iterator();
//                        index = createExcel(mongoCursor,index,dataBase,sheet2,titleArray2);
//                    }
//                }
//            }
//        }
//
//        //写到磁盘上
//        try {
//            FileOutputStream fileOutputStream = new FileOutputStream(new File("C:/Users/Administrator/Desktop/test.xlsx"));
//            workbook.write(fileOutputStream);
//            fileOutputStream.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println("写入结束");
//    }
//
//
//    /**
//     *  生成excel文件
//     * @param mongoCursor 集合
//     * @param index 位置
//     * @param key 进行判断那张标的
//     * @param sheet 哪个excel的
//     * @param titleArray 列
//     */
//    public static int createExcel(MongoCursor<Document> mongoCursor,int index,String key,XSSFSheet sheet,String[] titleArray){
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");//时间转换模式
//        while(mongoCursor.hasNext()){//迭代
//            int cell = 0;//列数
//            XSSFRow row = sheet.createRow(index+1);
//            for(int n=0;n<=titleArray.length-1;n++){ //创建列
//                row.createCell(n);
//            }
//            Document document = mongoCursor.next();
//            if(USER_REGISTRY_DB.equals(key)){
//                String user_id = document.getString("user_id");//UID
//                String phone_num =  document.getString("phone_num");//手机号
//                String gender =  document.getString("gender");//.equals("1")?"男":"女";//性别
//                if(gender != null){
//                    switch (gender){
//                        case "1":
//                            gender = "男";
//                            break;
//                        case "0":
//                            gender = "女";
//                            break;
//                        default:
//                            gender = "其他";
//                            break;
//                    }
//                }
//                row.getCell(cell).setCellValue(user_id);//UID
//                row.getCell(++cell).setCellValue(phone_num);//手机号
//                row.getCell(++cell).setCellValue(gender);//性别
//                cell++;
//            }
//            String ip =  document.getString("ip");//ip
//            String create_time = document.getString("create_time");//创建时间
//            if(create_time != null){
//                create_time = sdf.format(new Date(Long.valueOf(create_time)));
//            }
//            String device_id = document.getString("device_id");//手机唯一标示
//            String imei = "";//IMEI：android
//            String udid = "";//"UDID：IOS
//            String plateform =  document.getString("plateform");//平台
//            if(plateform != null){
//                switch (plateform){
//                    case "1":
//                        plateform = "安卓";
//                        imei = device_id;
//                        break;
//                    case "2":
//                        plateform = "IOS";
//                        udid = device_id;
//                        break;
//                    default:
//                        plateform = "微信";
//                        break;
//                }
//            }
//
//            String device_model =  document.getString("device_model");//设备型号
//            String device_oem =  document.getString("device_oem");//设备品牌
//            String screen_size =  document.getString("screen_size");//屏幕大小
//            String os_version =  document.getString("os_version");//操作系统名称版本
//            String longitude = document.getString("longitude");//经度
//            String latitude = document.getString("latitude");//维度
//            String ind = "经度:"+longitude+",维度:"+latitude;
//            String city =  document.getString("city");//城市
//            String province =  document.getString("province");//省份
//            String country =  document.getString("country");//国家
//            String download_channel = document.getString("download_channel");//渠道
//            String version =  document.getString("version");//版本
//            String net_status =  document.getString("net_status");//当前网络状态
//            String network_operators =  document.getString("network_operators");//网络运营商
//            String webchat_authorization =  document.getString("webchat_authorization");
//            if(webchat_authorization != null && webchat_authorization.equals("1")){//是否微信授权
//                webchat_authorization = "是";
//            }else{
//                webchat_authorization = "没有";
//            }
//            String live_room_build =  document.getString("live_room_build");//.equals("1")?"T":"F";//是否创建直播间
//            if(live_room_build != null && live_room_build.equals("1")){//是否微信授权
//                live_room_build = "是";
//            }else{
//                live_room_build = "没有";
//            }
//
//            String subscribe = document.getString("subscribe");//.equals("1")?"T":"F";//是否关注微信服务号
//            if(subscribe != null && subscribe.equals("1")){//是否微信授权
//                subscribe = "是";
//            }else{
//                subscribe = "没有";
//            }
//            row.getCell(cell).setCellValue(imei);//IMEI：android
//            row.getCell(++cell).setCellValue(ip);//ip
//            row.getCell(++cell).setCellValue(create_time);//创建时间
//            row.getCell(++cell).setCellValue(udid);//"UDID：IOS
//            row.getCell(++cell).setCellValue(plateform);//平台
//            row.getCell(++cell).setCellValue(device_model);//设备型号
//            row.getCell(++cell).setCellValue(device_oem);//设备品牌
//            if(key.equals(DEVICE_ACTIVE_DB)){
//                row.getCell(++cell).setCellValue("无");//设备型号
//            }
//            row.getCell(++cell).setCellValue(screen_size);//屏幕大小
//            row.getCell(++cell).setCellValue(os_version);//操作系统名称版本
//            row.getCell(++cell).setCellValue(ind);//坐标
//            row.getCell(++cell).setCellValue(city);//城市
//            row.getCell(++cell).setCellValue(province);//省份
//            row.getCell(++cell).setCellValue(country);//国家
//            row.getCell(++cell).setCellValue(download_channel);//渠道
//            row.getCell(++cell).setCellValue(version);//版本
//            row.getCell(++cell).setCellValue(net_status);//当前网络状态
//            row.getCell(++cell).setCellValue(network_operators);//网络运营商
//            row.getCell(++cell).setCellValue(webchat_authorization);//是否微信授权
//            row.getCell(++cell).setCellValue(live_room_build);//是否创建直播间
//            row.getCell(++cell).setCellValue(subscribe);//是否关注微信服务号
//            index++;
//        }
//        return index;
//    }
//
//
//}
