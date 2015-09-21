package ebank.tools;

import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class StringUtil {
    public static String getRandomNum(int maxValue) {
        Random random = new Random();
        return random.nextInt(maxValue) + "";
    }

    public static Long parseAmountFromStr(String str) {
        BigDecimal bd = new BigDecimal( str );
        bd = bd.movePointRight(2);
        bd = bd.add(new BigDecimal(0.5));
        return bd.longValue();
    }
    public static String getAmountFromNum(String number) {
        BigDecimal bd = new BigDecimal( number );
        bd = bd.movePointLeft(2);
        return getAmountFromNum(bd);
    }
    public static String getAmountFromNum(Long number) {
        BigDecimal bd = new BigDecimal( number );
        bd = bd.movePointLeft(2);
        return getAmountFromNum(bd);
    }
    public static String getAmountFromNum(BigDecimal number) {
        DecimalFormat df = new DecimalFormat("#0.00");
        return df.format(number);
    }

    public static String getNumericDate() {
        return getNumericDate(new Date());
    }
    public static String getNumericDate(Date date) {
        return formatDateTime(date, "yyyyMMdd");
    }
    public static String getNumericTime() {
        return getNumericTime(new Date());
    }
    public static String getNumericTime(Date date) {
        return formatDateTime(date, "HHmmss");
    }
    public static String getNumericDateTime() {
        return getNumericDateTime(new Date());
    }
    public static String getNumericDateTime(Date date) {
        return formatDateTime(date, "yyyyMMddHHmmss");
    }

    public static String getFullDate() {
        return getFullDate(new Date());
    }
    public static String getFullDate(Date date) {
        return formatDateTime(date, "yyyy-MM-dd");
    }
    public static String getFullTime() {
        return getFullTime(new Date());
    }
    public static String getFullTime(Date date) {
        return formatDateTime(date, "HH:mm:ss");
    }
    public static String getFullDateTime() {
        return getFullDateTime(new Date());
    }
    public static String getFullDateTime(Date date) {
        return formatDateTime(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static String formatDateTime(Date date, String pattern) {
        DateFormat df=new SimpleDateFormat(pattern);
        return df.format(date);
    }

    public static String[] splitAllTokens(String value, String re) {
        return StringUtils.splitPreserveAllTokens(value, re);
    }

    public static void main(String[] args) {
        System.out.println("parseAmountFromStr(\"0.99\") = " + parseAmountFromStr("0.99"));
        System.out.println("parseAmountFromStr(\"12\") = " + parseAmountFromStr("12"));
        System.out.println("parseAmountFromStr(\"8.1\") = " + parseAmountFromStr("8.1"));
        System.out.println("parseAmountFromStr(\"8.555\") = " + parseAmountFromStr("8.555"));
        System.out.println("getAmountFromNum(99L) = " + getAmountFromNum(99L));
        System.out.println("getAmountFromNum(\"99\") = " + getAmountFromNum("99"));
        System.out.println("getAmountFromNum((810L)) = " + getAmountFromNum((810L)));
        System.out.println("getAmountFromNum(1000L) = " + getAmountFromNum(1000L));
        System.out.println("getRandomNum(9999) = " + getRandomNum(9999));
    }
}
