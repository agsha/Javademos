package servletmvcdemo;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

import java.io.*;

public class Greetings extends HttpServlet {
	
	public void doPost(HttpServletRequest request,
						HttpServletResponse response)
						throws IOException, ServletException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		String country = request.getParameter("country");
		out.println("You are from " + country);
		//GreetingsModel model = new GreetingsModel();
		String greeting = GreetingsModel.getGreetingsInLanguage(country);
		out.println("</br>");
		out.println(greeting);
	}

}
