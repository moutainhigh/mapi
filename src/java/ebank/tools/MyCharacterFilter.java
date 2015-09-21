package ebank.tools;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MyCharacterFilter implements Filter {
    private Log log = LogFactory.getLog(getClass());
    private String charsetKeyName = "_input_charset";
    private Charset defaultCharset = Charset.forName("utf8");

    public void init(FilterConfig filterConfig) throws ServletException {
        String value = filterConfig.getInitParameter("charsetKeyName");
        if (value!=null && !"".equals(value)) {
            charsetKeyName = value;
        }
        value = filterConfig.getInitParameter("defaultCharset");
        if (value!=null && !"".equals(value)) {
            defaultCharset = Charset.forName(value);
        }
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        Charset inputCharset = getInputCharset( request );
        log.debug("input charset: " + inputCharset);
        if ( inputCharset!=null && !inputCharset.equals(defaultCharset) ) {
            request.setCharacterEncoding(inputCharset.name());
            resp.setCharacterEncoding(inputCharset.name());
        }
        chain.doFilter(req, resp);
    }

    public void destroy() {}

    private Charset getInputCharset(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString==null || "".equals(queryString)) return null;
        String[] kvs = queryString.split("&");
        for (int i = 0; i < kvs.length; i++) {
            String[] kv = kvs[i].split("=");
            if ( kv.length==2 && charsetKeyName.equals(kv[0]) ) {
                try {
                    return Charset.forName( kv[1] );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
