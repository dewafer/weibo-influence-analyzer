package wangyq.cloudant.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingJsonFactory;

/**
 * This class provide some simple http actions
 * 
 * @author wangyq
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class HttpActions {

	/**
	 * use jackson to parse JSONs
	 */
	private JsonFactory jsonFactory = new MappingJsonFactory();

	private ExecutorService exec = Executors.newCachedThreadPool();

	/**
	 * http get method
	 * 
	 * @param settings
	 * @return
	 * @throws Exception
	 */
	public Map get(Preparer settings) throws Exception {
		setPreparer(settings);
		return execute(Request.Get(build()));
	}

	/**
	 * http post method
	 * 
	 * @param values
	 * @param settings
	 * @return
	 * @throws Exception
	 */
	public Map post(Preparer settings, Object values) throws Exception {
		setPreparer(settings);
		return execute(Request.Post(build()), values);
	}

	/**
	 * http delete method
	 * 
	 * @param settings
	 * @return
	 * @throws Exception
	 */
	public Map delete(Preparer settings) throws Exception {
		setPreparer(settings);
		return execute(Request.Delete(build()));
	}

	/**
	 * http put method
	 * 
	 * @param settings
	 * @param values
	 * @return
	 * @throws Exception
	 */
	public Map put(Preparer settings, Object values) throws Exception {
		setPreparer(settings);
		return execute(Request.Put(build()), values);
	}

	private void setPreparer(Preparer settings) {
		if (settings != null) {
			preparer = settings;
		}
	}

	/**
	 * execute http method
	 * 
	 * @param httpMethod
	 * @param body
	 * @return
	 * @throws Exception
	 */
	private Map execute(Request httpMethod, Object body) throws Exception {
		// FIXME There is a major bug in HttpComponents 4.3.3 HTTPCLIENT-1474
		// https://issues.apache.org/jira/browse/HTTPCLIENT-1474
		// Use fluent-hc-4.3.2 instead
		return is2Map(setBody(prepareRequest(httpMethod), body).execute()
				.returnContent().asStream());
	}

	private Map execute(Request httpMethod) throws Exception {
		return execute(httpMethod, null);
	}

	/**
	 * Set request body if any
	 * 
	 * @param request
	 * @param body
	 * @return
	 * @throws Exception
	 */
	private Request setBody(Request request, Object body) throws Exception {
		if (body != null) {
			return request.bodyStream(object2is(body),
					ContentType.APPLICATION_JSON);
		} else {
			return request;
		}
	}

	/**
	 * convert an inputStream to a map using jackson's json parser
	 * 
	 * @param inputStream
	 * @return
	 * @throws Exception
	 */
	private Map is2Map(InputStream inputStream) throws Exception {

		JsonParser parser = jsonFactory.createParser(inputStream);
		parser.nextToken();
		if (parser.isExpectedStartArrayToken()) {
			Map result = new LinkedHashMap();
			result.put("array", parser.readValueAs(List.class));
			return result;
		} else {
			return parser.readValueAs(Map.class);
		}

	}

	/**
	 * convert a map to an inputStream using jackson's json generator
	 * 
	 * @param maps
	 * @return
	 * @throws Exception
	 */
	private InputStream object2is(final Object maps) throws Exception {

		PipedOutputStream out = new PipedOutputStream();
		PipedInputStream in = new PipedInputStream(out);
		final JsonGenerator generator = jsonFactory.createGenerator(out);

		// new Thread
		exec.execute(new Runnable() {

			@Override
			public void run() {
				try {
					try {
						generator.writeObject(maps);
					} finally {
						generator.close();
					}
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		return in;
	}

	/**
	 * this method build an URI
	 * 
	 * @return
	 * @throws Exception
	 */
	protected URI build() throws Exception {
		return prepareUrl(
				new URIBuilder().setScheme(getURLSchema()).setHost(getHost())
						.setPort(getPort()).setPath(getDbEndPoint())).build();
	}

	/**
	 * return db endpoint
	 * 
	 * @return
	 */
	protected String getDbEndPoint() {
		String db = getDbName();
		if (db != null && !db.startsWith("/")) {
			db = "/" + db;
		}
		return db;
	}

	private Preparer preparer = new AbstractPreparer() {
	};

	/**
	 * this method set request's header using <code>Preparer</code> before
	 * execute
	 * 
	 * @param request
	 * @return
	 */
	protected Request prepareRequest(Request request) {
		return preparer.prepareRequest(request);
	}

	/**
	 * this method set uri params using <code>Preparer</code> before build
	 * 
	 * @param builder
	 * @return
	 */
	protected URIBuilder prepareUrl(URIBuilder builder) {
		return preparer.prepareUrl(builder);
	}

	private String urlSchema = "https";

	/**
	 * uri schema NOT db schema, should be "http" or "https"
	 * 
	 * @return
	 */
	public String getURLSchema() {
		return urlSchema;
	}

	public void setURLSchema(String urlSchema) {
		this.urlSchema = urlSchema;
	}

	public abstract String getDbName();

	public int getPort() {
		return -1;
	}

	public abstract String getHost();

	public interface Preparer {
		public Request prepareRequest(Request request);

		public URIBuilder prepareUrl(URIBuilder builder);
	}

	public abstract class AbstractPreparer implements Preparer {
		public Request prepareRequest(Request request) {
			return request;
		}

		public URIBuilder prepareUrl(URIBuilder builder) {
			return builder;
		}

		/**
		 * add each paths into builder
		 * 
		 * @param builder
		 * @param paths
		 * @return builder
		 */
		public final URIBuilder appendPath(URIBuilder builder, String... paths) {

			StringBuilder sb = new StringBuilder(builder.getPath());
			for (String path : paths) {
				sb.append(path);
			}

			return builder.setPath(sb.toString());

		}

		/**
		 * the <code>param - value</code> pair will be added into parameters if
		 * the <code>condition</code> is true
		 * 
		 * @param builder
		 * @param param
		 * @param value
		 * @param condition
		 * @return builder
		 */
		public final URIBuilder addParamIf(URIBuilder builder, String param,
				String value, boolean condition) {
			if (condition) {
				return builder.addParameter(param, value);
			} else {
				return builder;
			}
		}
	}

}