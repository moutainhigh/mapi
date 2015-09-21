package trade

import org.apache.commons.logging.LogFactory

/**
 * 帐务指令包装器
 * 注意: 非线程安全
 */
class AccountCommandPackage {
    static log = LogFactory.getLog(AccountCommandPackage.class)

    String  commandNo   = UUID.randomUUID().toString().replace('-', '')
    List    commandList = []
    Date    dateCreated = new Date()
    Boolean updateOnly  = false
    Boolean redoMode    = false

    def append(TradeBase trade) {
        def commandType = 'transfer'
        if ( trade.tradeType == 'frozen' ) {
            commandType = 'freeze'
        } else if ( trade.tradeType == 'unfrozen' ) {
            commandType = 'unfreeze'
        }
        append(
                commandType     : commandType,
                tradeId         : trade.id,
                tradeNo         : trade.tradeNo,
                outTradeNo      : trade.outTradeNo,
                fromAccountNo   : trade.payerAccountNo,
                toAccountNo     : trade.payeeAccountNo,
                amount          : trade.amount,
                currency        : trade.currency,
                transferType    : trade.tradeType
        )
    }

    def append(Map attr) {
        if (updateOnly) return this
        attr.commandNo  = commandNo
        attr.subSeqno   = commandList.size()
        attr.syncTime   = dateCreated
        attr.syncFlag   = 'F'
        if (redoMode) {
            attr.redoFlag  = 'T'
            attr.redoCount = 0
        }
        if (commandList) {
            def last = commandList[-1]
            attr.commandType    = (attr.commandType) ? attr.commandType : last.commandType
            attr.tradeId        = (attr.tradeId) ? attr.tradeId : last.tradeId
            attr.tradeNo        = (attr.tradeNo) ? attr.tradeNo : last.tradeNo
            attr.outTradeNo     = (attr.outTradeNo) ? attr.outTradeNo : last.outTradeNo
            attr.currency       = (attr.currency) ? attr.currency : last.currency
            attr.transferType   = (attr.transferType) ? attr.transferType : last.transferType
        } else {
            attr.outTradeNo     = (attr.outTradeNo) ? attr.outTradeNo : ''
        }
        commandList << new AccountCommandSaf(attr)
        this
    }

    def save() {
        commandList.each { it.save failOnError: true }
    }

    def update( resp ) {
        log.info "update $resp"
        log.info "in update"
        if ( !resp ) return
        commandList.each { acs ->
            acs.syncFlag = 'S'
            acs.respCode = resp.errorCode
            if ( resp.result == 'true' ) {
                acs.transCode = resp.transCode
                acs.transId   = resp.transIds.remove(0)
                acs.redoFlag = ''
            }
            acs.save failOnError: true
        }
    }

    String toString() {
        "AccountCommandPackage($commandNo) $commandList"
    }

    static findByCommandNo( commandNo ) {
        def list = AccountCommandSaf.findAll(
                "from AccountCommandSaf where commandNo=? order by subSeqno",
                [commandNo]
        )
        if (list) {
            new AccountCommandPackage(commandNo: commandNo, commandList: list, updateOnly: true, redoMode: (list[0].redoFlag=='T'))
        } else {
            null
        }
    }

    static createRedoByCommandNo( commandNo ) {
        def list = AccountCommandSaf.findAll(
                "from AccountCommandSaf where commandNo=? and redoFlag='T' and syncFlag='S' order by subSeqno",
                [commandNo]
        )
        if (list) {
            def acPackage = new AccountCommandPackage(updateOnly: true, redoMode: true)
            acPackage.commandList = list.collect {
                it.redoFlag = 'F'
                it.save failOnError: true
                it.clone(
                        commandNo: acPackage.commandNo, redoFlag: 'T', syncTime: acPackage.dateCreated
                ).save failOnError: true
            }
            acPackage
        } else {
            null
        }
    }
}
