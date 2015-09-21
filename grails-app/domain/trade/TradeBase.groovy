package trade

class TradeBase {
    Long    rootId
    Long    originalId
    String  tradeNo
    String  tradeType
    Long    partnerId
    Long    payerId
    String  payerCode
    String  payerName
    String  payerAccountNo
    Long    payeeId
    String  payeeCode
    String  payeeName
    String  payeeAccountNo
    String  outTradeNo
    Long    amount      = 0
    Long    feeAmount   = 0
    String  currency    = 'CNY'
    String  subject
    String  status      = 'starting'
    Integer tradeDate
    String  note
    Date    dateCreated
    Date    lastUpdated

    static constraints = {
        rootId nullable: true
        originalId  nullable: true
        tradeNo(maxSize:36,blank: false)
        tradeType maxSize: 16, inList: ['payment', 'transfer', 'refund',
                'charge', 'withdrawn', 'royalty', 'royalty_rfd', 'frozen', 'unfrozen']
        partnerId(nullable: true)
        payerId(nullable: true)
        payerCode(maxSize: 64, nullable: true)
        payerAccountNo(maxSize: 24,blank:false)
        payeeId(nullable: true)
        payeeCode(maxSize: 64,nullable: true)
        payeeAccountNo(maxSize: 24,blank:false)
        outTradeNo(maxSize: 64,nullable: true)
        amount()
        feeAmount()
        currency(maxSize: 4,blank:false)
        subject(maxSize: 256, nullable: true)
        status maxSize: 16, inList: ['starting','processing','completed','closed']
        tradeDate()
        note maxSize: 64, nullable: true
    }

    static mapping = {
        id generator: 'sequence', params: [sequence: 'seq_trade']
        tablePerHierarchy false
    }

    def afterInsert () {
        if (!rootId) {
            rootId = id
            save()
        }
    }
}
