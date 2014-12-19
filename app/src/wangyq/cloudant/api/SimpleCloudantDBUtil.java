package wangyq.cloudant.api;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;

/**
 * This class provide simple operation on cloudantdb / couchdb
 * @author wangyq
 *
 */
/**
 * @author wangyq
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SimpleCloudantDBUtil extends HttpActions {

	private String host;
	private int port = -1;
	private String dbName;
	private String username;
	private String password;
	private String url;

	public SimpleCloudantDBUtil(String host, String dbName, String username,
			String password) {
		this.host = host;
		this.dbName = dbName;
		this.username = username;
		this.password = password;
	}

	public SimpleCloudantDBUtil(String host, String dbName) {
		this(host, dbName, null, null);
	}

	public SimpleCloudantDBUtil(String url, String username, String password) {
		this.username = username;
		this.password = password;
		this.url = url;
	}

	/**
	 * @return the host
	 */
	@Override
	public String getHost() {
		return host;
	}

	/**
	 * @param host
	 *            the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the port
	 */
	@Override
	public int getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the dbName
	 */
	@Override
	public String getDbName() {
		return dbName;
	}

	/**
	 * @param dbName
	 *            the dbName to set
	 */
	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	/**
	 * insert document
	 * 
	 * @param id
	 * @param object
	 * @return
	 * @throws Exception
	 */
	public Map insert(String id, Map object) throws Exception {
		return put(new WithId(id), object);
	}

	/**
	 * insert documents using bulk api
	 * 
	 * @param objects
	 * @return
	 * @throws Exception
	 */
	public Map insert(List<Map> objects) throws Exception {
		return post(new BulkDocs(), makeDocs(objects));
	}

	/**
	 * list documents
	 * 
	 * @param includeDocs
	 * @return
	 * @throws Exception
	 */
	public Map list(boolean includeDocs) throws Exception {
		return get(new AllDocs(includeDocs));
	}

	/**
	 * list documents include docs, as same <code>list(true)</code>
	 * 
	 * @return
	 * @throws Exception
	 */
	public Map list() throws Exception {
		return list(true);
	}

	/**
	 * fetch a document
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public Map fetch(String id) throws Exception {
		return get(new WithId(id));
	}

	/**
	 * update a document
	 * 
	 * @param id
	 * @param rev
	 * @param object
	 * @return
	 * @throws Exception
	 */
	public Map update(String id, String rev, Object object) throws Exception {
		return put(new WithId(id, rev), object);
	}

	/**
	 * update documents using bulk api
	 * 
	 * @param objects
	 * @return
	 * @throws Exception
	 */
	public Map update(List<Map> objects) throws Exception {
		return post(new BulkDocs(), makeDocs(objects));
	}

	/**
	 * remove a document
	 * 
	 * @param id
	 * @param rev
	 * @return
	 * @throws Exception
	 */
	public Map remove(String id, String rev) throws Exception {
		return delete(new WithId(id, rev));
	}

	/**
	 * remove documents using bulk api
	 * 
	 * @param objects
	 * @return
	 * @throws Exception
	 */
	public Map remove(List<Map> objects) throws Exception {
		return post(new BulkDocs(), makeDocs(objects));
	}

	private Map makeDocs(List objects) {
		Map docs = new HashMap();
		docs.put("docs", objects);
		return docs;
	}

	class Login extends AbstractPreparer {

		@Override
		public URIBuilder prepareUrl(URIBuilder builder) {
			if (username != null && password != null) {
				return super.prepareUrl(builder)
						.setUserInfo(username, password);
			} else {
				return super.prepareUrl(builder);
			}
		}

		@Override
		public Request prepareRequest(Request request) {

			if ("https".equals(getURLSchema().toLowerCase())) {
				if (username != null && password != null) {
					String encodedUserPass = new String(
							Base64.encodeBase64((username + ":" + password)
									.getBytes()));
					return super.prepareRequest(request).setHeader(
							"Authorization", "Basic " + encodedUserPass);
				}
			}
			return super.prepareRequest(request);

		}

	}

	class BulkDocs extends Login {

		@Override
		public URIBuilder prepareUrl(URIBuilder builder) {
			return appendPath(super.prepareUrl(builder), "/_bulk_docs");
		}
	}

	class AllDocs extends Login {

		private boolean include_docs = false;

		public AllDocs() {
		}

		public AllDocs(boolean include_docs) {
			this.include_docs = include_docs;
		}

		@Override
		public URIBuilder prepareUrl(URIBuilder builder) {
			return addParamIf(
					appendPath(super.prepareUrl(builder), "/_all_docs"),
					"include_docs", "true", include_docs);
		}
	}

	class WithId extends Login {

		private String id;
		private String rev;

		public WithId(String id) {
			this.id = id;
		}

		public WithId(String id, String rev) {
			this.id = id;
			this.rev = rev;
		}

		@Override
		public URIBuilder prepareUrl(URIBuilder builder) {
			return addParamIf(appendPath(super.prepareUrl(builder), "/", id),
					"rev", rev, rev != null);
		}

	}

	@Override
	protected URI build() throws Exception {
		if (url != null && url.length() > 0) {
			return prepareUrl(new URIBuilder(url)).build();
		} else {
			return super.build();
		}
	}

	public void createDatabaseIfNotExists() throws Exception {
		if (dbName == null || dbName.length() == 0) {
			return;
		}
		try {
			get(new Login());
			// db exists, do nothing
		} catch (HttpResponseException e) {
			if (404 == e.getStatusCode()) {
				// db not exists
				// create one
				put(new Login(), null);
			}
		}
	}

}
