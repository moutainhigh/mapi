package gateway

class Gwpayments {
    String id
    String prid
    String paytype
    String paynum
    long payamount
    long amount
    String infromacct
    String intoacct
    String refnum
    int paysts
    Date paytime
    String channel
    String modes
    String recepit
    Date createdate
    String payinfo
    static constraints = {

    }

    static mapping = {
         version false
    }

}
