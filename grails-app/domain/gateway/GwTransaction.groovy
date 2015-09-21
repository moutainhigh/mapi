package gateway

class GwTransaction {
//    ID	VARCHAR2(22 BYTE)	No	32
    String  id
//    GWORDERS_ID	VARCHAR2(22 BYTE)	Yes	1	订单流水
    GwOrder order
//    TRXNUM	VARCHAR2(128 BYTE)	No	2	交易号
    String  bankTransNo
//    TRXTYPE	VARCHAR2(4 BYTE)	No	3	交易类型  100 消费 200 撤销 300 退款
    String  transType
//    CHANNEL	VARCHAR2(2 BYTE)	No	4	交易通道[0:余额支付 1:网上银行]
    String  channel
//    PAYMENT_TYPE	VARCHAR2(2 BYTE)	No	5	划款类型[0:立即到账 1:担保]
    String  paymentType
//    PAYMODE	VARCHAR2(2 BYTE)	No	6	付款方式[0:借记 1:贷记 2:未知]
    String  cardType
//    AMOUNT	NUMBER	No	7	交易金额
    Long    amount
//    CURRENCY	VARCHAR2(4 BYTE)	No	8	币种
    String  currency
//    SERVICECODE	VARCHAR2(20 BYTE)	Yes	9	支付服务码
    String gwServiceCode
//    ACQUIRER_CODE	VARCHAR2(20 BYTE)	No	10	收单行编码
    String  acquirerCode
//    ACQUIRER_NAME	VARCHAR2(30 BYTE)	Yes	11	收单行名称
    String  acquirerName
//    ACQUIRER_MERCHANT	VARCHAR2(64 BYTE)	No	12	收单商户号
    String  acquirerMerchant
//    ACQUIRER_SEQ	VARCHAR2(128 BYTE)	Yes	13	收单流水
    String  acquirerSeq
//    ACQUIRER_DATE	VARCHAR2(8 BYTE)	Yes	14	收单返回日期
    String  acquirerDate
//    ACQUIRER_MSG	VARCHAR2(256 BYTE)	Yes	15	收单信息备注
    String  acquirerMsg
//    SUBMITDATES	VARCHAR2(20 BYTE)	No	16	收单提交时间
    String  submitTime
//    PAYER_IP	VARCHAR2(20 BYTE)	No	17	返回IP
    String  returnIp
//    REFNUM	VARCHAR2(128 BYTE)	Yes	18	参考号
    String  referenceNo
//    AUTHCODE	VARCHAR2(6 BYTE)	Yes	19	授权号
    String  authNo
//    FROMACCTID	VARCHAR2(22 BYTE)	Yes	20	付款人账户ID
    String  acquirerInnerAccountNo
//    FROMACCTNUM	VARCHAR2(256 BYTE)	No	21	付款账户名称
    String  acquirerInnerAccountName
//    BUYER_ID	VARCHAR2(22 BYTE)	Yes	22	收款人用户ID
    String  buyerCustomerNo
//    BUYER_NAME	VARCHAR2(64 BYTE)	Yes	23	收款人用户号
    String  buyerCode
//    PAYINFO	VARCHAR2(64 BYTE)	Yes	24	付款信息
    String payinfo
//    CREATEDATE	DATE	No	25	交易创建时间
    Date    dateCreated
//    CLOSEDATE	DATE	No	26	交易完成时间
    Date    completionTime
//    TRXSTS	VARCHAR2(3 BYTE)	No	27	交易状态[0:WAIT_TRADE, 1:TRADE_FINISHED,2:TRADE_FAILURE,3:TRADE_CLOSED]
    String  status
//    OPERS	VARCHAR2(10 BYTE)	Yes	28	操作员
    String  handleOperName
//    OPERDATE	DATE	No	29	处理时间
    Date    handleTime
//    VERSION	NUMBER	No	30	版本
//    TRXDESC	VARCHAR2(100 BYTE)	Yes	31	交易备注
    String  note

    static constraints = {
        order   nullable: true
    }

    static mapping = {
        table 'gwtrxs'
        cache   usage: 'read-only'
        version false
        id  generator:'assigned', type: 'string'
        order           column: 'GWORDERS_ID'
        bankTransNo     column: 'TRXNUM'
        transType       column: 'TRXTYPE'
        cardType        column: 'PAYMODE'
        gwServiceCode   column: 'SERVICECODE'
        submitTime      column: 'SUBMITDATES'
        returnIp        column: 'PAYER_IP'
        referenceNo     column: 'REFNUM'
        authNo          column: 'AUTHCODE'
        acquirerInnerAccountNo      column: 'FROMACCTID'
        acquirerInnerAccountName    column: 'FROMACCTNUM'
        buyerCustomerNo column: 'BUYER_ID'
        buyerCode       column: 'BUYER_NAME'
        dateCreated     column: 'CREATEDATE'
        completionTime  column: 'CLOSEDATE'
        status          column: 'TRXSTS'
        handleOperName  column: 'OPERS'
        handleTime      column: 'OPERDATE'
        note            column: 'TRXDESC'
    }

    static transTypeMap = ['100':'消费', '200':'撤销', '300':'退款']
    static channelMap   = ['0':'余额支付', '1':'网上银行']
    static paymentTypeMap = ['0':'立即到账', '1':'担保']
    static cardTypeMap = ['0':'借记卡', '1':'信用卡', '2':'未知']
    static statusMap = ['0':'等待支付', '1':'支付完成', '2':'支付失败', '3':'有效期内未付']
}
