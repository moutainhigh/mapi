package mapi

import boss.InnerAccount
import customer.Customer
import customer.CustomerService
import customer.RefundDetail
import customer.RoyaltyBinding
import ebank.lang.MAPIException
import ebank.tools.StringUtil
import gateway.GwTransaction
import net.sf.json.JSONObject
import trade.AccountCommandPackage
import trade.TradeBase
import trade.TradePayment
import trade.TradeRefund
import java.util.regex.Pattern
import java.util.regex.Matcher

class AsynRefundController extends BaseController {
    def accountClientService
    def noGeneratorService
    def jmsService

    protected setConstraints() {
        //必输字段
        required_attr = ['partner', 'orig_out_trade_no']
    }
    /**
     * 标题: 处理单笔退款接口，也可以退分润
     * 退款参数: 交易退款信息$收费退款信息|分润退款信息|分润退款信息
     * 交易退款信息: 原付款交易号^退交易金额^退款理由
     * 收费退款信息: 被收费人userId^退款金额^退款理由
     * 分润退款信息: 转出人userId^转入人userId^退款金额^退款理由
     * 接口类型 HTTP调用, XML, JSON返回
     * TODO 分润退款接口
     */
    protected execute() {
        log.debug 'in refund:' + params.orig_order_no
        def partner = params._partner
        // 解析退款参数
        def refundData = [:]
        def tradePayment
        def money = 0
        def refundAmount = 0
        def gwTransactions
        //正常退款
        if (params.amount) {
            if (!(params.amount ==~ /^(\d{0,8}+)(\.\d{1,2})?$/)) {
                throw new MAPIException("ASYN_REFUND_AMOUNT_ERROR!")
            } else {
                def xamount = (params.amount as double)
                if (xamount < 0.01) {
                    throw new MAPIException("PARAMETER_AMOUT_ZERO");
                }
            }
        }
        // 格式1 orig_out_trade_no, amount, subject
        if (params.order_no && String.valueOf(params.order_no).length() > 64) {
            throw new MAPIException('OUTTRADENO_PARAMETER_OVERFLOW')
        }
        refundData = [
                fromCustomerNo: params.merchant_ID,
                outTradeNo: params.orig_order_no,
                amount: StringUtil.parseAmountFromStr(params.amount),
                subject: (params.note) ? URLDecoder.decode(params.note, params._input_charset) : '',
                time: new Date(),
                refundType: 'normal',
                refundParams: 'n/a'
        ]

        //退款订单号不能重复。
        if (params.orig_order_no) {
            def tradeBase = TradeBase.findWhere(outTradeNo: params.orig_order_no, tradeType: 'refund', partnerId: partner.id, status: 'completed')
            def tradeRefund = TradeBase.findWhere(outTradeNo: params.orig_order_no, tradeType: 'refund', partnerId: partner.id, status: 'processing')
            if (tradeBase) {
                throw new MAPIException('OUT_TRADE_REPEAT')
            }
            if (tradeRefund) {
                throw new MAPIException('DO_NOT_SUBMIT_REPEAT')
            }
        }
        //根据原退款订单号，查询该笔交易是否是异步退款
        def cnRefundDetail = RefundDetail.findWhere(refundNo: params.orig_order_no, refundType: '1', status: 'completed')
        log.info "asynRefund:" + partner.id + "out trade no:" + refundData.origOutTradeNo
        if (cnRefundDetail) {
            //根据原退款订单号及ID查询退分润交易。
            def asynRefund = TradeBase.findAllWhere(outTradeNo: params.orig_order_no, tradeType: 'royalty_rfd', status: 'completed')
            //根据分润退款得到能退款的总金额（包括退款金额及手续费）
            def amount = Double.parseDouble(params.amount) * 100
            if (asynRefund.size() > 0) {
                //查找支付订单
                tradePayment = TradeBase.findWhere(rootId: asynRefund[0].rootId, tradeType: 'payment', partnerId: partner.id)
                asynRefund.each {
                    refundAmount = refundAmount + it.amount
                }
            }
            //如果是全额退款，退款金额必须和退分润金额相等，否则只能小于等于退分润金额
            if (params.is_asyn_refund == 'Y' || 'y'.equalsIgnoreCase(params.is_asyn_refund) || params.is_asyn_refund == 'y') {
                if (refundAmount != amount) {
                    log.error "ASYN_REFUND_AMOUNT_EQUAL:" + params.amount
                    throw new MAPIException('ASYN_REFUND_AMOUNT_EQUAL')
                }
            } else if (params.is_asyn_refund == 'N' || 'n'.equalsIgnoreCase(params.is_asyn_refund) || params.is_asyn_refund == 'n') {
                if (amount > refundAmount) {
                    log.error "ASYN_REFUND_AMOUNT_GREATER:" + params.amount
                    throw new MAPIException('ASYN_REFUND_AMOUNT_GREATER')
                }
                money = refundAmount - amount
            }
        } else { //该笔交易为同步退款或不存在
            log.error "OUT_TRADE_NOT_ASYN_REFUND:" + params.orig_order_no
            throw new MAPIException('OUT_TRADE_NOT_ASYN_REFUND')
        }
        def payment = TradePayment.findById(tradePayment.id)
        //异步退分润手续费
        def feeAmount = refundAmount * tradePayment.feeAmount / tradePayment.amount
        if (payment) {
            // 查看原交易的付款方是否为Guest账户，来判断退款是否直接退到付款人账户里
            refundData.isGuestPayment = (payment.payerAccountNo == InnerAccount.getGuestAccountNo())
        }

        //创建退款交易表

        // refund payee srvAcct reset
        def payeeAccountNo = payment.payeeAccountNo
        def refund = payment.createRefund(refundData)
        payment.payeeAccountNo = payeeAccountNo   //reset the payeeAccountNo
        log.info("payeeAcct:" + refund.payerAccountNo + " payment:" + payment.id)
        refund.submitType = 'automatic'

        if (refundData.isGuestPayment) {
            // 先退到中间过渡账户的逻辑
            refund.payeeAccountNo = InnerAccount.getMiddleAccountNo()
            refund.handleStatus = 'waiting'
            // 查找原来的交易信息
            gwTransactions = GwTransaction.find(
                    "from GwTransaction where order.id=? and order.partnerCustomerNo=? and status='1' order by completionTime asc", [payment.tradeNo, params.merchant_ID]
            )
            if (!gwTransactions) {
                throw new MAPIException('TRADE_NOT_EXIST')
            }
            refund.acquirerCode = gwTransactions.acquirerCode ? gwTransactions.acquirerCode : 'n/a'
            refund.acquirerMerchantNo = gwTransactions.acquirerMerchant ? gwTransactions.acquirerMerchant : 'n/a'
            refund.acquirerAccountId = gwTransactions.acquirerInnerAccountName as Long ? gwTransactions.acquirerInnerAccountName as Long : 0
        } else {
            refund.acquirerCode = 'n/a'
            refund.acquirerMerchantNo = 'n/a'
            refund.acquirerAccountId = 0
        }

        def acPackage = new AccountCommandPackage()
        def tradeList = []
        //非分润退款保存
        saveNomaleRefund(refund, acPackage, money, partner, refundData, feeAmount)
        TradeBase.withTransaction {
            trx ->
            //修改RefundDetail表状态
            // 发送指令, 接受指令结果
//            acPackage.commandList.remove(0)
            def resp = accountClientService.executeCommands(acPackage)
            // 根据结果更新
            if (!resp) {
                writeResponse 'SYSTEM_BUSY'
            } else if (resp.result == 'true') {
                payment.refundAmount += refundData.amount
                if (payment.amount == payment.refundAmount) {
                    payment.status = 'closed'
                }
                payment.save failOnError: true
                refund.status = (refundData.isGuestPayment) ? 'processing' : 'completed'
                refund.handleStatus = (refundData.isGuestPayment) ? refund.handleStatus : 'completed'
                refund.save failOnError: true
                tradeList.each { trade ->
                    trade.status = 'completed'
                    trade.save failOnError: true
                }
                writeResponse 'SUCCESS'
            } else {
                refund.status = 'closed'
                refund.save failOnError: true
                tradeList.each { trade ->
                    trade.status = 'closed'
                    trade.save failOnError: true
                }
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
            log.info 'up 2 ?'
            acPackage.update resp
            log.info 'up end'
        }
        log.info 'refund end'
    }

/**
 * 保存普通退款交易
 * @return
 */
    protected saveNomaleRefund(TradeRefund refund, acPackage, money, partner, refundData, feeAmount) {
        TradeBase.withTransaction { trx ->
            refund.backFee = feeAmount
            refund.save flash: true, failOnError: true
            if (money > 0 && !refundData.isGuestPayment) {
                acPackage.append(
                        tradeId: refund.id,
                        tradeNo: refund.tradeNo,
                        outTradeNo: refund.outTradeNo,
                        fromAccountNo: refund.payerAccountNo,
                        toAccountNo: partner.accountNo,
                        amount: money,
                        currency: refund.currency,
                        transferType: 'transfer'
                )
            }
            acPackage.append refund
            acPackage.save()
        }
    }
}
