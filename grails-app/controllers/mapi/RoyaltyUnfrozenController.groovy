package mapi

import ebank.lang.MAPIException
import ebank.tools.StringUtil
import trade.AccountCommandPackage
import trade.TradeBase
import trade.TradeFrozen
import trade.TradeUnfrozen
import trade.TradePayment

class RoyaltyUnfrozenController extends BaseController {
    def accountClientService

    protected setConstraints() {
        required_attr = ['partner', 'out_trade_no', 'orig_out_trade_no','orig_trade_no', 'amount']
        verifyOutTradeNoRepeat = true
    }

    protected execute() {
        log.info 'in frozen'
        def partner = params._partner

        if (!(params.amount ==~ /^(\d{0,8}+)(\.\d{1,2})?$/)) {
            throw new MAPIException("AMOUNT_FORMAT_ERROR!")
        }
        if (Double.parseDouble(params.amount) < 0.01) {
            throw new MAPIException("AMOUNT_TOO_SMALL!")
        }
        def amount = StringUtil.parseAmountFromStr(params.amount)
        def frozen = TradeFrozen.findByPartnerIdAndOutTradeNo(partner.id, params.orig_order_no)
        def unFrozen=TradeUnfrozen.findWhere(partnerId: partner.id,outTradeNo: params.orig_order_no,status: 'completed')
        if(unFrozen){
           throw new MAPIException( 'ORIG_OUT_TRADE_NO_REPEAT' )
        }
        //根据原订单号，查询原订单
        def payment=TradePayment.findByOutTradeNoAndStatus(params.orig_trade_no,'completed')
        if(!payment){
             throw new MAPIException( 'ORIG_TRADE_NO_NOT_EXIST')
        } else if(payment.royaltyStatus!='10'){
             throw new MAPIException( 'ORIG_TRADE_NO_NOT_ROYALTY')
        }
        if (!frozen) {
            throw new MAPIException( 'TRADE_NOT_EXIST' )
        } else if (frozen.status!='completed') {
            throw new MAPIException( 'TRADE_STATUS_NOT_ALLOW' )
        }
        if (amount >frozen.amount) {
            throw new MAPIException( 'GREATER_UNFROZEN_MONEY' )
        }else if(amount < frozen.amount)  {
             throw new MAPIException( 'SMALL_UNFROZEN_MONEY' )
        }
        if(frozen?.rootId!=payment?.rootId){
           throw new MAPIException( 'TRADE_NO_DISSAFFINITY')
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
