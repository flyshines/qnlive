package qingning.user.server.configuration;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import qingning.server.ServiceManger;
@Configuration
@EnableWebMvc
@ComponentScan({"qingning.user.server.controller","qingning.server.advice"})
@ImportResource(value={"classpath:message.xml","classpath:mqbasicconfig.xml"})
public class SystemConfiguration extends WebMvcConfigurerAdapter {
	private ClassPathXmlApplicationContext context = null;	
	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
		LinkedList<MediaType>supportedMediaTypes = new LinkedList<MediaType>();
		supportedMediaTypes.add(MediaType.APPLICATION_JSON);
		stringConverter.setSupportedMediaTypes(supportedMediaTypes);
		converters.add(stringConverter);
		
		MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
		
		converters.add(jacksonConverter);
		super.configureMessageConverters(converters);
	}
	
	@Bean(name="serviceManger")
	public ServiceManger getServiceManger() throws Exception{
		ServiceManger serviceManger= new ServiceManger();
		serviceManger.initSystem("classpath:application.properties");
		serviceManger.initServer(new String[]{"classpath:qingning-user-server.xml"});
		
		return serviceManger;
	}
	@Bean(name="applicationContext")
	public ClassPathXmlApplicationContext getApplicationContext(){
		if(context==null){
			context=new ClassPathXmlApplicationContext(new String[] {"user-rpc-server.xml"});
			context.start();
		}
		return context;
	}
}
