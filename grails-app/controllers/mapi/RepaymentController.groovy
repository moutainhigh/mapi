package mapi

import ebank.lang.MAPIException
import trade.TradeBase
import trade.TradePayment
import customer.Customer
import trade.TradeRefund
import boss.InnerAccount

class RepaymentController extends BaseController {
    def accountClientService

    protected setConstraints() {
        required_attr = ['partner', 'orig_out_trade_no', 'out_trade_no']
    }

    protected execute() {
        log.info 'in repayment'
        def partner = params._partner
        def repaymentData
        def payerId
        //根据原订单号查询退款
        def payment = TradePayment.findWhere(partnerId: partner.id, outTradeNo: params.orig_order_no, royaltyType: '10')
        if (!payment) {
            throw new MAPIException('ORIG_OUT_TRADE_NO_NOT_EXIST')
        }
        def refund = TradeRefund.findAllWhere(partnerId: partner.id, rootId: payment.rootId, outTradeNo: params.order_no, tradeType: 'refund')
        if (!refund) {
            throw new MAPIException('ROYALTY_REFUND_NOT_EXIST')
        }
        if (refund.size() > 1) {
            refund = TradeRefund.findAllWhere(partnerId: partner.id, rootId: payment.rootId, outTradeNo: params.order_no, tradeType: 'refund', status: 'completed')
        }
        if (!refund) {
            throw new MAPIException('ROYALTY_REFUND_NOT_EXIST')
        }
        //是否是访客客户
        if (payment.payerAccountNo == InnerAccount.getGuestAccountNo()) {
            if (refund[0].handleStatus != 'completed') {
                throw new MAPIException(params.order_no + '_NEED_CHECK_PASS!')
            }
        }
        if (params.repayment_parameters != null && params.repayment_parameters != '') {
            repaymentData = ParameterParser.parseRepaymentParams(refund[0], params.repayment_parameters)
        }
        def query = {
            eq('partnerId', partner.id)
            eq('tradeType', 'royalty_rfd')
            eq('rootId', payment.rootId)
            or {
                eq('originalId', refund[0].id)
                eq('outTradeNo', refund[0].outTradeNo)
            }
            eq('refundFlag', '1')
        }
        def repayment = TradeRefund.createCriteria().list(query)
        if (!repayment) {
            throw new MAPIException('ORIG_OUT_TRADE_NO_IS_NOT_REPAYMENT')
        } else {
            TradeBase.withTransaction { trx ->
                def commandNo = UUID.randomUUID().toString().replace('-', '')
                def commandList = []
                def fromAccountNo
                def money = 0
                repayment.each { item ->
                    if (repaymentData) {
                        repaymentData.repayment.collect {
                            if (it.fromCustomerNo == item.payerId) {
                                money = it.amount
                                fromAccountNo = Customer.get(item.payerId).accountNo
                                commandList.add([commandType: 'transfer', fromAccountNo: fromAccountNo, toAccountNo: item.payerAccountNo, amount: money, transferType: 'royalty_rfd', tradeNo: item.tradeNo, outTradeNo: item.outTradeNo, subjict: item.subject])
                            }
                        }

                    } else {
                        money = item.amount
                        fromAccountNo = Customer.get(item.payerId).accountNo
                        commandList.add([commandType: 'transfer', fromAccountNo: fromAccountNo, toAccountNo: item.payerAccountNo, amount: money, transferType: 'royalty_rfd', tradeNo: item.tradeNo, outTradeNo: item.outTradeNo, subjict: item.subject])
                    }
                }
                def resp = accountClientService.executeCommands(commandNo, commandList)
                if (!resp) {
                    writeResponse 'SYSTEM_BUSY'
                } else if (resp.result == 'true') {
                    repayment.each { item ->
                        if (repaymentData) {
                            repaymentData.repayment.collect {
                                if (it.fromCustomerNo == item.payerId) {
                                    item.refundFlag = '2'
                                    item.save failOnError: true
                                }
                            }
                        } else {
                            item.refundFlag = '2'
                            item.save failOnError: true
                        }
                    }
                    writeResponse 'SUCCESS'
                } else {
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
            }
        }
        log.info 'repayment trade end'
    }
}
