package ebank.tools;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class FormFunction {
    public static String BuildForm(Map<Object, Object> params, String key) {
        Map paramsNew = compressMap(params);
        StringBuffer form = new StringBuffer();
        return null;
    }

    public static boolean verifyMD5Sign(Map<Object, Object> params, String key) {
        String charset = (String) params.get("_input_charset");
        String sign = (String) params.get("sign");
        String sign_type = (String) params.get("sign_type");
        if (!"md5".equalsIgnoreCase(sign_type) &&
                (params.get("sign")==null || "".equals(params.get("sign")))) {
            return false;
        }
        return sign.equals( createMD5Sign(params, key, charset) );
    }

	/**
	 * 功能：生成签名结果
	 * @param params 要签名的数组
	 * @param key 安全校验码
     * @param charset 字符编码
	 * @return 签名结果字符串
	 */
	public static String createMD5Sign(Map<Object, Object> params, String key, String charset) {
        charset = (charset==null || "".equals(charset)) ? "utf8" : charset;
		String param_str = params2string( compressMap(params) );  //把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
		param_str = param_str + key;                     //把拼接后的字符串再与安全校验码直接连接起来
        String sign = null;
        try {
            sign = MD5Tool.md5(param_str.getBytes(charset));
            System.out.println("str: " + param_str + "\nmd5: " + sign);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return sign;
	}

    /**
     * 压缩参数表
     * @param params 参数
     * @return 压缩后的参数
     */
    public static Map<Object, Object> compressMap(Map<Object, Object> params) {
        Map<Object, Object> paramsNew = new HashMap<Object, Object>();
        for(Object key : params.keySet()) {
            Object value = params.get( key );
            if (value==null || "".equals(value) ||
                    "sign".equals(key) || "sign_type".equals(key)) {
                continue;
            }
            paramsNew.put(key, value);
        }
        return paramsNew;
    }

	/**
	 * 功能：把数组所有元素排序，并按照“参数=参数值”的模式用“&”字符拼接成字符串
	 * @param params 需要排序并参与字符拼接的参数组
	 * @return 拼接后字符串
	 */
	public static String params2string(Map<Object, Object> params){
        StringBuffer buffer = new StringBuffer();
        TreeMap<Object, Object> paramsSort = new TreeMap<Object, Object>(params);
        boolean first = true;
        for(Object key : paramsSort.keySet()) {
            Object value = paramsSort.get( key );
            if (first) { first = false; }
            else       { buffer.append('&'); }
            buffer.append(key).append('=').append(value);
        }
		return buffer.toString();
	}

    public static void main(String[] args) {
        // trade_no:tn04, sign_type:md5, partner:5005, return_type:xml, out_trade_no:otn04
        String key = "9g6a4f9a35fg9bae2793a85a17g37a3c8495g32bgc7ecbb21ega7d582bdb8cc8";
        Map<Object, Object> params = new HashMap<Object, Object>();
        params.put("out_customer_code", "100000000000055");
        params.put("_input_charset", "gbk");
        params.put("partner", "100000000000059");
        params.put("return_type", "xml");
        params.put("return_url", "http://localhost:9001/bind_result.jsp");
        // dbd9c352c20c3b1b78dbb05004d12861
        String md5 = createMD5Sign(params, key, "gbk");
        System.out.println("md5 = " + md5);
        params.put("sign_type", "md5");
        params.put("sign", "dbd9c352c20c3b1b78dbb05004d12861");
        System.out.println( verifyMD5Sign(params, key) );
        params.put("sign", "6c87abcd7d917dd51e98a46c0f6ad4f6");
        System.out.println( verifyMD5Sign(params, key) );
    }
}
