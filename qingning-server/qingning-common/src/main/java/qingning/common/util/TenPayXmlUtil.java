package qingning.common.util;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class TenPayXmlUtil {

    public static Map<String, String> doXMLParse(String strxml) throws IOException,ParserConfigurationException,SAXException{
        strxml = strxml.replaceFirst ("encoding=\".*\"", "encoding=\"UTF-8\"");
        if (null == strxml || "".equals (strxml)) { return null; }
        Map<String, String> m = new TreeMap<String, String> ();
        InputStream in = new ByteArrayInputStream (strxml.getBytes ("UTF-8"));
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance ();
        DocumentBuilder builder = factory.newDocumentBuilder ();
        Document doc = builder.parse (in);
        NodeList returnCodeList = doc.getElementsByTagName ("return_code");
        for ( int i = 0 ; i < returnCodeList.getLength () ; i++ ) {
            String returnCode = returnCodeList.item (i).getFirstChild ().getNodeValue ();
            m.put ("return_code", returnCode);
        }
        NodeList returnMsgList = doc.getElementsByTagName ("return_msg");
        for ( int i = 0 ; i < returnMsgList.getLength () ; i++ ) {
            String returnMsg = returnMsgList.item (i).getFirstChild ().getNodeValue ();
            m.put ("return_msg", returnMsg);
        }
        if ("SUCCESS".equals (m.get ("return_code"))) {
            NodeList resultCodeList = doc.getElementsByTagName ("result_code");
            for ( int i = 0 ; i < resultCodeList.getLength () ; i++ ) {
                String resultCode = resultCodeList.item (i).getFirstChild ().getNodeValue ();
                m.put ("result_code", resultCode);
            }
            NodeList appIdList = doc.getElementsByTagName ("appid");
            if (appIdList != null) {
                for ( int i = 0 ; i < appIdList.getLength () ; i++ ) {
                    String appId = appIdList.item (i).getFirstChild ().getNodeValue ();
                    m.put ("appid", appId);
                }
            }
            NodeList nonceStrList = doc.getElementsByTagName ("nonce_str");
            if (nonceStrList != null) {
                for ( int i = 0 ; i < nonceStrList.getLength () ; i++ ) {
                    String nonceStr = nonceStrList.item (i).getFirstChild ().getNodeValue ();
                    m.put ("random_char", nonceStr);
                }
            }
            if ("SUCCESS".equals (m.get ("result_code"))) {
                NodeList prePayIdList = doc.getElementsByTagName ("prepay_id");
                if (prePayIdList != null) {
                    for ( int i = 0 ; i < prePayIdList.getLength () ; i++ ) {
                        String prePayId = prePayIdList.item (i).getFirstChild ().getNodeValue ();
                        m.put ("prepay_id", prePayId);
                    }
                }
                NodeList tradeStateList = doc.getElementsByTagName ("trade_state");
                if (tradeStateList != null) {
                    for ( int i = 0 ; i < tradeStateList.getLength () ; i++ ) {
                        String tradeState = tradeStateList.item (i).getFirstChild ().getNodeValue ();
                        m.put ("trade_state", tradeState);
                    }
                }
                NodeList tradeNoList = doc.getElementsByTagName ("out_trade_no");
                if (tradeNoList != null) {
                    for ( int i = 0 ; i < tradeNoList.getLength () ; i++ ) {
                        String tradeNo = tradeNoList.item (i).getFirstChild ().getNodeValue ();
                        m.put ("out_trade_no", tradeNo);
                    }
                }
                NodeList transactionList = doc.getElementsByTagName ("transaction_id");
                if (transactionList != null) {
                    for ( int i = 0 ; i < transactionList.getLength () ; i++ ) {
                        String transaction = transactionList.item (i).getFirstChild ().getNodeValue ();
                        m.put ("transaction_id", transaction);
                    }
                }
            } else {
                NodeList errDescList = doc.getElementsByTagName ("err_code_des");
                if (errDescList != null) {
                    for ( int i = 0 ; i < errDescList.getLength () ; i++ ) {
                        String errDesc = errDescList.item (i).getFirstChild ().getNodeValue ();
                        m.put ("err_code_des", errDesc);
                    }
                }
            }
        }
        // 关闭流
        in.close ();
        return m;
    }

    public static String doXMLCreate(Map<String, String> params) throws ParserConfigurationException{
        Iterator<Map.Entry<String, String>> it = params.entrySet ().iterator ();
        StringBuilder strBuilder = new StringBuilder ("<xml>\r");
        while (it.hasNext ()) {
            Map.Entry<String, String> entry = it.next ();
            String k = entry.getKey ();
            String v = entry.getValue ();
            strBuilder.append ("<").append (k).append (">").append (v).append ("</").append (k).append (">").append ("\r");
        }
        strBuilder.append ("</xml>");
        return strBuilder.toString ();
    }
}
