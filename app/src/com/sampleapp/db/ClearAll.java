/*-------------------------------------------------------------------*/
/* Copyright IBM Corp. 2013 All Rights Reserved                      */
/*-------------------------------------------------------------------*/

package com.sampleapp.db;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class ClearAll
 */
@SuppressWarnings("rawtypes")
public class ClearAll extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");

		CloudantDBUtil du = CloudantDBUtil.getInstance();
		du.clearAll();

		Iterator cr = du.getCursor();
		request.setAttribute("records", cr);
		request.setAttribute("totinf", du.getCount());
		request.getRequestDispatcher("/displayall.jsp").forward(request,
				response);
	}
}
