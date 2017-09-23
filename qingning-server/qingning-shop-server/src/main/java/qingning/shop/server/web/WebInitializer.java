package qingning.shop.server.web;

import java.io.IOException;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.DispatcherServlet;

import qingning.shop.server.configuration.SystemConfiguration;

public class WebInitializer implements WebApplicationInitializer{

	@Override
	public void onStartup(ServletContext context) throws ServletException {
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		ctx.register(SystemConfiguration.class);
		ctx.setServletContext(context);

		javax.servlet.ServletRegistration.Dynamic servlet = context.addServlet("dispatcher", new DispatcherServlet(ctx));
		servlet.addMapping("/");
		servlet.setLoadOnStartup(1);
		
		javax.servlet.FilterRegistration.Dynamic filter = context.addFilter("csrfFilter", new Filter (){
			@Override
			public void init(FilterConfig config) throws ServletException {				
			}
			@Override
			public void destroy() {				
			}
			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterchain)
					throws IOException, ServletException {				
				if(response instanceof HttpServletResponse){
					HttpServletResponse httpServletResponse=(HttpServletResponse)response;
					httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
					//httpServletResponse.setHeader("Access-Control-Allow-Credentials", "true");
					httpServletResponse.setHeader("Access-Control-Allow-Methods","POST,GET,DELETE,PUT");
					httpServletResponse.setHeader("Access-Control-Allow-Headers","x-requested-with,content-type,access_token,version");
				}
				filterchain.doFilter(request, response);
			}
			
		});
		filter.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE), true, "dispatcher");
		filter = context.addFilter("encodingFilter", CharacterEncodingFilter.class);
		filter.setInitParameter("encoding", "utf-8");
		filter.setInitParameter("forceEncoding", "true");
		filter.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE), true, "dispatcher");

		
	}
}