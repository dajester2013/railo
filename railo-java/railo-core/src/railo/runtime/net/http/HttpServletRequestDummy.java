package railo.runtime.net.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import railo.commons.collections.HashTable;
import railo.commons.io.IOUtil;
import railo.commons.io.res.Resource;
import railo.commons.lang.Pair;
import railo.runtime.config.Config;
import railo.runtime.exp.PageException;
import railo.runtime.op.Caster;
import railo.runtime.op.date.DateCaster;
import railo.runtime.type.Array;
import railo.runtime.type.KeyImpl;
import railo.runtime.type.Struct;
import railo.runtime.type.StructImpl;
import railo.runtime.type.dt.DateTimeImpl;
import railo.runtime.type.it.ItAsEnum;
import railo.runtime.util.EnumerationWrapper;

public final class HttpServletRequestDummy implements HttpServletRequest,Serializable {
	

	private Cookie[] cookies;
	
	private String authType;
	private Pair<String,Object>[] headers=new Pair[0];
	private Pair<String,Object>[] parameters=new Pair[0];
	private Struct attributes=new StructImpl();
	private String method="GET";
	private String pathInfo;
	private String pathTranslated;
	private String contextPath="/";
	private String queryString;
	private String remoteUser;
	private String requestedSessionId;
	private String requestURI;

	private String protocol="HTTP/1.1";
	private String serverName="localhost";
	private int port=80;

	private String characterEncoding="ISO-8859-1";

	private String contentType;
	private byte[] inputData=new byte[0];


	private static InetAddress DEFAULT_REMOTE;
	private static String DEFAULT_REMOTE_ADDR;
	private static String DEFAULT_REMOTE_HOST;
	static {
		try {
			DEFAULT_REMOTE=InetAddress.getLocalHost();
			DEFAULT_REMOTE_ADDR=DEFAULT_REMOTE.getHostAddress();
			DEFAULT_REMOTE_HOST=DEFAULT_REMOTE.getHostName();
		} 
		catch (UnknownHostException e) {}
	}
	//private InetAddress remoteq=DEFAULT_REMOTE;
	private String remoteAddr=DEFAULT_REMOTE_ADDR;
	private String remoteHost=DEFAULT_REMOTE_HOST;

	private Locale locale=Locale.getDefault();

	private boolean secure;

	private Resource contextRoot;

	private String scheme="http";

	private HttpSession session;



	/**
	 * constructor of the class
	 * @param headers 
	 * @param parameters 
	 * @param httpSession 
	 * @param pairs 
	 * @param cookiess 
	 */
	public HttpServletRequestDummy(Resource contextRoot,String serverName, String scriptName,String queryString, 
			Cookie[] cookies, Pair[] headers, Pair[] parameters, Struct attributes, HttpSession session) {
		this.serverName=serverName;
		requestURI=scriptName;
		this.queryString=queryString;
		this.parameters=translateQS(queryString);
		this.contextRoot=contextRoot;
		if(cookies!=null)setCookies(cookies);
		if(headers!=null)this.headers=headers;
		if(parameters!=null)this.parameters=parameters;
		if(attributes!=null)this.attributes=attributes;
		this.session=session;
	}
	/**
	 * constructor of the class
	 * @throws PageException
	 * /
	public HttpServletRequestDummy(String serverName, String scriptName,Struct queryString) throws PageException {
		this.serverName=serverName;
		requestURI=scriptName;
		
		StringBuffer qs=new StringBuffer();
		String[] keys=queryString.keys();
		parameters=new Item[keys.length];
		String key;
		Object value;
		for(int i=0;i<keys.length;i++) {
			if(i>0) qs.append('&');
			key=keys[i];
			value=queryString.get(key);
			parameters[i]=new Item(key,value);
			
			qs.append(key);
			qs.append('=');
			qs.append(Caster.toString(value));
		}
		
		this.queryString=qs.toString();
	}*/
	
	private Pair[] translateQS(String qs) {
        if(qs==null) return new Pair[0];
        Array arr=railo.runtime.type.List.listToArrayRemoveEmpty(qs,"&");
        Pair[] parameters=new Pair[arr.size()];
        //Array item;
        int index;
        String name;
        
        for(int i=1;i<=parameters.length;i++) {
            name=Caster.toString(arr.get(i,""),"");
            index=name.indexOf('=');
            if(index!=-1) parameters[i-1]=new Pair(name.substring(0,index),name.substring(index+1));
            else parameters[i-1]=new Pair(name,"");
              
        }
        return parameters;
    }
	
	

	/** 
	 * @see javax.servlet.http.HttpServletRequest#getAuthType()
	 */
	public String getAuthType() {
		return authType;
	}
	
	/**
	 * sets the name of the authentication scheme used to protect the servlet. 
	 * All servlet containers support basic, 
	 * form and client certificate authentication, 
	 * and may additionally support digest authentication. 
	 * @param authType authentication type
	 */
	public void setAuthType(String authType) {
		this.authType=authType;
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getCookies()
	 */
	public Cookie[] getCookies() {
		return cookies;
	}
	
	/**
	 * sets an array containing all of the Cookie objects 
	 * the client sent with this request. 
	 * This method returns null if no cookies were sent.
	 * @param cookies
	 */
	public void setCookies(Cookie[] cookies) {
		this.cookies=cookies;
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
	 */
	public long getDateHeader(String name) {
		Object value=getHeader(name);
		if(value!=null) {
			Date date=DateCaster.toDateAdvanced(value,null,null);
			if(date!=null)return date.getTime();
			throw new IllegalArgumentException("can't convert value "+value+" to a Date");
		}
		return -1;
	}
	
	public void setDateHeader(String name, long value) {
		// TODO wrong format
		setHeader(name,new DateTimeImpl(value,false).castToString());
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getHeader(java.lang.String)
	 */
	public String getHeader(String name) {
		return ReqRspUtil.get(headers,name);
	}
	
	/**
	 * sets a new header value
	 * @param name name of the new value
	 * @param value header value
	 */ 
	public void setHeader(String name, String value) {
		headers=ReqRspUtil.set(headers,name,value);
	}
	
	/**
	 * add a new header value
	 * @param name name of the new value
	 * @param value header value
	 */ 
	public void addHeader(String name, String value) {
		headers=ReqRspUtil.add(headers,name,value);
	}
	
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getHeaders(java.lang.String)
	 */
	public Enumeration getHeaders(String name) {
		HashSet set=new HashSet();
		for(int i=0;i<headers.length;i++) {
			if(headers[i].getName().equalsIgnoreCase(name))
				set.add(Caster.toString(headers[i].getValue(),null));
		}
		return new EnumerationWrapper(set);
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
	 */
	public Enumeration getHeaderNames() {
		HashSet set=new HashSet();
		for(int i=0;i<headers.length;i++) {
			set.add(headers[i].getName());
		}
		return new EnumerationWrapper(set);
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
	 */
	public int getIntHeader(String name) {
		Object value=getHeader(name);
		if(value!=null) {
			try {
				return Caster.toIntValue(value);
			} catch (PageException e) {
				throw new NumberFormatException(e.getMessage());
			}
		}
		return -1;
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getMethod()
	 */
	public String getMethod() {
		return method;
	}
	
	/**
	 * sets the request method
	 * @param method
	 */
	public void setMethod(String method) {
		this.method = method;
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getPathInfo()
	 */
	public String getPathInfo() {
		return pathInfo;
	}
	
	
	/**
	 * Sets any extra path information associated with the URL the client sent 
	 * when it made this request. 
	 * The extra path information follows the servlet path but precedes 
	 * the query string. 
	 * @param pathInfo
	 */
	public void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}
	
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
	 */
	public String getPathTranslated() {
		return pathTranslated;
	}

	/**
	 * sets any extra path information after the servlet name 
	 * but before the query string, translates to a real path. 
	 * Same as the value of the CGI variable PATH_TRANSLATED. 
	 * @param pathTranslated
	 */
	public void setPathTranslated(String pathTranslated) {
		// TODO muss auf pathinfo basieren
		this.pathTranslated = pathTranslated;
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getContextPath()
	 */
	public String getContextPath() {
		return contextPath;
	}
	
	/**
	 * sets the portion of the request URI that indicates the context of the request. 
	 * The context path always comes first in a request URI. 
	 * The path starts with a "/" character but does not end with a "/" character. 
	 * @param contextPath
	 */
	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getQueryString()
	 */
	public String getQueryString() {
		return queryString;
	}
	
	/**
	 * sets the query string that is contained in the request URL after the path. 
	 * Same as the value of the CGI variable QUERY_STRING.

	 * @param queryString
	 */
	public void setQueryString(String queryString) {
		this.queryString = queryString;
		parameters=translateQS(queryString);
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
	 */
	public String getRemoteUser() {
		return remoteUser;
	}
	
	/**
	 * sets the login of the user making this request, 
	 * if the user has been authenticated, 
	 * or null if the user has not been authenticated. 
	 * Whether the user name is sent with each subsequent request depends 
	 * on the browser and type of authentication. 
	 * Same as the value of the CGI variable REMOTE_USER.
	 * @param remoteUser
	 */
	public void setRemoteUser(String remoteUser) {
		this.remoteUser = remoteUser;
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
	 */
	public boolean isUserInRole(String role) {
		// TODO impl
		return false;
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	public Principal getUserPrincipal() {
		 //TODO impl
		return null;
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
	 */
	public String getRequestedSessionId() {
		return requestedSessionId;
	}
	
	/**
	 * sets the session ID specified by the client. 
	 * This may not be the same as the ID of the actual session in use. 
	 * For example, if the request specified an old (expired) session ID 
	 * and the server has started a new session, 
	 * this method gets a new session with a new ID. 
	 * @param requestedSessionId
	 */
	public void setRequestedSessionId(String requestedSessionId) {
		this.requestedSessionId = requestedSessionId;
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getRequestURI()
	 */
	public String getRequestURI() {
		return requestURI;
	}

	/**
	 * sets the part of this request's URL from the protocol name 
	 * up to the query string in the first line of the HTTP request. 
	 * The web container does not decode this String.
	 * @param requestURI
	 */
	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getRequestURL()
	 */
	public StringBuffer getRequestURL() {
		return new StringBuffer(isSecure()?"https":"http").
			append("://").
			append(serverName).
			append(':').
			append(port).
			append('/').
			append(requestURI);
	}
	
	/**
	 * @see javax.servlet.http.HttpServletRequest#getServletPath()
	 */
	public String getServletPath() {
		// TODO when different ?
		return requestURI;
	}
	/**
	 * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
	 */
	public HttpSession getSession(boolean arg0) {
		return session;
	}
	/**
	 * @see javax.servlet.http.HttpServletRequest#getSession()
	 */
	public HttpSession getSession() {
		return getSession(true);
	}
	/**
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
	 */
	public boolean isRequestedSessionIdValid() {
//		 not supported
		return false;
	}
	/**
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
	 */
	public boolean isRequestedSessionIdFromCookie() {
//		 not supported
		return false;
	}
	/**
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
	 */
	public boolean isRequestedSessionIdFromURL() {
//		 not supported
		return false;
	}
	/**
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
	 */
	public boolean isRequestedSessionIdFromUrl() {
		return isRequestedSessionIdFromURL();
	}
	/**
	 * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String key) {
		return attributes.get(key,null);
	}
	
	/**
	 * @see javax.servlet.ServletRequest#setAttribute(java.lang.String, java.lang.Object)
	 */
	public void setAttribute(String key, Object value) {
		attributes.setEL(key,value);
	}
	
	/**
	 * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String key) {
		attributes.removeEL(KeyImpl.init(key));
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getAttributeNames()
	 */
	public Enumeration getAttributeNames() {
		return ItAsEnum.toStringEnumeration(attributes.keyIterator());
	}
	/**
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 */
	public String getCharacterEncoding() {
		return characterEncoding;
	}
	/**
	 * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
	 */
	public void setCharacterEncoding(String characterEncoding)
			throws UnsupportedEncodingException {
		this.characterEncoding=characterEncoding;
	}
	/**
	 * @see javax.servlet.ServletRequest#getContentLength()
	 */
	public int getContentLength() {
		return -1;
	}
	/**
	 * @see javax.servlet.ServletRequest#getContentType()
	 */
	public String getContentType() {
		return contentType;
	}
	
	/**
	 * sets the content Type of the Request
	 * @param contentType
	 */
	public void setContentType(String contentType) {
		this.contentType=contentType;
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getInputStream()
	 */
	public ServletInputStream getInputStream() throws IOException {
		return new ServletInputStreamDummy(inputData);
	}

	public void setParameter(String key,String value) {
		parameters=ReqRspUtil.set(parameters,key,value);
		rewriteQS();
	}

	public void addParameter(String key,String value) {
		parameters=ReqRspUtil.add(parameters,key,value);
		rewriteQS();
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
	 */
	public String getParameter(String key) {
		return ReqRspUtil.get(parameters,key);
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
	 */
	public String[] getParameterValues(String key) {
		ArrayList list=new ArrayList();
		for(int i=0;i<parameters.length;i++) {
			if(parameters[i].getName().equalsIgnoreCase(key))
				list.add(Caster.toString(parameters[i].getValue(),null));
		}
		return (String[]) list.toArray(new String[list.size()]);
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getParameterNames()
	 */
	public Enumeration getParameterNames() {
		HashSet set=new HashSet();
		for(int i=0;i<parameters.length;i++) {
			set.add(parameters[i].getName());
		}
		return new EnumerationWrapper(set);
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getParameterMap()
	 */
	public Map getParameterMap() {
		Map p=new HashTable(); 
		for(int i=0;i<parameters.length;i++) {
			p.put(parameters[i].getName(), parameters[i].getValue());
		}
		return p;
	}

	/**
	 * set the Protocol (Default "http")
	 * @param protocol
	 */
	public void setProtocol(String protocol) {
		this.protocol=protocol;
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getProtocol()
	 */
	public String getProtocol() {
		return protocol;
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getScheme()
	 */
	public String getScheme() {
		return scheme;
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getScheme()
	 */
	public void setScheme(String scheme) {
		this.scheme=scheme;
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getServerName()
	 */
	public String getServerName() {
		return serverName;
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getServerPort()
	 */
	public int getServerPort() {
		return port;
	}	
	
	/**
	 * @param port The port to set.
	 */
	public void setServerPort(int port) {
		this.port = port;
	}
	
    /**
     * @see javax.servlet.ServletRequest#getReader()
     */
    public BufferedReader getReader() throws IOException {
        return IOUtil.toBufferedReader(IOUtil.getReader(getInputStream(),"ISO-8859-1"));
    }

	/**
	 * @see javax.servlet.ServletRequest#getRemoteAddr()
	 */
	public String getRemoteAddr() {
		return remoteAddr;
	}

	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr=remoteAddr;
	}
	public void setRemoteHost(String remoteHost) {
		this.remoteHost=remoteHost;
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getRemoteHost()
	 */
	public String getRemoteHost() {
		return remoteHost;
	}
	
	public void setRemoteInetAddress(InetAddress ia) {
		setRemoteAddr(ia.getHostAddress());
		setRemoteHost(ia.getHostName());
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getLocale()
	 */
	public Locale getLocale() {
		return locale;
	}
	
	public void setLocale(Locale locale) {
		this.locale=locale;
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getLocales()
	 */
	public Enumeration getLocales() {
		return new EnumerationWrapper(Locale.getAvailableLocales());
	}
	
	/**
	 * @see javax.servlet.ServletRequest#isSecure()
	 */
	public boolean isSecure() {
		return secure;
	}
	
	public void setSecure(boolean secure) {
		this.secure=secure;
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
	 */
	public RequestDispatcher getRequestDispatcher(String arg0) {
		return new RequestDispatcherDummy(this);
	}
	
	/**
	 * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
	 */
	public String getRealPath(String path) {
		return contextRoot.getReal(path);
	}

	/**
	 * @return the inputData
	 */
	public byte[] getInputData() {
		return inputData;
	}
	/**
	 * @param inputData the inputData to set
	 */
	public void setInputData(byte[] inputData) {
		this.inputData = inputData;
	}

	private void rewriteQS() {
		StringBuffer qs=new StringBuffer();
		Pair p;
		for(int i=0;i<parameters.length;i++) {
			if(i>0) qs.append('&');
			p=parameters[i];
			qs.append(p.getName());
			qs.append('=');
			qs.append(Caster.toString(p.getValue(),""));
		}
		queryString=qs.toString();
	}
	public void setSession(HttpSession session) {
		this.session=session;
	}
	public static HttpServletRequestDummy clone(Config config,Resource rootDirectory,HttpServletRequest req) {

		HttpServletRequestDummy dest = new HttpServletRequestDummy(
				rootDirectory,
				req.getServerName(),
				req.getRequestURI(),
				req.getQueryString(),
				HttpUtil.cloneCookies(config,req),
				HttpUtil.cloneHeaders(req),
				HttpUtil.cloneParameters(req),
				HttpUtil.getAttributesAsStruct(req),
				getSessionEL(req)
			);
		

		try {
			dest.setCharacterEncoding(req.getCharacterEncoding());
		} catch (Exception e) {
			
		}
		
		dest.setRemoteAddr(req.getRemoteAddr());
		dest.setRemoteHost(req.getRemoteHost());
		dest.setAuthType(req.getAuthType());
		dest.setContentType(req.getContentType());
		dest.setContextPath(req.getContextPath());
		dest.setLocale(req.getLocale());
		dest.setMethod(req.getMethod());
		dest.setPathInfo(req.getPathInfo());
		dest.setProtocol(req.getProtocol());
		dest.setRequestedSessionId(req.getRequestedSessionId());
		dest.setScheme(req.getScheme());
		dest.setServerPort(req.getServerPort());
		dest.setSession(getSessionEL(req));
		return dest;
	}
	private static HttpSession getSessionEL(HttpServletRequest req) {
		try{
			return req.getSession();
		}
		catch(Throwable t){}
		return null;
	}
	public void setAttributes(Struct attributes) {
		this.attributes=attributes;
	}
	
}
