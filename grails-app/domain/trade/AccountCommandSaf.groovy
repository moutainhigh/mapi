package trade
class AccountCommandSaf {
    String  commandNo
    String  commandType = 'transfer'
    Integer subSeqno    = 0
    Long    tradeId
    String  tradeNo
    String  outTradeNo
    String  fromAccountNo
    String  toAccountNo
    Long    amount
    String  currency    = 'CNY'
    String  transferType
    String  redoFlag
    Integer redoCount
    Date    syncTime
    String  syncFlag    = 'F'
    String  respCode
    String  transCode
    Long    transId
    String  subject
    Date    dateCreated
    Date    lastUpdated

    static constraints = {
        commandType     inList: ['transfer', 'freeze', 'unfreeze']
        outTradeNo      nullable: true
        transferType    inList: ['payment', 'transfer', 'refund', 'charge', 'withdrawn',
                'royalty', 'royalty_rfd', 'frozen', 'unfrozen', 'fee', 'fee_rfd']
        redoFlag    inList: ['T', 'F'], nullable: true
        redoCount   nullable: true
        syncFlag    inList: ['F', 'S']
        respCode    nullable: true
        transCode   nullable: true
        transId     nullable: true
        subject     nullable: true
    }

    static mapping = {
        table 'trade_account_command_saf'
        id generator: 'sequence', params: [sequence: 'seq_trade_account_command']
    }

    def clone(attr = [:]) {
        new AccountCommandSaf(
                commandNo       : (attr.commandNo) ? attr.commandNo : commandNo,
                commandType     : commandType,
                subSeqno        : subSeqno,
                tradeId         : tradeId,
                tradeNo         : tradeNo,
                outTradeNo      : outTradeNo,
                fromAccountNo   : fromAccountNo,
                toAccountNo     : toAccountNo,
                amount          : amount,
                currency        : currency,
                transferType    : transferType,
                subject         : subject,
                redoFlag        : (attr.redoFlag) ? attr.redoFlag : '',
                redoCount       : (attr.redoCount) ? attr.redoCount : redoCount+1,
                syncFlag        : (attr.syncFlag) ? attr.syncFlag : 'F',
                syncTime        : (attr.syncTime) ? attr.syncTime : new Date()
        )
    }

    def toAccountCommandMap() {[
            commandType     : commandType,
            fromAccountNo   : fromAccountNo,
            toAccountNo     : (toAccountNo) ? toAccountNo : '',
            amount          : amount,
            transferType    : transferType,
            tradeNo         : tradeNo,
            outTradeNo      : (outTradeNo) ? outTradeNo : '',
            subject         : (subject) ? subject : ''
    ]}
}
