package trade

class TradeCharge extends TradeBase {
    Long    paymentRequestId
    String  addedMethod
    Long    backAmount
    String  fundingSource
    Boolean isCreditCard
    String  paymentIp

    static constraints = {
        addedMethod(maxSize: 16)
        fundingSource(maxSize: 16)
        paymentIp(maxSize: 20)
    }

    static mapping = {
    }
}
