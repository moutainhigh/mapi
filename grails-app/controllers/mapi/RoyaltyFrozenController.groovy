package mapi

import customer.Customer
import customer.CustomerOperator
import customer.LoginCertificate
import customer.RoyaltyBinding
import ebank.lang.MAPIException
import ebank.tools.StringUtil
import trade.AccountCommandPackage
import trade.TradeBase
import trade.TradeFrozen

class RoyaltyFrozenController extends BaseController {
    def accountClientService
    def noGeneratorService

    protected setConstraints() {
        required_attr = ['partner', 'orig_out_trade_no', 'out_trade_no', 'customer_no', 'amount']
        //serviceCode = 'royalty'
        verifyOutTradeNoRepeat = true
    }

    protected execute() {
        log.info 'in frozen'
        def partner = params._partner
        def customer
        def royaltyAmount = 0//分润总金额
        if (!(params.amount ==~ /^(\d{0,8}+)(\.\d{1,2})?$/)) {
            throw new MAPIException("AMOUNT_FORMAT_ERROR!")
        }
        if (Double.parseDouble(params.amount) < 0.01) {
            throw new MAPIException("AMOUNT_TOO_SMALL!")
        }
        if (params.customer_no.toString().indexOf('@') != -1) {
            customer = Customer.get(CustomerOperator.findByIdAndStatus(LoginCertificate.findByLoginCertificate(params.customer_no).customerOperator.id, 'normal').customer.id)
        } else {
            customer = Customer.findByCustomerNo(params.customer_no)
        }
        if (!customer || customer.status != 'normal') {
            throw new MAPIException('USER_NOT_EXIST')
        }
//        def bind = RoyaltyBinding.findByPartnerAndCustomer(partner, customer)
        def bind = RoyaltyBinding.findWhere(partner: partner, customer: customer, bizType: '10', status: 'bind', nopassRefundFlag: 'T')
        if (!bind) {
            throw new MAPIException('BINDING_NOT_EXIST')
        }
//        if (!bind || bind.status!='bind' || bind.nopassRefundFlag!='T') {
//            throw new MAPIException( 'BINDING_NOT_EXIST' )
//        }
        //根据原分润支付订单号查询分润交易，判断冻结金额不能大于分润金额
        def royalty = TradeBase.findAllWhere(partnerId: partner.id, payeeId: customer.id, outTradeNo: params.orig_order_no, tradeType: 'royalty', status: 'completed')
        if (royalty.size() > 0) {
            royalty.each {
                royaltyAmount += it.amount
            }
            //交易冻结对某个分润交易的某个收款方只允许冻结一次
            def royaltyFrozen = TradeBase.findWhere(partnerId: partner.id, payerId: customer.id, rootId: royalty[0].rootId, tradeType: 'frozen', status: 'completed')
            if (royaltyFrozen) {
                throw new MAPIException('FROZEN_REPEAT')
            }
        } else {
            throw new MAPIException('ROYALTY_NOT_EXIST')
        }
        if (royaltyAmount < Double.parseDouble(params.amount) * 100) {
            throw new MAPIException('ROYALTY_AMOUNT_NOT_ENOUGH')
        }
        def now = new Date()
        def acPackage = new AccountCommandPackage()
        def amount = StringUtil.parseAmountFromStr(params.amount)
        def frozen = new TradeFrozen(
                tradeType: 'frozen',
                tradeNo: noGeneratorService.createTradeNo('frozen', now),
                outTradeNo: params.order_no,
                partnerId: partner.id,
                payerId: customer.id,
                payerName: customer.name,
                payerAccountNo: customer.accountNo,
                payeeId: customer.id,
                payeeName: customer.name,
                payeeAccountNo: customer.accountNo,
                amount: amount,
                status: 'starting',
                tradeDate: StringUtil.getNumericDate(now) as int,
                subject: (params.subject) ? params.subject : '',
                frozenType: 'normal',
                rootId: royalty[0].rootId ? royalty[0].rootId : ''
        )
        log.info "save frozen: $frozen"
        TradeBase.withTransaction { trx ->
            frozen.save flash: true, failOnError: true
            acPackage.append frozen
            acPackage.save()
        }

        TradeBase.withTransaction { trx ->
            // 发送指令, 接受指令结果
            def resp = accountClientService.executeCommands(acPackage)
            // 根据结果更新
            if (!resp) {
                writeResponse 'SYSTEM_BUSY'
            } else if (resp.result == 'true') {
                frozen.status = 'completed'
                frozen.save failOnError: true
                writeResponse 'SUCCESS'
            } else {
                frozen.status = 'closed'
                frozen.save failOnError: true
                switch (resp.errorCode) {
                    case '02':
                        writeResponse 'ACCOUNT_STATUS_NOT_ALLOW'
                        break
                    case '03':
                        writeResponse 'AVAILABLE_AMOUNT_NOT_ENOUGH'
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
