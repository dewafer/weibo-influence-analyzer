/*-------------------------------------------------------------------*/
/* Copyright IBM Corp. 2013 All Rights Reserved                      */
/*-------------------------------------------------------------------*/

package com.sampleapp.web;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import weibo4j.Account;
import weibo4j.Timeline;
import weibo4j.Users;
import weibo4j.http.AccessToken;
import weibo4j.model.Paging;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.model.User;
import weibo4j.model.WeiboException;
import weibo4j.org.json.JSONArray;
import weibo4j.org.json.JSONException;

/**
 * Servlet implementation class DispCalc
 */
public class DispCalc extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DispCalc() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");

		Object attribute = request.getSession().getAttribute(
				WeiboLoginServlet.WEIBO_LOGIN_SERVLET_RES_TOKEN);
		try {
			AccessToken token = null;
			if (attribute != null) {
				token = (AccessToken) attribute;
			} else {
				throw new WeiboException("No accessToken.");
			}
			String weiboNickName = request.getParameter("weibo_name");
			String usrId = null;
			if (weiboNickName == null) {
				// calc self
				Account account = new Account();
				account.client.setToken(token.getAccessToken());
				usrId = account.getUid().getString("uid");
				Users usrs = new Users();
				usrs.setToken(token.getAccessToken());
				weiboNickName = usrs.showUserById(usrId).getScreenName();

			} else {
				Users usrs = new Users();
				usrs.setToken(token.getAccessToken());
				User user = usrs.showUserByScreenName(weiboNickName);
				usrId = user.getId();
			}

			if (usrId == null) {
				throw new WeiboException("No usrId.");
			}

			Users user = new Users();
			user.client.setToken(token.getAccessToken());
			JSONArray userCount = user.getUserCount(usrId);
			int followerCount = userCount.getJSONObject(0).getInt(
					"followers_count");

			Timeline timeline = new Timeline();
			timeline.client.setToken(token.getAccessToken());
			StatusWapper retweets = timeline.getUserTimelineByUid(usrId,
					new Paging(1, 200), 0, 0);

			int retweetCount = 0;

			for (Status tweet : retweets.getStatuses()) {
				retweetCount += tweet.getRepostsCount();
			}

			System.out.println("The rtcount is: " + retweetCount);

			int mentionCount = 0;

			int retweetScore = 0;
			int followerScore = 0;

			if (retweetCount >= 100000)
				retweetScore = 60;
			else if (retweetCount >= 20000)
				retweetScore = 50;
			else if (retweetCount >= 10000)
				retweetScore = 40;
			else if (retweetCount >= 5000)
				retweetScore = 30;
			else if (retweetCount >= 1000)
				retweetScore = 20;
			else if (retweetCount >= 500)
				retweetScore = 10;
			else if (retweetCount >= 100)
				retweetScore = 5;
			else if (retweetCount >= 10)
				retweetScore = 1;

			if (followerCount >= 10000000)
				followerScore = 40;
			else if (followerCount >= 1000000)
				followerScore = 35;
			else if (followerCount >= 500000)
				followerScore = 30;
			else if (followerCount >= 100000)
				followerScore = 25;
			else if (followerCount >= 1000)
				followerScore = 20;
			else if (followerCount >= 500)
				followerScore = 15;
			else if (followerCount >= 100)
				followerScore = 10;
			else if (followerCount >= 10)
				followerScore = 5;

			// Search API call to calculate the mentions out of 100
			// Since Weibo have no such API, we use /statuses/mentions instead.
			Timeline mentions = new Timeline();
			mentions.client.setToken(token.getAccessToken());
			StatusWapper statusWapper = mentions.getMentions(
					new Paging(1, 100), 0, 0, 0);
			mentionCount += statusWapper.getStatuses().size();

			System.out.println("the mcount is: " + mentionCount);

			// Calculate the total score of the user.
			int totalscore = retweetScore + followerScore + mentionCount;

			System.out.println("The total score is: " + totalscore);

			// weibo to be displayed on the google maps
			List<Status> result1 = retweets.getStatuses();

			// TODO Sorry, we don't have klout.
			String kloutScore = "";
			kloutScore = "n/a";

			request.setAttribute("totalscore", totalscore);
			request.setAttribute("t_name", weiboNickName);
			request.setAttribute("fcount", followerCount);
			request.setAttribute("fscore", followerScore);
			request.setAttribute("rtcount", retweetCount);
			request.setAttribute("rtscore", retweetScore);
			request.setAttribute("mcount", mentionCount);
			request.setAttribute("rtweets", retweets.getStatuses());
			request.setAttribute("result1", result1);
			request.setAttribute("score", kloutScore);

			request.setAttribute("BAIDU_MAP_AK", System.getenv().get("BAIDU_MAP_AK"));
			
			request.getRequestDispatcher("result.jsp").forward(request,
					response);
		} catch (JSONException e) {
			e.printStackTrace();
			response.sendRedirect("index.html?message=errorcode99");
		} catch (WeiboException e) {
			e.printStackTrace();
			response.sendRedirect("index.html?message=errorcode-1");
		}
	}
}
