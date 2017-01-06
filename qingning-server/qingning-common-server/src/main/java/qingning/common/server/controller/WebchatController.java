package qingning.common.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import qingning.common.server.util.ServerUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Controller
public class WebchatController {
	/**
	 * 微信消息接收和token验证
	 * @param model
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping("/webchat/validation")
	public void tokenValidation(Model model, HttpServletRequest request,HttpServletResponse response) throws Exception {
		boolean isGet = request.getMethod().toLowerCase().equals("get");
		PrintWriter print;
		if (isGet) {           
			// 微信加密签名
			String signature = request.getParameter("signature");
			// 时间戳
			String timestamp = request.getParameter("timestamp");
			// 随机数
			String nonce = request.getParameter("nonce");
			// 随机字符串
			String echostr = request.getParameter("echostr");
			// 通过检验signature对请求进行校验，若校验成功则原样返回echostr，表示接入成功，否则接入失败
			if (signature != null && ServerUtils.checkSignature(signature, timestamp, nonce)) {
				try {
					print = response.getWriter();
					print.write(echostr);
					print.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
