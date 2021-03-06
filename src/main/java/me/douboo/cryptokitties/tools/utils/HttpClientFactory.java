package me.douboo.cryptokitties.tools.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.douboo.cryptokitties.tools.exceptions.ClientCreateException;
import me.douboo.cryptokitties.tools.exceptions.ClientInvokerException;

/**
 * httpClient工厂类
 * 
 * @author luheng
 * @version v01.00.00 $Revision$
 * @date 2015年4月18日
 * @time 下午2:48:39
 */
public class HttpClientFactory {
	private static final Logger logger = LoggerFactory.getLogger(HttpClientFactory.class);

	// 请求超时时间
	private int requestTimeout = 30000;

	// 连接超时时间
	private int connectTimeout = 30000;

	// 网络超时时间
	private int socketTimeout = 30000;

	// 最大连接数
	private int maxTotal = 200;

	private Integer retry = 3;

	public static void putMsg(final String url,final Map<String,Object> p) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					createSSLInstance().doPut(url,p);
				} catch (Exception e) {
				}
			}
		}).start();
	}

	// 请求头集合
	private Map<String, String> headers = new HashMap<String, String>();

	public int getRequestTimeout() {
		return requestTimeout;
	}

	public int getMaxTotal() {
		return maxTotal;
	}

	public void setMaxTotal(int maxTotal) {
		this.maxTotal = maxTotal;
	}

	public RequestConfig getRequestConfig() {
		return requestConfig;
	}

	public void setRequestConfig(RequestConfig requestConfig) {
		this.requestConfig = requestConfig;
	}

	public void setRequestTimeout(int requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	// 默认请求头accept信息
	private String accept = "text/html;charset=Utf-8,application/json";
	private String contentType = "application/x-www-form-urlencoded; charset=UTF-8";
	private String userAgent = "Mozilla/5.0 (UTOUU)";
	private String XRequestWith = "XMLHttpRequest";

	public String getAccept() {
		return accept;
	}

	public void setAccept(String accept) {
		this.accept = accept;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public Integer getRetry() {
		return retry;
	}

	public void setRetry(Integer retry) {
		this.retry = retry;
	}

	// public static String cookie;
	private RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(this.requestTimeout)
			.setConnectTimeout(this.connectTimeout).setSocketTimeout(this.socketTimeout).build();; // 创建终端
	private ConnectionConfig connectionConfig = ConnectionConfig.custom().setCharset(Charset.defaultCharset()).build();
	private CloseableHttpClient client = createClient();

	private HttpClientFactory() {
	};

	public ConnectionConfig getConnectionConfig() {
		return connectionConfig;
	}

	public void setConnectionConfig(ConnectionConfig connectionConfig) {
		this.connectionConfig = connectionConfig;
	}

	public CloseableHttpClient getClient() {
		return client;
	}

	public void setClient(CloseableHttpClient client) {
		this.client = client;
	}

	/**
	 * 从工厂类生产实例
	 * 
	 * @author luheng
	 * @return
	 */
	public static HttpClientFactory createInstance() {
		HttpClientFactory instance = new HttpClientFactory();
		return instance;
	}

	public static HttpClientFactory createSSLInstance() {
		HttpClientFactory instance = new HttpClientFactory();
		instance.setClient(instance.createSSLClientDefault());
		return instance;
	}

	/**
	 * 创建默认终端http
	 * 
	 * @author luheng
	 * @return
	 */
	public CloseableHttpClient createClient() {
		// HttpClient4.x可以自带维持会话功能，只要使用同一个HttpClient且未关闭连接
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
		connManager.setMaxTotal(this.maxTotal);
		connManager.setDefaultMaxPerRoute(connManager.getMaxTotal());
		CloseableHttpClient client = HttpClients.custom()
				.setRetryHandler(new DefaultHttpRequestRetryHandler(retry, true)).setConnectionManager(connManager)
				.build();
		return client;
	}

	/**
	 * 创建支持请求SSL服务的客户端
	 * 
	 * @author luheng
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 */
	public CloseableHttpClient createSSLClientDefault() {
		// 允许所有ssl
		SSLContext sslContext;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
				@Override
				public boolean isTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws java.security.cert.CertificateException {
					return true;
				}
			}).build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw new ClientCreateException("ssl http client 创建失败!");
		}
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
		return HttpClients.custom().setSSLSocketFactory(sslsf).setMaxConnTotal(this.maxTotal)
				.setMaxConnPerRoute(this.maxTotal).setRetryHandler(new DefaultHttpRequestRetryHandler(retry, true))
				.setDefaultConnectionConfig(connectionConfig).build();
	}

	/**
	 * 执行带client的post请求
	 * 
	 * @param uri
	 * @param nameValuePairs
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpException
	 */
	public String doPost(CloseableHttpClient client, String uri, List<NameValuePair> nameValuePairs) {
		// 配置URI
		HttpPost post = new HttpPost(uri);
		this.initPostHeaders(post);

		// 传参
		if (null != nameValuePairs && nameValuePairs.size() > 0) {
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs, Consts.UTF_8);
			post.setEntity(entity);
		}

		// 执行请求
		try {
			CloseableHttpResponse response = client.execute(post);
			String result = returnStringRes(response);
			return result;
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw new ClientInvokerException(e);
		}

	}

	/**
	 * 执行带client的get请求
	 * 
	 * @author luheng
	 * @param client
	 * @param uri
	 * @param nameValuePairs
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String doGet(CloseableHttpClient client, String uri, List<NameValuePair> nameValuePairs) {
		// 配置URI
		HttpGet get = new HttpGet(uri);

		// 传参
		if (null != nameValuePairs && nameValuePairs.size() > 0) {

			for (int i = 0; i < nameValuePairs.size(); i++) {
				NameValuePair nameValuePair = nameValuePairs.get(i);
				if (i == 0)
					uri += "?";
				else
					uri += "&";
				uri += nameValuePair.getName() + "=" + nameValuePair.getValue();
			}
			get = new HttpGet(uri);
		}

		this.initGetHeaders(get);

		// 执行请求
		try {
			CloseableHttpResponse response = client.execute(get);
			return returnStringRes(response);
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw new ClientInvokerException(e);
		}
	}

	/**
	 * 执行post请求
	 * 
	 * @param uri
	 * @param nameValuePairs
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpException
	 */
	public String doPost(String uri, List<NameValuePair> nameValuePairs) {
		// 配置URI
		HttpPost post = new HttpPost(uri);
		this.initPostHeaders(post);

		// 传参
		if (null != nameValuePairs && nameValuePairs.size() > 0) {
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs, Consts.UTF_8);
			post.setEntity(entity);
		}

		// 执行请求
		try {
			CloseableHttpResponse response = client.execute(post);
			return returnStringRes(response);
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw new ClientInvokerException(e);
		}
	}

	/**
	 * 执行post请求
	 * 
	 * @param uri
	 * @param nameValuePairs
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpException
	 */
	public String doPost(String uri, Map<String, Object> map) {
		List<NameValuePair> params = map2NameValuePair(map);

		return this.doPost(uri, params);
	}

	/**
	 * 执行get请求
	 * 
	 * @param uri
	 * @param nameValuePairs
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpException
	 */
	public String doGet(String uri, Map<String, Object> map) {
		List<NameValuePair> params = map2NameValuePair(map);
		return this.doGet(uri, params);
	}

	/**
	 * 执行doPut请求
	 * 
	 * @author luheng
	 * @param uri
	 * @param map
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String doPut(String uri, Map<String, Object> map) {
		List<NameValuePair> params = map2NameValuePair(map);

		return this.doPut(uri, params);
	}

	// Map转List<NameValuePair>
	private List<NameValuePair> map2NameValuePair(Map<String, Object> map) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		if (null != map && map.size() > 0)
			for (String key : map.keySet()) {
				Object val = map.get(key);
				String valStr = "";
				if (null != val)
					valStr = String.valueOf(val);
				params.add(new BasicNameValuePair(key, valStr));
			}
		return params;
	}

	/**
	 * 执行put请求
	 * 
	 * @param uri
	 * @param nameValuePairs
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpException
	 */
	public String doPut(String uri, List<NameValuePair> nameValuePairs) {
		// 配置URI
		HttpPut httpPut = new HttpPut(uri);
		this.initPostHeaders(httpPut);

		// 传参
		if (null != nameValuePairs && nameValuePairs.size() > 0) {
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs, Consts.UTF_8);
			httpPut.setEntity(entity);
		}

		// 执行请求
		try {
			CloseableHttpResponse response = client.execute(httpPut);
			return returnStringRes(response);
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw new ClientInvokerException(e);
		}
	}

	/**
	 * 执行delete请求
	 * 
	 * @param uri
	 * @param nameValuePairs
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpException
	 */
	public String doDelete(String uri) {
		// 配置URI
		HttpDelete httpDelete = new HttpDelete(uri);
		this.initPostHeaders(httpDelete);

		try {
			// 执行请求
			CloseableHttpResponse response = client.execute(httpDelete);
			// 获取状态码
			return returnStringRes(response);
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw new ClientInvokerException(e);
		}
	}

	// 返回字符串
	private String returnStringRes(CloseableHttpResponse response) throws IOException, HttpResponseException {
		StatusLine statusLine = response.getStatusLine();
		String result = "";
		if (statusLine.getStatusCode() >= 200 && statusLine.getStatusCode() < 300) {
			// 获取返回实体
			HttpEntity responseEntity = response.getEntity();
			result = EntityUtils.toString(responseEntity, Consts.UTF_8);
			// 释放资源，关闭流
			EntityUtils.consume(responseEntity);
			response.close();
		} else {
			throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
		}
		return result;
	}

	/**
	 * 执行post请求
	 * 
	 * @param uri
	 * @param nameValuePairs
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpException
	 */
	public CloseableHttpResponse doPost(CloseableHttpClient client, String uri, NameValuePair[] nameValuePairs) {
		// 配置URI
		HttpPost post = new HttpPost(uri);
		this.initPostHeaders(post);

		// 传参
		if (null != nameValuePairs && nameValuePairs.length > 0) {
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(Arrays.asList(nameValuePairs), Consts.UTF_8);
			post.setEntity(entity);
		}

		// 执行请求
		try {
			return client.execute(post);
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw new ClientInvokerException(e);
		}
	}

	/**
	 * 添加请求头
	 * 
	 * @author luheng
	 */
	public void addPostHeader(String headerName, String headerValue) {
		this.headers.put(headerName, headerValue);
	}

	// 初始化post请求头
	private void initPostHeaders(HttpEntityEnclosingRequestBase http) {
		http.setConfig(requestConfig);
		if (StringUtils.isNotEmpty(this.accept))
			http.setHeader(HttpHeaders.ACCEPT, this.accept);
		if (StringUtils.isNotEmpty(this.contentType))
			http.setHeader(HttpHeaders.CONTENT_TYPE, this.contentType);
		if (StringUtils.isNotEmpty(this.userAgent))
			http.setHeader(HttpHeaders.USER_AGENT, this.userAgent);
		if (StringUtils.isNotEmpty(this.XRequestWith))
			http.setHeader("X-Requested-With", this.XRequestWith);
		for (String headerName : this.headers.keySet()) {
			http.setHeader(headerName, this.headers.get(headerName));
		}
	}

	// 初始化post请求头
	private void initPostHeaders(HttpRequestBase http) {
		http.setConfig(requestConfig);
		if (StringUtils.isNotEmpty(this.accept))
			http.setHeader(HttpHeaders.ACCEPT, this.accept);
		if (StringUtils.isNotEmpty(this.contentType))
			http.setHeader(HttpHeaders.CONTENT_TYPE, this.contentType);
		if (StringUtils.isNotEmpty(this.userAgent))
			http.setHeader(HttpHeaders.USER_AGENT, this.userAgent);
		if (StringUtils.isNotEmpty(this.XRequestWith))
			http.setHeader("X-Requested-With", this.XRequestWith);
		for (String headerName : this.headers.keySet()) {
			http.setHeader(headerName, this.headers.get(headerName));
		}
	}

	/**
	 * 执行post请求
	 * 
	 * @param uri
	 * @param nameValuePairs
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpException
	 */
	public String doPost(String uri, NameValuePair[] nameValuePairs) {
		CloseableHttpResponse response = this.doPost(this.client, uri, nameValuePairs);
		try {
			return returnStringRes(response);
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw new ClientInvokerException(e);
		}
	}

	/**
	 * 执行put请求
	 * 
	 * @author luheng
	 * @param uri
	 * @param nameValuePairs
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public String doPut(String uri, NameValuePair[] nameValuePairs) throws ParseException, IOException {
		CloseableHttpResponse response = this.doPut(this.client, uri, nameValuePairs);
		String result = returnStringRes(response);
		return result;
	}

	/**
	 * 执行put请求
	 * 
	 * @author luheng
	 * @param client2
	 * @param uri
	 * @param nameValuePairs
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public CloseableHttpResponse doPut(CloseableHttpClient client2, String uri, NameValuePair[] nameValuePairs) {
		// 配置URI
		HttpPut put = new HttpPut(uri);
		this.initPostHeaders(put);

		// 传参
		if (null != nameValuePairs && nameValuePairs.length > 0) {
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(Arrays.asList(nameValuePairs), Consts.UTF_8);
			put.setEntity(entity);
		}

		// 执行请求
		try {
			return this.client.execute(put);
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw new ClientInvokerException(e);
		}
	}

	/**
	 * 执行get请求
	 * 
	 * @author luheng
	 * @param uri
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String doGet(String uri) {
		// 配置URI
		HttpGet get = new HttpGet(uri);

		this.initGetHeaders(get);

		// 执行请求
		try {
			CloseableHttpResponse response = this.client.execute(get);
			return returnStringRes(response);
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw new ClientInvokerException(e);
		}
	}

	/**
	 * 执行get请求
	 * 
	 * @author luheng
	 * @param uri
	 * @param nameValuePairs
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String doGet(String uri, List<NameValuePair> nameValuePairs) {
		// 配置URI
		HttpGet get = new HttpGet(uri);

		// 传参
		if (null != nameValuePairs && nameValuePairs.size() > 0) {

			for (int i = 0; i < nameValuePairs.size(); i++) {
				NameValuePair nameValuePair = nameValuePairs.get(i);
				if (i == 0)
					uri += "?";
				else
					uri += "&";
				uri += nameValuePair.getName() + "=" + nameValuePair.getValue();
			}
			get = new HttpGet(uri);
		}

		this.initGetHeaders(get);

		// 执行请求
		try {
			CloseableHttpResponse response = this.client.execute(get);
			return returnStringRes(response);
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw new ClientInvokerException(e);
		}
	}

	// 初始化get请求头
	private void initGetHeaders(HttpGet get) {
		get.setConfig(requestConfig);
		if (StringUtils.isNotEmpty(this.accept))
			get.setHeader(HttpHeaders.ACCEPT, this.accept);
		if (StringUtils.isNotEmpty(this.contentType))
			get.setHeader(HttpHeaders.CONTENT_TYPE, this.contentType);
		if (StringUtils.isNotEmpty(this.userAgent))
			get.setHeader(HttpHeaders.USER_AGENT, this.userAgent);
		if (StringUtils.isNotEmpty(this.XRequestWith))
			get.setHeader("X-Requested-With", this.XRequestWith);
		for (String headerName : this.headers.keySet()) {
			get.setHeader(headerName, this.headers.get(headerName));
		}
	}

	/**
	 * 执行post请求
	 * 
	 * @param uri
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String doPost(String uri) {
		return doPost(uri, new ArrayList<NameValuePair>());
	}

}
