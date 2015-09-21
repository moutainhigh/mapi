package mapi

import trade.TradeUnfrozen
import trade.AccountCommandPackage
import ebank.tools.StringUtil
import trade.TradeBase
import trade.TradeFrozen
import ebank.lang.MAPIException

class UnfrozenController extends BaseController {
    def accountClientService

    protected setConstraints() {
        required_attr = ['partner', 'out_trade_no', 'orig_out_trade_no', 'amount']
        verifyOutTradeNoRepeat = true
    }

    protected execute() {
        log.info 'in frozen'
        def partner = params._partner

        def amount = StringUtil.parseAmountFromStr(params.amount)
        def frozen = TradeFrozen.findByPartnerIdAndOutTradeNo(partner.id, params.orig_order_no)
        if (!frozen) {
            throw new MAPIException( 'TRADE_NOT_EXIST' )
        } else if (frozen.status!='completed') {
            throw new MAPIException( 'TRADE_STATUS_NOT_ALLOW' )
        }
        if (amount > (frozen.amount-frozen.unfrozenAmount)) {
            throw new MAPIException( 'GREATER_UNFROZEN_MONEY' )
        }

        def now = new Date()
        def acPackage = new AccountCommandPackage()
        def unfrozen = frozen.createUnfrozen(amount, now)
        unfrozen.outTradeNo = params.order_no
        unfrozen.subject = (params.subject) ? params.subject : ''
        TradeBase.withTransaction { trx ->
            unfrozen.save flash: true, failOnError: true
            acPackage.append unfrozen
            acPackage.save()
        }

        TradeBase.withTransaction { trx ->
            // 发送指令, 接受指令结果
            def resp = accountClientService.executeCommands(acPackage)
            // 根据结果更新
            if ( !resp ) {
                writeResponse 'SYSTEM_BUSY'
            } else if (resp.result == 'true') {
                frozen.unfrozenAmount += amount
                if (frozen.amount == frozen.unfrozenAmount) {
                    frozen.status = 'closed'
                }
                frozen.save failOnError: true
                unfrozen.status = 'completed'
                unfrozen.save failOnError: true
                writeResponse 'SUCCESS'
            } else {
                unfrozen.status = 'closed'
                unfrozen.save failOnError: true
                switch (resp.errorCode) {
                    case '02':
                        writeResponse 'ACCOUNT_STATUS_NOT_ALLOW'
                        break
                    default:
                        writeResponse 'GENERAL_FAIL'
                        break
                }
            }
            acPackage.update resp
        }
        log.info 'frozen end'
    }
}
