package com.sampleapp.web;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import weibo4j.Oauth;
import weibo4j.http.AccessToken;
import weibo4j.model.WeiboException;
import weibo4j.util.WeiboConfig;

/**
 * Servlet implementation class WeiboLoginServlet
 */
@WebServlet("/login")
public class WeiboLoginServlet extends HttpServlet {
	public static final String WEIBO_LOGIN_SERVLET_RES_TOKEN = "WeiboLoginServlet.resToken";
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public WeiboLoginServlet() {
		super();
	}

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");

		String authCode = request.getParameter("code");

		Oauth oauth = new Oauth();
		if (authCode == null) {

			// prepare callback url
			String callback = request.getRequestURL().toString();
			WeiboConfig.updateProperties("redirect_URI", callback);
			// set config for weibo
			Map<String, String> env = System.getenv();
			WeiboConfig.updateProperties("client_ID", env.get("WEIBO_CLIENT_ID"));
			WeiboConfig.updateProperties("client_SERCRET", env.get("WEIBO_CLIENT_SERCRET"));
			String getAuthoriCodeURL;
			try {
				getAuthoriCodeURL = oauth.authorize("code", null);
				response.sendRedirect(getAuthoriCodeURL);
			} catch (WeiboException e) {
				e.printStackTrace();
				response.sendRedirect("index.html?message=errorcode215");
				return;
			}

		} else {

			AccessToken accessToken = null;
			try {
				accessToken = oauth.getAccessTokenByCode(authCode);
			} catch (WeiboException e) {
				e.printStackTrace();
				response.sendRedirect("index.html?message=errorcode215");
				return;
			}

			if (accessToken == null) {
				response.sendRedirect("index.html?message=errorcode215");
				return;
			} else {
				request.getSession().setAttribute(
						WEIBO_LOGIN_SERVLET_RES_TOKEN, accessToken);
				request.getRequestDispatcher("DispCalc").forward(request,
						response);
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}

}
