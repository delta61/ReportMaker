package Servlet;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import plain.Blank;
import singletons.BlankHolder;
import singletons.ClientWorker;


public class MainServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static ClientWorker cw;
       
    public MainServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		HashMap<String, Blank> blankList = BlankHolder.getInstance(getServletContext().getRealPath("config" + File.separator + "EXAMPLES")).getBlanks();
		cw = ClientWorker.getInstance(getServletContext().getRealPath("config" + File.separator + "clients.txt"));
		
		request.setAttribute("blanklist", blankList);
		request.setAttribute("clientworker", cw);
		
		getServletContext().getRequestDispatcher("/Form.jsp").forward(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
	}

}
