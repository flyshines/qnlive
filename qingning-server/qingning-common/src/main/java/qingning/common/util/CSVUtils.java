package qingning.common.util;

import com.alibaba.dubbo.common.utils.StringUtils;
import org.apache.commons.beanutils.BeanUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;


public class CSVUtils {
    /**
     * @param exportData
     * @param map
     * @param outPutPath
     * @param fileName
     * @param type      1:课程 2:通用 3:订单 4 收益
     * @return
     */
    public static File createCSVFile(List exportData, LinkedHashMap map, String outPutPath, String fileName) {
    	if(StringUtils.isBlank(outPutPath)){
    		String os = System.getProperties().getProperty("os.name");
    		if(os != null && os.toLowerCase().indexOf("windows") > -1){
    			outPutPath = "d:\\export\\";
    		}else{
    			outPutPath = MiscUtils.getConfigByKey("export_path")+MiscUtils.formatDate(String.valueOf(System.currentTimeMillis()));
    		}
    	}
        File csvFile = null;
        BufferedWriter csvFileOutputStream = null;
        try {
            File file = new File(outPutPath);
            if (!file.exists()) {
                file.mkdir();
            }
            csvFile = File.createTempFile(fileName, ".csv", new File(outPutPath));
            csvFileOutputStream = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(csvFile), "GBK"), 8192);

        	for (Iterator propertyIterator = map.entrySet().iterator();
        			propertyIterator.hasNext();) {
        		Map.Entry propertyEntry = (Map.Entry) propertyIterator.next();
        		csvFileOutputStream.write((("\"" +
        				(String) propertyEntry.getValue()) != null)
        				? (String) propertyEntry.getValue() : ("" + "\""));

        		if (propertyIterator.hasNext()) {
        			csvFileOutputStream.write(",");
        		}
        	}

            csvFileOutputStream.newLine();

            for (Iterator iterator = exportData.iterator(); iterator.hasNext();) {
                Object row = iterator.next();
                for (Iterator propertyIterator = map.entrySet().iterator();
                        propertyIterator.hasNext();) {
                    Map.Entry propertyEntry = (Map.Entry) propertyIterator.next();
                    
                    String val = BeanUtils.getProperty(row, (String) propertyEntry.getKey());
                    String key = propertyEntry.getKey().toString();
                    if(val == null || val.length() == 0){
                        if("coupon_type".equals(key)){
                                val = "抵扣";
                        }else {
                            val = " ";
                        }
                    }else{
                        try {
							if("create_time".equals(key)){
								val = MiscUtils.formatDateHrous1(val);
							}else if("update_time".equals(key) && val.indexOf("-") != -1){
								val = MiscUtils.formatDateHrous1(val);
							}else if("record_time".equals(key) && val.indexOf("-") != -1){
								val = MiscUtils.formatDateHrous1(val);
							}else if("profit_type".equals(key)){
                                if("0".equals(val)){
                                    val = "门票收益";
                                }else if("1".equals(val)){
                                    val = "打赏";
                                }else if("2".equals(val)){
                                    val = "课程分销收益";
                                }else if("3".equals(val)){
                                    val = "分销者收益";
                                }
							}
						} catch (Exception e) {
							val = "";
						}
                    }
                    if(key.contains("time")){
                        csvFileOutputStream.write(val.replaceAll("\"", "“").replaceAll("null", ""));
                    }else{
                        csvFileOutputStream.write(val.replaceAll("\"", "“").replaceAll("null", "").replaceAll("-",""));
                    }

                    if (propertyIterator.hasNext()) {
                        csvFileOutputStream.write(",");
                    }
                }

                if (iterator.hasNext()) {
                    csvFileOutputStream.newLine();
                 }
            }

            csvFileOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                csvFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return csvFile;
    }

    public static void exportFile(HttpServletResponse response, String fileName, File file) throws IOException {
        response.setContentType("application/csv;charset=UTF-8");
        response.setHeader("Content-Disposition","attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));
        InputStream in = null;

        try {
            in = new FileInputStream(file);
            byte[] buffer = new byte[2048];
            response.setCharacterEncoding("UTF-8");
            OutputStream out = response.getOutputStream();
            
            int i = -1;
            while ((i = in.read(buffer)) != -1) {
            	out.write(buffer, 0, i);
  		  	}
	  		out.flush();
	  		out.close();
          
        } catch (FileNotFoundException e) {
            System.out.println(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    
    /**
     * 删除该目录filePath下的所有文件
     * @param filePath
     *      文件目录路径
     */
    public static void deleteFiles(String filePath) {
      File file = new File(filePath);
      if (file.exists()) {
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
          if (files[i].isFile()) {
            files[i].delete();
          }
        }
      }
    }
   
    /**
     * 删除单个文件
     * @param filePath
     *     文件目录路径
     * @param fileName
     *     文件名称
     */
    public static void deleteFile(String filePath, String fileName) {
      File file = new File(filePath);
      if (file.exists()) {
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
          if (files[i].isFile()) {
            if (files[i].getName().equals(fileName)) {
              files[i].delete();
              return;
            }
          }
        }
      }
    }
    
    
    /**
     * 测试数据
     * @param args
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String[] args) {
      List exportData = new ArrayList<Map>();
      Map row1 = new LinkedHashMap<String, String>();
      row1.put("1", "11");
      row1.put("2", "12");
      row1.put("3", "13");
      row1.put("name", "花花洗");
      row1.put("5", "5");
      exportData.add(row1);
      row1 = new LinkedHashMap<String, String>();
      row1.put("1", "21");
      row1.put("2", "22");
      row1.put("3", "23");
      row1.put("name", "刘剑飞");
      row1.put("5", "5");
      row1.put("6", "6");
      exportData.add(row1);
      LinkedHashMap map = new LinkedHashMap();
      map.put("1", "第一列");
      map.put("2", "第二列");
      map.put("3", "第三列");
      map.put("name", "名字");
   
      String fileName = "文件导出";
      File file = CSVUtils.createCSVFile(exportData, map, null, fileName);
      String fileName2 = file.getName();
      System.out.println("文件名称：" + fileName2);
    }
}
