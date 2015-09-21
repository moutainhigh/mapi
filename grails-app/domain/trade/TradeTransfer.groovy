package trade

class TradeTransfer extends TradeBase{
    String  submitType
    Long    customerOperId
    String  submitter
    String  submitIp

    static constraints = {
        submitType(maxSize: 32,inList: ['manual','automatic'])
        submitter(maxSize: 32)
        submitIp(maxSize: 20)
    }
}
