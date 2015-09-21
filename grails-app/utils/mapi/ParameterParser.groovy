package mapi

import ebank.tools.StringUtil
import ebank.lang.MAPIException
import customer.CustomerOperator
import customer.RefundDetail
import trade.TradeBase
import java.util.regex.Pattern
import java.util.regex.Matcher
import gateway.GwOrder
import gateway.Gwsuborders
import boss.InnerAccount
import trade.TradeRefund

/**
 * Created by IntelliJ IDEA.
 * User: tommy
 * Date: 11-3-16
 * Time: 下午6:01
 * To change this template use File | Settings | File Templates.
 */
class ParameterParser {
    static NOTE_MAX_SIZE = 30
    static ROYALTY_ITEM_MAX_SIZE = 10
    static ROYALTY_PARAM_MAX_SIZE = 512

    /**
     * 从参数中解析出帐务指令集合
     * @param seller
     * @param royaltyParameters 接口中的参数串
     * 例如：
     * 分润收款账号1^80^说明1|分润收款账号1^分润收款账号2^5^说明2|分润收款账号3^20^说明3
     * @param amount
     * @return
     */
    static parseRoyaltyParams(seller, royaltyParameters, amount) {
        if (!royaltyParameters || royaltyParameters.size() > ROYALTY_PARAM_MAX_SIZE) {
            throw new MAPIException('ILLEGAL_PARAMETER', "error royalty refundParameters size: $royaltyParameters")
        }
        def customer = [:]
        customer[seller] = amount
        def commandList = []
        def items = royaltyParameters.split(/\|/)
        if (items.size() > ROYALTY_ITEM_MAX_SIZE) {
            throw new MAPIException('ILLEGAL_PARAMETER', "royalty item size too large: ${items.size()}")
        }
        items.each { item ->
            def args = StringUtil.splitAllTokens(item, '^')
            def from = null, to = null, amt = null, note = '', fromEmail = null, toEmail = null
            switch (args.size()) {
                case 4:
                    (from, to, amt, note) = args
                    if (args[0].indexOf('@') != -1) {
                        fromEmail = args[0]
                        def fromOperator = CustomerOperator.find(
                                "from CustomerOperator o inner join fetch o.loginCertificate lc where lc.loginCertificate=?",
                                [args[0]]
                        )
                        if (fromOperator) {
                            from = fromOperator.customer.customerNo
                        } else {
                            throw new MAPIException('ILLEGAL_PARAMETER', "error: $item, from($from) email error.")
                        }
                    }
                    if (args[1].indexOf('@') != -1) {
                        toEmail = args[1]
                        def toOperator = CustomerOperator.find(
                                "from CustomerOperator o inner join fetch o.loginCertificate lc where lc.loginCertificate=?",
                                [args[1]]
                        )
                        if (toOperator) {
                            to = toOperator.customer.customerNo
                        } else {
                            throw new MAPIException('ILLEGAL_PARAMETER', "error: $item, from($to) email error.")
                        }
                    }
                    break
                case 3:
                    (to, amt, note) = args
                    if (args[0].indexOf('@') != -1) {
                        toEmail = args[0]
                        def toOperator = CustomerOperator.find(
                                "from CustomerOperator o inner join fetch o.loginCertificate lc where lc.loginCertificate=?",
                                [args[0]]
                        )
                        if (toOperator) {
                            to = toOperator.customer.customerNo
                        } else {
                            throw new MAPIException('ILLEGAL_PARAMETER', "error: $item, from($to) email error.")
                        }
                    }
                    from = seller
                    break
                case 2:
                    (to, amt) = args
                    if (args[0].indexOf('@') != -1) {
                        toEmail = args[0]
                        def toOperator = CustomerOperator.find(
                                "from CustomerOperator o inner join fetch o.loginCertificate lc where lc.loginCertificate=?",
                                [args[0]]
                        )
                        if (toOperator) {
                            to = toOperator.customer.customerNo
                        } else {
                            throw new MAPIException('ILLEGAL_PARAMETER', "error: $item, from($to) email error.")
                        }
                    }
                    from = seller
                    break
                default:
                    throw new MAPIException('ILLEGAL_PARAMETER', "error: $item")
            }
            if (!(amt ==~ /^(\d{0,8}+)(\.\d{1,2})?$/)) {
                throw new MAPIException("PARAMETER_AMOUNT_ERROR!")
            }
            amt = StringUtil.parseAmountFromStr(amt)
            if (amt < 0) throw new MAPIException('ILLEGAL_PARAMETER', "error: $item, amount: $amt")
            if (!customer.containsKey(from) || customer[from] < amt) {
                throw new MAPIException('ILLEGAL_PARAMETER', "error: $item, from($from) amount error.")
            }
            if (note.size() > NOTE_MAX_SIZE) {
                throw new MAPIException('ILLEGAL_PARAMETER', "error: $item, note too large: $note")
            }
            if (!customer[to]) customer[to] = 0
            customer[from] -= amt
            customer[to] += amt
            commandList << [fromCustomerNo: from, toCustomerNo: to, amount: amt, subject: note, fromEmail: fromEmail, toEmail: toEmail]
        }
        [commandList, customer]
    }

    /**
     * 从参数中解析出帐务指令集合
     * @param seller
     * @param royaltyParameters 接口中的参数串
     * @param amount
     * @return
     */
    static parseMergeParams = {orig_out_trade_no, royaltyParameters ->
        def refundData = [royaltyParameters: royaltyParameters]
        def sellerCode = ''

        //字段长度判断
        if (!royaltyParameters || royaltyParameters.size() > ROYALTY_PARAM_MAX_SIZE) {
            throw new MAPIException('ILLEGAL_PARAMETER', "error royalty refundParameters size: $royaltyParameters")
        }
        def items = royaltyParameters.split(/\|/)
        //包含个数判断
        if (items.size() > ROYALTY_ITEM_MAX_SIZE) {
            throw new MAPIException('ILLEGAL_PARAMETER', "royalty item size too large: ${items.size()}")
        }
        items.each { item ->
            def args = StringUtil.splitAllTokens(item, '^')
            def from = null, to = null, amt = null, note = ''
            //已退金额
            def refundAmount = 0
            switch (args.size()) {
                case 4:
                    (from, to, amt, note) = args
                    break
                case 3:
                    (from, to, amt) = args
                    break
                default:
                    throw new MAPIException('ILLEGAL_PARAMETER', "error: $item")
            }
            if (amt == null || amt == '') {
                throw new MAPIException("PARAMETER_AMOUNT_NEED!")
            }
            if (!(amt ==~ /^(\d{0,8}+)(\.\d{1,2})?$/)) {
                throw new MAPIException("PARAMETER_AMOUNT_ERROR!")
            }
            def tradeNo = /^[0-9a-zA-Z_-]*$/
            Pattern patternNo = Pattern.compile(tradeNo);
            Matcher matcherNo = patternNo.matcher(from);
            if (!matcherNo.find()) {
                throw new MAPIException("OUT_TRADE_NO_PATTERN_ERROR!")
            }
            amt = StringUtil.parseAmountFromStr(amt)
            def gwOrder = GwOrder.findByOutTradeNo(orig_out_trade_no)
            if (!gwOrder) {
                throw new MAPIException("ORIG_OUT_TRADE_NO_NOT_EXIST!")
            }
            def gwSubmitOrder = Gwsuborders.findWhere(outtradeno: from, gwordersid: gwOrder.id, sellerCode: to)
            if (!gwSubmitOrder) {
                throw new MAPIException("MEGER_NOT_EXIST!")
            }
            if (sellerCode.equalsIgnoreCase(to)) {
                throw new MAPIException("PAYEE_ACCOUNT_NO_REPEAT!")
            } else {
                sellerCode = to
            }
            def tradeBase = TradeBase.findByTradeNo(gwSubmitOrder.id)
            if (!tradeBase) {
                throw new MAPIException("PAYMENT_NOT_EXIST!")
            } else if (tradeBase.status != 'completed') {
                throw new MAPIException('TRADE_STATUS_NOT_ALLOW')
            } else {
                from = tradeBase.payeeAccountNo
                to = tradeBase.payerAccountNo
            }
            //根据OriginalId查找已退单据
            def tradeRefund = TradeRefund.findAllByOriginalIdAndStatusInList(tradeBase.id,['completed','processing'])
            if (tradeRefund.size() > 0) {
                tradeRefund.each {
                    refundAmount = it.amount + refundAmount
                }
            }
            if ((amt + refundAmount) > tradeBase.amount) {
                throw new MAPIException("REFUND_AMOUNT_TOOMUCH!")
            }
            if (amt <= 0) throw new MAPIException('PARAMETER_AMOUNT_ERROR', "error: $item, amount: $amt")
            if (note.size() > NOTE_MAX_SIZE) {
                throw new MAPIException('ILLEGAL_PARAMETER', "error: $item, note too large: $note")
            }
            if (!refundData.merges) refundData.merges = []
            refundData.merges << [fromCustomerNo: from, toCustomerNo: to, amount: amt, subject: note, origOutTradeNo: tradeBase.tradeNo]
        }
        refundData
    }

    /**
     * 从参数中解析出帐务指令集合
     * @param seller
     * @param royaltyParameters 接口中的参数串
     * @param amount
     * @return
     */
    static parseRepaymentParams = {refund, repaymentParameters ->
        def repaymentData = [repaymentParameters: repaymentParameters]
        //字段长度判断
        if (!repaymentParameters || repaymentParameters.size() > ROYALTY_PARAM_MAX_SIZE) {
            throw new MAPIException('ILLEGAL_PARAMETER', "error royalty refundParameters size: $repaymentParameters")
        }
        def items = repaymentParameters.split(/\|/)
        //包含个数判断
        if (items.size() > ROYALTY_ITEM_MAX_SIZE) {
            throw new MAPIException('ILLEGAL_PARAMETER', "royalty item size too large: ${items.size()}")
        }
        items.each { item ->
            def args = StringUtil.splitAllTokens(item, '^')
            def from = null, to = null, amt = null
            //已退金额
            switch (args.size()) {
                case 2:
                    (from, amt) = args
                    break
                default:
                    throw new MAPIException('ILLEGAL_PARAMETER', "error: $item")
            }
            if (!(amt ==~ /^(\d{0,8}+)(\.\d{1,2})?$/)) {
                throw new MAPIException("PARAMETER_AMOUNT_ERROR!")
            }
            amt = StringUtil.parseAmountFromStr(amt)
            def customerOperator = CustomerOperator.findByDefaultEmailAndStatus(from, 'normal')
            if (!customerOperator) {
                throw new MAPIException("REPAYMENT_ACCOUNT_EMAIL_NOT_EXIST!")
            }
            def payerId = customerOperator.customer.id
            //查询退款金额
            def tradeAmount
            tradeAmount = TradeRefund.findWhere(originalId: refund.id, payerId: payerId, status: 'completed')
            if (tradeAmount == null) {
                tradeAmount = TradeRefund.findWhere(outTradeNo: refund.outTradeNo, payerId: payerId, status: 'completed')
            }
            if (!tradeAmount) {
                throw new MAPIException("ROYALTY_REFUND_NOT_EXIST!")
            }else if(tradeAmount.refundFlag!='1'){
                throw new MAPIException(from+"_NOT_REPAYMENT!")
            }
            if (tradeAmount.amount < amt) {
                throw new MAPIException("AMOUNT_TOMUCH")
            }
            if (amt <= 0) throw new MAPIException('ILLEGAL_PARAMETER', "error: $item, amount: $amt")
            if (!repaymentData.repayment) repaymentData.repayment = []
            repaymentData.repayment << [fromCustomerNo: payerId, amount: amt]
        }
        repaymentData
    }

    /**
     * 退款参数: 交易退款信息$收费退款信息|分润退款信息|分润退款信息
     * 交易退款信息: 原付款交易号^退交易金额^退款理由
     * 收费退款信息: 被收费人userId^退款金额^退款理由
     * 分润退款信息: 转出人userId^转入人userId^退款金额^退款理由
     * @param seller
     * @param refundParameters
     * @return
     */
    static parseRefundParams = {seller, refundParameters, refundType, outTradeNo ->
        def args, basePart = null, feePart = null, otn = null, from = null, to = null, amt = null, note = null, feeFrom = null, feeAmt = null, baseAmt = null, feeNote = null, baseNote = null
        def refundData = [refundParameters: refundParameters]
        def date = new Date()
        def items = refundParameters.split(/\|/)
        if (items.size() > ROYALTY_ITEM_MAX_SIZE + 1) {
            throw new MAPIException('ILLEGAL_PARAMETER', "refund item size too large: ${items.size()}")
        }
        (basePart, feePart) = StringUtil.splitAllTokens(items[0], '$')
        // 解析交易退款信息： 原付款交易号^退交易金额^退款理由
        args = StringUtil.splitAllTokens(basePart, '^')
        if (args.size() == 3) {
            (otn, amt, note) = args
            (otn, baseAmt, baseNote) = args
        } else {
            throw new MAPIException('ILLEGAL_PARAMETER', "error refund base: $basePart")
        }
        if (!otn)
            throw new MAPIException('ILLEGAL_PARAMETER', "out_trade_no is null")
        if (!(amt ==~ /^(\d{0,8}+)(\.\d{1,2})?$/)) {
            throw new MAPIException("PARAMETER_AMOUNT_ERROR!")
        }
        amt = StringUtil.parseAmountFromStr(amt)
        baseAmt = amt
        if (amt < 1)
            throw new MAPIException('ILLEGAL_PARAMETER', "error amount: $amt")
        if (note.size() > NOTE_MAX_SIZE)
            throw new MAPIException('ILLEGAL_PARAMETER', "note too large: ${note.size()}")
        refundData.putAll(origOutTradeNo: otn, amount: amt, refundTime: new Date(), subject: note)
        // 解析收费退款信息： 被收费人userId^退款金额^退款理由
        if (feePart) {
            args = StringUtil.splitAllTokens(feePart, '^')
            if (args.size() == 3) {
                (from, amt, note) = args
                (feeFrom, feeAmt, feeNote) = args
                if (args[0].indexOf('@') != -1) {
                    def fromOperator = CustomerOperator.find(
                            "from CustomerOperator o inner join fetch o.loginCertificate lc where lc.loginCertificate=?",
                            [args[0]]
                    )
                    if (fromOperator) {
                        from = fromOperator.customer.customerNo
                        feeFrom = fromOperator.customer.customerNo
                    } else {
                        throw new MAPIException('ILLEGAL_PARAMETER', "error: $args, from($from) email error.")
                    }
                }
            } else {
                throw new MAPIException('ILLEGAL_PARAMETER', "error refund fee: $feePart")
            }
            if (!from)
                throw new MAPIException('ILLEGAL_PARAMETER', "fee customer no is null")
            if (!(amt ==~ /^(\d{0,8}+)(\.\d{1,2})?$/)) {
                throw new MAPIException("PARAMETER_AMOUNT_ERROR!")
            }
            amt = StringUtil.parseAmountFromStr(amt)
            feeAmt = amt
            if (amt < 0)
                throw new MAPIException('ILLEGAL_PARAMETER', "error amount: $amt")
            if (note.size() > NOTE_MAX_SIZE)
                throw new MAPIException('ILLEGAL_PARAMETER', "note too large: ${note.size()}")
            refundData.feeRfd = [fromCustomerNo: from, amount: amt, subject: note]
        }
        // 解析分润退款信息: 转出人userId^转入人userId^退款金额^退款理由
        for (int i = 1; i < items.size(); i++) {
            def item = items[i]
            args = StringUtil.splitAllTokens(item, '^')
            if (args.size() == 4) {
                (from, to, amt, note) = args
                if (args[0].indexOf('@') != -1) {
                    def fromOperator = CustomerOperator.find(
                            "from CustomerOperator o inner join fetch o.loginCertificate lc where lc.loginCertificate=?",
                            [args[0]]
                    )
                    if (fromOperator) {
                        from = fromOperator.customer.customerNo
                    } else {
                        throw new MAPIException('ILLEGAL_PARAMETER', "error: $item, from($from) email error.")
                    }
                }
                if (args[1].indexOf('@') != -1) {
                    def toOperator = CustomerOperator.find(
                            "from CustomerOperator o inner join fetch o.loginCertificate lc where lc.loginCertificate=?",
                            [args[1]]
                    )
                    if (toOperator) {
                        to = toOperator.customer.customerNo
                    } else {
                        throw new MAPIException('ILLEGAL_PARAMETER', "error: $item, from($to) email error.")
                    }
                }
            } else if (args.size() == 3) {
                (from, amt, note) = args
                if (args[0].indexOf('@') != -1) {
                    def fromOperator = CustomerOperator.find(
                            "from CustomerOperator o inner join fetch o.loginCertificate lc where lc.loginCertificate=?",
                            [args[0]]
                    )
                    if (fromOperator) {
                        from = fromOperator.customer.customerNo
                    } else {
                        throw new MAPIException('ILLEGAL_PARAMETER', "error: $item, from($from) email error.")
                    }
                }
                to = seller
            } else {
                throw new MAPIException('ILLEGAL_PARAMETER', "error: $item")
            }
            if (!from || !to)
                throw new MAPIException('ILLEGAL_PARAMETER', "royalty customer no is null: $item")
            if (!(amt ==~ /^(\d{0,8}+)(\.\d{1,2})?$/)) {
                throw new MAPIException("PARAMETER_AMOUNT_ERROR!")
            }
            amt = StringUtil.parseAmountFromStr(amt)
            if (amt < 0) throw new MAPIException('ILLEGAL_PARAMETER', "error amount: $amt")
            if (note.size() > NOTE_MAX_SIZE) {
                throw new MAPIException('ILLEGAL_PARAMETER', "error: $item, note too large: ${note.size()}")
            }
            def refundDetail = new RefundDetail()

            refundDetail.outTradeNo = otn
            refundDetail.refundAmount = baseAmt
            refundDetail.refundNote = baseNote
            refundDetail.feeNo = feeFrom
            refundDetail.feeAmount = feeAmt
            refundDetail.feeNote = feeNote
            refundDetail.fromCustomerNo = from
            refundDetail.toCustomerNo = to
            refundDetail.amount = amt
            refundDetail.note = note
            refundDetail.refundDate = date
            refundDetail.status = 'processing'
            refundDetail.refundType = refundType
            refundDetail.refundNo = outTradeNo ? outTradeNo : ''
            refundDetail.save flush: true
            if (!refundData.royaltyRefunds) refundData.royaltyRefunds = []
            if (refundData.feeRfd.fromCustomerNo == from) {
                amt = (refundData.feeRfd.amount as int) + (amt as int)
            }
            refundData.royaltyRefunds << [fromCustomerNo: from, toCustomerNo: to, amount: amt, subject: note]
        }
        if (refundData.royaltyRefunds) {
            if (!refundData.feeRfd)
                throw new MAPIException('ILLEGAL_PARAMETER', "royalty refund not feePart.")
            refundData.refundType = 'royalty'
        } else {
            refundData.refundType = 'normal'
        }
        refundData
    }
}
