/*-------------------------------------------------------------------*/
/*                                                                   */
/*                                                                   */
/* Copyright IBM Corp. 2013 All Rights Reserved                      */
/*                                                                   */
/*                                                                   */
/*-------------------------------------------------------------------*/
/*                                                                   */
/*        NOTICE TO USERS OF THE SOURCE CODE EXAMPLES                */
/*                                                                   */
/* The source code examples provided by IBM are only intended to     */
/* assist in the development of a working software program.          */
/*                                                                   */
/* International Business Machines Corporation provides the source   */
/* code examples, both individually and as one or more groups,       */
/* "as is" without warranty of any kind, either expressed or         */
/* implied, including, but not limited to the warranty of            */
/* non-infringement and the implied warranties of merchantability    */
/* and fitness for a particular purpose. The entire risk             */
/* as to the quality and performance of the source code              */
/* examples, both individually and as one or more groups, is with    */
/* you. Should any part of the source code examples prove defective, */
/* you (and not IBM or an authorized dealer) assume the entire cost  */
/* of all necessary servicing, repair or correction.                 */
/*                                                                   */
/* IBM does not warrant that the contents of the source code         */
/* examples, whether individually or as one or more groups, will     */
/* meet your requirements or that the source code examples are       */
/* error-free.                                                       */
/*                                                                   */
/* IBM may make improvements and/or changes in the source code       */
/* examples at any time.                                             */
/*                                                                   */
/* Changes may be made periodically to the information in the        */
/* source code examples; these changes may be reported, for the      */
/* sample code included herein, in new editions of the examples.     */
/*                                                                   */
/* References in the source code examples to IBM products, programs, */
/* or services do not imply that IBM intends to make these           */
/* available in all countries in which IBM operates. Any reference   */
/* to the IBM licensed program in the source code examples is not    */
/* intended to state or imply that IBM's licensed program must be    */
/* used. Any functionally equivalent program may be used.            */
/*-------------------------------------------------------------------*/
package com.sampleapp.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONObject;

import wangyq.cloudant.api.SimpleCloudantDBUtil;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class CloudantDBUtil {

	// For grabbing vcap_services info
	private Map<String, String> env;
	private String vcap;
	private String username;
	private String password;
	private String host;
	private String dbname = "weibo-influence-analyzer";

	// For interacting with Cloudant
	private SimpleCloudantDBUtil db;

	// Make this a singleton
	private static CloudantDBUtil instance;

	public static synchronized CloudantDBUtil getInstance() {
		if (instance == null) {
			instance = new CloudantDBUtil();
		}
		return instance;
	}

	private CloudantDBUtil() {

		env = System.getenv();
		vcap = env.get("VCAP_SERVICES");

		if (vcap == null) {
			System.out.println("No VCAP_SERVICES found");
			return;
		}

		System.out.println("VCAP_SERVICES found");

		try {
			JSONObject vcap_services = new JSONObject(vcap);

			Iterator iter = vcap_services.keys();
			JSONArray userProvide = null;

			// find instance of Cloudant JSONDB bound to app
			while (iter.hasNext()) {
				String key = (String) iter.next();
				if (key.startsWith("cloudantNoSQLDB")) {
					userProvide = vcap_services.getJSONArray(key);
				}
			}

			// Grab the first instance
			JSONObject instance = userProvide.getJSONObject(0);

			JSONObject credentials = instance.getJSONObject("credentials");
			host = credentials.getString("host");
			username = credentials.getString("username");
			password = credentials.getString("password");

			System.out.println("Found all the params");

			// Cloudant initialization
			db = new SimpleCloudantDBUtil(host, dbname, username, password);

			System.out.println("Connected to cloudant on " + host);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// saves the data in the cloudant
	public void saveData(String t_name, int totalscore, int fcount, int fscore,
			int rtcount, int rtscore, int mcount) {
		// check user name
		Map user = null;
		try {
			user = db.fetch(t_name);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("user[" + t_name + "] not found");
		}

		if (user != null) {
			// Update the existing record

			user.put("totalscore", totalscore);
			user.put("fcount", fcount);
			user.put("fscore", fscore);
			user.put("rtcount", rtcount);
			user.put("rtscore", rtscore);
			user.put("mcount", mcount);

			String id = (String) user.get("id");
			String rev = (String) user.get("rev");

			try {
				db.update(id, rev, user);
				System.out.println("Existing document updated");
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Existing document update failure:" + e);
			}

		} else {
			// Insert the new record
			user = new HashMap();
			user.put("twitname", t_name);
			user.put("totalscore", totalscore);
			user.put("fcount", fcount);
			user.put("fscore", fscore);
			user.put("rtcount", rtcount);
			user.put("rtscore", rtscore);
			user.put("mcount", mcount);

			// use t_name as id
			try {
				db.insert(t_name, user);
				System.out.println("New record successfully inserted");
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("New record insert failure:" + e);
			}
		}
	}

	// delete the selected record from mongoDB
	public void delSelected(String twitname) {

		// fetch first
		Map user = null;
		try {
			user = db.fetch(twitname);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("user not exist:" + e);
		}
		if (user != null) {
			try {
				String id = (String) user.get("_id");
				String rev = (String) user.get("_rev");
				db.remove(id, rev);
				System.out.println(twitname + " record deleted");
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("user remove failure:" + e);
			}
		}
	}

	// deletes all the records from cloudant
	public void clearAll() {
		try {
			Map list = db.list(false);
			List rows = (List) list.get("rows");

			List delrows = new ArrayList();
			for (Object o : rows) {
				Map m = (Map) o;
				Map d = new HashMap();
				String id = (String) m.get("id");
				String rev = (String) ((Map) m.get("value")).get("rev");
				d.put("_id", id);
				d.put("_rev", rev);
				d.put("_deleted", true);
				delrows.add(d);
			}

			if (!delrows.isEmpty()) {
				db.remove(delrows);
				System.out.println("Deleted all records");
			} else {
				System.out.println("no records deleted");
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("delete all failure:" + e);
		}
	}

	public Iterator getCursor() {
		Iterator itr = null;
		try {
			Map result = db.list();
			itr = ((List) result.get("rows")).iterator();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("getCursor failure:" + e);
		}
		return itr;
	}

	public int getCount() {
		int count = 0;
		try {
			Map result = db.list(false);
			count = (int) result.get("total_rows");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("getCursor failure:" + e);
		}
		return count;
	}
}
