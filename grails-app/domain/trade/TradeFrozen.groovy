package trade

import ebank.tools.StringUtil

class TradeFrozen extends TradeBase {
    def noGeneratorService

    Long    unfrozenAmount = 0
    String  frozenType
    String  frozenParams = 'n/a'
    String  frozenStatus = 'completed'

    static constraints = {
        unfrozenAmount  min: 0L
        frozenType      inList: ['normal', 'royalty']
        frozenParams    nullable: true
    }

    static mapping = {
    }

    def createUnfrozen(Long unfrozenAmount, Date time) {
        new TradeUnfrozen(
                rootId          : rootId,
                originalId      : id,
                tradeType       : 'unfrozen',
                tradeNo         : noGeneratorService.createTradeNo('unfrozen', time),
                partnerId       : partnerId,
                payerId         : payeeId,
                payerCode       : payeeCode,
                payerName       : payeeName,
                payerAccountNo  : payeeAccountNo,
                payeeId         : payerId,
                payeeCode       : payerCode,
                payeeName       : payerName,
                payeeAccountNo  : payerAccountNo,
                amount          : unfrozenAmount,
                currency        : currency,
                tradeDate       : StringUtil.getNumericDate(time) as int,
                status          : 'starting',
                unfrozenType    : 'normal'
        )
    }
}
