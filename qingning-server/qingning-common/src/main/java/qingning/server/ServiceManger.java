package qingning.server;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import qingning.common.entity.FunctionInfo;
import qingning.common.entity.InputParameter;
import qingning.common.entity.OutputParameter;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;

public class ServiceManger {
	private static Log log = LogFactory.getLog(ServiceManger.class);
	private Map<String, Map<String,QNLiveServer>> servicerMap = new HashMap<String, Map<String,QNLiveServer>>();
	private JedisUtils jedisUtils = null;
	
	public JedisUtils getJedisUtils(){
		return jedisUtils;
	}
	
	private void initServerDetails(Map<String,QNLiveServer> map, Element element) throws Exception {
		List<Element> children = element.getChildren();
		if(MiscUtils.isEmpty(children)){
			return;
		}
		for(Element child : children){
			String elementName = child.getName();
			if(!Constants.DEFAULT.equals(elementName) && !Constants.VERSION.equals(elementName)){
				log.info("The incorrectly server element " +elementName);
				continue;
			}

			String className=child.getAttributeValue(Constants.CLASS);
			if(MiscUtils.isEmpty(className)){
				log.info("The class name is not defined by the server element  " +elementName);
				continue;
			}
			
			if(!Constants.DEFAULT.equals(elementName)){
				elementName= child.getAttributeValue(Constants.NUM);
			}
			
			if(MiscUtils.isEmpty(elementName)){
				log.info("The version element must set the num attribute " +className);
				continue;
			}
			List<Element> functionElementList = child.getChildren("function");
			if(MiscUtils.isEmpty(functionElementList)){
				continue;
			}
			
			QNLiveServer gcwServer = (QNLiveServer) Class.forName(className).newInstance();
			map.put(elementName, gcwServer);
			gcwServer.setJedisUtils(jedisUtils);

			for(Element functionElement : functionElementList){
				String functionName=functionElement.getAttributeValue(Constants.NAME);
				if(MiscUtils.isEmpty(functionName)){
					continue;
				}
				
				FunctionInfo functionInfo = new FunctionInfo();
				functionInfo.setFunctionName(functionName);
				gcwServer.addFunctionInfo(functionInfo);
				Element auth = functionElement.getChild(Constants.AUTH);
				if(auth!=null){
					Element accessToken = auth.getChild(Constants.ACCESSTOKEN);				
					if(accessToken != null){
						String require = accessToken.getAttributeValue(Constants.REQUIRE);
						if(require!=null){
							require=require.trim().toLowerCase();
						}
						if("y".equals(require)){
							functionInfo.setAccessTokenRequire(true);
						}
					}
					Element appName = auth.getChild(Constants.APP_NAME);
					if(appName != null){
						String require = appName.getAttributeValue(Constants.REQUIRE);
						if(require!=null){
							require=require.trim().toLowerCase();
						}
						if("y".equals(require)){
							functionInfo.setAppNameRequire(true);
						}
					}


					Element timesLimit = auth.getChild(Constants.TIMESLIMIT);
					if(timesLimit!=null){
						long time = -1;
						try{
							time = Long.parseLong(timesLimit.getAttributeValue(Constants.MILLISECOND));
						} catch(Exception e){
							time = -1;
						}
						functionInfo.setTimesLimits(time);
					}
				}
				
				Element inputs = functionElement.getChild(Constants.INPUTS);
				if(inputs!=null){
					List<Element> parameters = inputs.getChildren(Constants.PARAM);
					if(!MiscUtils.isEmpty(parameters)){
						for(Element parameter : parameters){						
							String parameterName = parameter.getAttributeValue(Constants.NAME);
							if(MiscUtils.isEmpty(parameterName)){
								continue;
							}
							String require = parameter.getAttributeValue(Constants.REQUIRE);
							if(require!=null){
								require=require.trim().toLowerCase();
							}
							String format = parameter.getAttributeValue(Constants.FORMAT);
							String type = parameter.getAttributeValue(Constants.TYPE);						
							String validate = parameter.getAttributeValue(Constants.VALIDATE);
							InputParameter inputParamter = new InputParameter();
							functionInfo.addInputParameter(inputParamter);
							inputParamter.setName(parameterName);
							inputParamter.setFormat(format);
							inputParamter.setType(type);
							inputParamter.setValidate(validate);
							inputParamter.setRequire(require);
						}
					}
				}
				Element outputs = functionElement.getChild(Constants.OUTPUTS);
				if(outputs!=null){
					List<Element> parameters = outputs.getChildren(Constants.PARAM);
					if(!MiscUtils.isEmpty(parameters)){						
						for(Element parameter : parameters){							
							String parameterName = parameter.getAttributeValue(Constants.NAME);
							boolean isSpecial = Constants.SPECIAL.equals(parameterName);
							if(isSpecial){
								if(parameters.size()>1){
									log.warn("The setting of the outputparameter element is not right." +className);
									return;
								}
							}
							OutputParameter outputParameter = createOutputParameter(parameter, className, isSpecial);
							if(!MiscUtils.isEmpty(outputParameter)){
								functionInfo.addOutputParameter(outputParameter);
							}
						}
					}
				}
			}			
		}
	}
	
	private OutputParameter createOutputParameter(Element parameter, String className, boolean isSpecial){
		String parameterName = parameter.getAttributeValue(Constants.NAME);
		if(MiscUtils.isEmpty(parameterName)){
			return null;
		}
		if(!isSpecial){
			if(Constants.SPECIAL.equals(parameterName)){
				log.warn("The setting of the outputparameter element is not right." +className);
				return null;
			}
		}
		String fieldName = parameter.getAttributeValue(Constants.FIELDNAME);
		fieldName=fieldName==null?"":fieldName.trim();
		String type = parameter.getAttributeValue(Constants.TYPE);
		String defaultStr = parameter.getAttributeValue(Constants.DEFAULT);
		String convertStr = parameter.getAttributeValue(Constants.CONVERT);
		OutputParameter outputParameter = new OutputParameter();		
		outputParameter.setFieldName(fieldName);
		outputParameter.setName(parameterName);
		outputParameter.setType(type);
		outputParameter.setDefault(defaultStr==null?"":defaultStr);
		if(!MiscUtils.isEmpty(convertStr)){
			outputParameter.setConvertFunction(convertStr);
		}
		if(isSpecial){
			if(!Constants.SYSLIST.equals(type) && !Constants.SYSOBJECT.equals(type)){
				log.warn("The setting of the outputparameter element is not right." +className);
				return null;
			}
		}
		if(Constants.SYSLIST.equals(type) || Constants.SYSMAP.equals(type)){
			List<Element> list = parameter.getChildren(Constants.PARAM);			
			boolean isList = Constants.SYSLIST.equals(type);
			if(!MiscUtils.isEmpty(list)){
				for(Element child : list){
					OutputParameter childOutputParameter = createOutputParameter(child,className,false);
					if(!MiscUtils.isEmpty(childOutputParameter)){
						if(isList){
							outputParameter.addOutputParameterForList(childOutputParameter);
						} else {
							outputParameter.addOutputParameterForMap(childOutputParameter);
						}
					}
				}
			}
		}
		return outputParameter;
	}
	
	public void initSystem(String path) throws Exception{
		if(MiscUtils.isEmptyString(path)){
			log.info("The ServiceManger(initSystem) is initialized by the empty path");
			return;
		}
		Map<String,String> propertiesMap = MiscUtils.convertPropertiesFileToMap(path);
		this.jedisUtils = new JedisUtils(propertiesMap);
	}
	
	public void initServer(String[] paths) throws Exception{
		if(paths != null && paths.length > 0){
			for(String path:paths){
				initServer(path);
			}
		}
	}
	
	public void initServer(String path) throws Exception{	
		if(MiscUtils.isEmptyString(path)){
			log.info("The ServiceManger(initServer) is initialized by the empty path");
			return;
		}
		InputStream input = null;
		try{
			if(path.toLowerCase().startsWith("classpath:")){				
				input = ServiceManger.class.getClassLoader().getResourceAsStream(path.substring("classpath:".length()));
			} else {
				input = new FileInputStream(path);
			}
			SAXBuilder builder = new SAXBuilder();
			Document document = builder.build(input);
			Element element = document.getRootElement();
			List<Element> children = element.getChildren();
			if(children==null || children.size() <1){
				return;
			}
			for(Element child:children){
				if(!Constants.SERVER.equals(child.getName())){
					continue;
				}
				Attribute attribute = child.getAttribute(Constants.NAME);
				String serviceName=attribute.getValue();
				if(MiscUtils.isEmptyString(serviceName)){
					log.info("The empty serviceName exists in the file "+path);
					continue;
				}
				Map<String,QNLiveServer> map = servicerMap.get(serviceName);
				if(map==null){
					map = new HashMap<String,QNLiveServer>();
					servicerMap.put(serviceName, map);
				}
				initServerDetails(map, child);
			}
		}finally{
			if(input!=null){
				input.close();
			}
		}
	}
	
	public QNLiveServer getServer(String serverName){
		return getServer(serverName,null);
	}
	
	public QNLiveServer getServer(String serverName, String version){
		QNLiveServer server = null;
		Map<String,QNLiveServer> map = servicerMap.get(serverName);
		if(map!=null){
			if(!MiscUtils.isEmptyString(version)){				
				server=map.get(version);
			}
			if(server==null){
				server=map.get(Constants.DEFAULT);
			}			
		}
		return server;
	}
}
