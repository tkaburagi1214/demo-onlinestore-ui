package com.example.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.client.OrderClient;
import com.example.client.ServiceClient;
import com.example.client.UserClient;
import com.example.entity.Orderhistory;
import com.example.entity.Product;
import com.example.util.Utils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@Component
@Controller
public class UiContoller {
	@Autowired
	ServiceClient serviceClient;
	
	@Autowired
	UserClient userClient;
	
	@Autowired
	OrderClient orderClient;
	
	@Autowired
	Utils utils;

	@HystrixCommand
	@RequestMapping("/")
	public String login() {
		return utils.returnLogin();
	}

	@HystrixCommand
	@RequestMapping("/v")
	public String showVersion() {
		return "v2";
	}

	@HystrixCommand
	@RequestMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return utils.returnLogin();
	}

	@HystrixCommand
	@RequestMapping("/kill")
	public String kill() {
		System.exit(-1);
		return utils.returnLogin();
	}
	
	@RequestMapping("/killorder")
	public String killorder() {
		orderClient.kill();
		return utils.returnLogin();
	}
	
	@RequestMapping("/killservice")
	public String kilservice() {
		serviceClient.kill();
		return utils.returnLogin();
	}
	
	@RequestMapping("/killuser")
	public String killuser() {
		userClient.kill();
		return utils.returnLogin();
	}

    @HystrixCommand(fallbackMethod = "fallbackAuthUser")
    @RequestMapping("/auth")
	public String authUser(@RequestParam("user") String user, HttpSession session, Model model)
			throws JsonParseException, JsonMappingException, IOException {
    	if (session == null || user == null) {
			return utils.returnLogin();
		}
		
		session.setAttribute("userid", userClient.getUserInfo(user).getId());
		session.setAttribute("username", userClient.getUserInfo(user).getName());
		session.setAttribute("address", userClient.getUserInfo(user).getAddress());
		session.setAttribute("mobile", userClient.getUserInfo(user).getMobile());
		session.setAttribute("company", userClient.getUserInfo(user).getCompany());
		session.setAttribute("cardnumber", userClient.getUserInfo(user).getCardnumber());
		session.setAttribute("fullname", userClient.getUserInfo(user).getFullname());
		
		model.addAttribute("prds", serviceClient.getProducts());
		model.addAttribute("instance", orderClient.getInstance());
		
		return "onlinestore/index";
	}
    
    
    
    @SuppressWarnings("unused")
    private String fallbackAuthUser(@RequestParam("user") String user, HttpSession session, Model model)
            throws JsonParseException, JsonMappingException, IOException {
        System.out.println("#####FALLBACKED!!#####");
        ObjectMapper mapper = new ObjectMapper();
        Product prd = new Product();
        prd.setCategory("Error");
        prd.setName("Error");
        prd.setPrice(000000);
        model.addAttribute("prds", prd);
//      model.addAttribute("instance", orderClient.getInstance());
        
        return "onlinestore/index";
    }

	@HystrixCommand
	@RequestMapping("/menu")
	public String menu(Model model, HttpSession session) throws JsonParseException, JsonMappingException, IOException {
		if (session.getAttribute("username") == null) {
			return utils.returnLogin();
		}
		model.addAttribute("order", orderClient.getOrderByUser(session.getAttribute("username").toString()));
		return "onlinestore/menu";
	}

	@RequestMapping("/javainfo")
	public String getJavaInfo(Model model, HttpSession session)
			throws JsonParseException, JsonMappingException, IOException {
		if (session.getAttribute("username") == null) {
			return utils.returnLogin();
		}
		
		String vcap = System.getenv("VCAP_APPLICATION");
		ObjectMapper mapper = new ObjectMapper();
		JsonNode vcap_app = mapper.readTree(vcap);
		String[] UiList = new String[3];
		UiList[0] = System.getenv("CF_INSTANCE_ADDR");
		UiList[1] = System.getenv("VERSION");
		UiList[2] = vcap_app.get("instance_index").asText();
		
		String[][] list = {orderClient.getLocalInfo(), userClient.getLocalInfo(), serviceClient.getLocalInfo(), UiList};
				
		model.addAttribute("list", list);

		return "onlinestore/javainfo";
	}
	
	@HystrixCommand
	@RequestMapping("/search")
	public String getPrdsByNameLike(@RequestParam("name") String name, Model model) throws JsonParseException, JsonMappingException, IOException {
		model.addAttribute("prds", serviceClient.getProductsByName(name));
		model.addAttribute("instance", orderClient.getInstance());
		return "onlinestore/index";
		
	}

	@HystrixCommand
	@RequestMapping(value = "/order", method = RequestMethod.POST)
	public String order(HttpServletRequest request, HttpSession session, Orderhistory order, Model model) {
		order.setUsername(session.getAttribute("username").toString());
		order.setProduct(request.getParameter("product"));
		orderClient.saveOrder(order);
		return "onlinestore/thankyou";
	}	
}
