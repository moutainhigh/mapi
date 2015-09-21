package mapi

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import trade.TradePayment
import ebank.tools.StringUtil
import trade.TradeBase
import trade.TradeRefund
import trade.AccountCommandPackage
import customer.Customer
import customer.CustomerService
import customer.RoyaltyBinding
import ebank.lang.MAPIException
import boss.InnerAccount
import gateway.GwTransaction
import net.sf.json.JSONObject
import customer.RefundDetail
import gateway.GwOrder
import account.AcAccount
import gateway.Gwsuborders

class RefundController extends BaseController {
    def accountClientService
    def noGeneratorService
    def jmsService
    def settleClientService
    def alipayService

    protected setConstraints() {
        //必输字段
        required_attr = ['merchant_ID', ['refund_parameters', 'orig_order_no']]
        //用于查询手续费承担用户
        def gwOrder
        def type
        if (params.order_no != null && params.order_no != '') {
            gwOrder = GwOrder.findByOutTradeNo(params.order_no)
        }
        if (gwOrder != null && gwOrder != '') {
            if (gwOrder.royaltyType == '10') {
                type = '1'
            }
        } else if (params.order_no == null) {
            type = '1'
        }
        if (params.refund_parameters && type == '1') {
            serviceCode = 'royalty'
        }
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
        //退款明细
        def refundDetail
        //当前交易的手续费金额
        def feeMoney = 0
        //已完成同一订单号的手续费金额
        def feeComMoney = 0
        //即时分润退款和异步分润退款
        def refundType = '2'
        def payment
        def refund
        def acPackage = new AccountCommandPackage()
        def tradeList = []
        def tradeRoyalty
        //是否允许垫付
        def advance = params.advance ? params.advance : '0'
        refundData.time = new Date()
        def royaltyType = '0'
        if (params.orig_order_no == null || params.orig_order_no == '') {
            royaltyType = '1'
        } else if (params.orig_order_no != null && params.orig_order_no != '') {
            def gwOrder = GwOrder.findByOutTradeNo(params.orig_order_no)
            if (!gwOrder) {
                throw new MAPIException("ORIG_OUT_TRADE_NO_NOT_EXIST!")
            }
            if (gwOrder.royaltyType == '12') {
                royaltyType = '2'
            } else if (gwOrder.royaltyType == '10') {
                def tradeBase = TradeBase.findByOutTradeNo(params.orig_order_no)
                tradeRoyalty = TradeBase.findAllWhere(rootId: tradeBase.rootId, status: 'completed', tradeType: 'royalty')
                if (tradeRoyalty.size() > 0) {
                    royaltyType = '1'
                }
            }
        }
        //正常退款
        if (params?.amount && !(params?.amount ==~ /^(\d{0,8}+)(\.\d{1,2})?$/)) {
            throw new MAPIException("PARAMETER_AMOUNT_FORMAT")
        }
        def tamount = (params?.amount as double)
        if (tamount && tamount < 0.01) {
            throw new MAPIException("PARAMETER_AMOUT_ZERO");
        }
        if (params.refund_parameters && royaltyType == '1') {//分润退款
            //即时分润退款和异步分润退款
            if (params.refund_type) {
                refundType = params.refund_type
            }
            if (refundType == '1' || refundType.equals('1')) {
                if (!params.order_no) {
                    throw new MAPIException('OUTTRADENO_PARAMETER_NOT_NULL')
                }
            }
            def royaltyService = params._customerService as CustomerService
            // 格式2 refund_parameters
            refundData = ParameterParser.parseRefundParams(params.merchant_ID, params.refund_parameters, refundType, params.order_no)
            //承担手续费参数 例如：{"payfee_customer_no":"100000000000033"}
            def serviceParams = JSONObject.fromObject(royaltyService.serviceParams)
            //手续费承担商户号  例如：“100000000000033”
            def payfeeCusno = serviceParams.payfee_customer_no
            //当前该笔退款的总金额
            def refundTotalMoney = 0
            def sign = 0
            //商户分润退款金额不能大于商户分润金额
            //退款明细
            refundDetail = RefundDetail.findAllByOutTradeNoAndStatus(refundData.origOutTradeNo, 'processing')
            def count = RefundDetail.findAllWhere(outTradeNo: refundData.origOutTradeNo, status: 'processing')
//            def feeCount = RefundDetail.findAllWhere(outTradeNo: refundData.origOutTradeNo, status: 'completed')
            def fromCNo
            def feeCusNo
            if (refundDetail.size() > 0) {
                refundDetail.each {
                    it.status = 'closed'
                    it.save flush: true
                }
            }
            //验证是否是承担手续费账户
            if (refundDetail.size() > 0) {
                refundDetail.each {
                    if (it.feeNo != payfeeCusno) {
                        throw new MAPIException(it.feeNo + '_NOT_FEEAMOUNT_NO')
                    }
                }
            }
            //同一笔记录中不能有相同的商户号且必须有承担手续费的商户。
            if (count.size() > 0) {
                count.each {
                    if (fromCNo == it.fromCustomerNo) {
                        throw new MAPIException(it.fromCustomerNo + '_REPEAT')
                    } else {
                        fromCNo = it.fromCustomerNo
                    }
                    if (it.feeNo == it.fromCustomerNo) {
                        refundTotalMoney += (it.feeAmount as long) + (it.amount as long)
                        feeMoney = it.feeAmount as long
                        sign = 1
                    } else {
                        refundTotalMoney += (it.amount as long)
                    }
                    if (refundData.feeRfd.fromCustomerNo == it.fromCustomerNo) {
                        feeCusNo = it.fromCustomerNo
                    }
                }
                if (sign == 0) {
                    refundTotalMoney = refundTotalMoney + refundData.feeRfd.amount
                }
                if (!feeCusNo) {
                    throw new MAPIException('PARAMETER_NOT_FEEAMOUNT_NO')
                }
            }
//            //已分润退款的手续费
//            if (feeCount.size() > 0) {
//                feeCount.each {
//                    if (it.feeNo == it.fromCustomerNo) {
//                        feeComMoney += (it.feeAmount as long)
//                    }
//                }
//            }
            if (refundData.amount != refundTotalMoney) {
                throw new MAPIException('REFUND_TOTALAMOUNT_UNEQUAL_REFUND_AMOUNT')
            }
            refundData.time = new Date()
            //退款订单号如果不为空则为输入的退款订单号，如果为空则自动生成一个。
            if (params.order_no != null && params.order_no != '') {
                refundData.put('outTradeNo', params.order_no)
            } else {
                refundData.put('outTradeNo', noGeneratorService.createTradeNo('refund', refundData.time))
            }
            log.info "parse refund params: ${params.refund_parameters} to $refundData"
        } else if (royaltyType == '2') {
            if (params.order_no) {
                def tradeBase = TradeBase.findWhere([outTradeNo: params.order_no, tradeType: 'refund', partnerId: partner.id])
                if (tradeBase) {
                    throw new MAPIException('OUT_TRADE_REPEAT')
                }
            }
            refundData = ParameterParser.parseMergeParams(params.orig_order_no, params.refund_parameters)
            refundData.merges.collect {
                it.time = new Date()
                it.refundType = 'normal'
                it.refundParams = 'n/a'
                if (params.order_no != null && params.order_no != '') {
                    it.put('outTradeNo', params.order_no)
                } else {
                    it.put('outTradeNo', noGeneratorService.createTradeNo('refund', new Date()))
                }
                payment = TradePayment.findByTradeNo(it.origOutTradeNo)
                if (payment.royaltyType == '12' && !refundData.merges) {
                    log.error "ILLEGAL_PARAMETER:" + params.orig_order_no
                    throw new MAPIException('ILLEGAL_PARAMETER')
                }
                // 查看原交易的付款方是否为Guest账户，来判断退款是否直接退到付款人账户里
                it.isGuestPayment = (payment.payerAccountNo == InnerAccount.getGuestAccountNo())
                //2012-01-09 新增内容
                it.acquirerMerchantNo = GwTransaction.findByOrder(GwOrder.get(Gwsuborders.get(payment?.paymentRequestId)?.gwordersid))?.acquirerMerchant ?: 'n/a'
                it.acquirerCode = GwTransaction.findByOrder(GwOrder.get(Gwsuborders.get(payment?.paymentRequestId)?.gwordersid))?.acquirerCode ?: 'n/a'
                it.channel = GwTransaction.findByOrder(GwOrder.get(Gwsuborders.get(payment?.paymentRequestId)?.gwordersid))?.channel
                it.trxnum = GwTransaction.findByOrder(GwOrder.get(Gwsuborders.get(payment?.paymentRequestId)?.gwordersid))?.bankTransNo

                // refund payee srvAcct reset
                def payeeAccountNo = payment.payeeAccountNo
                def payeeAccn = CustomerService.createCriteria().list {
                    eq("customerId", payment.payeeId)
                    eq("serviceCode", "online")
                }?.get(0)?.srvAccNo
                log.info("refund:" + payment.payeeId + " " + payeeAccn)
                if (payeeAccn) payment.payeeAccountNo = payeeAccn
                payment.feeAmount = 0
                //在创建refund前要判断现有金额是否够分润退款的，如果不够，且允许垫付的，那么这笔退分润交易由平台垫付，交易状态为已垫付。
                refund = payment.createRefund(it)
                payment.payeeAccountNo = payeeAccountNo   //reset the payeeAccountNo
                log.info("payeeAcct:" + refund.payerAccountNo + " payment:" + payment.id)
                refund.submitType = 'automatic'
                if (it.isGuestPayment) {
                    // 先退到中间过渡账户的逻辑
                    refund.payeeAccountNo = InnerAccount.getMiddleAccountNo()
                    refund.handleStatus = 'waiting'
                }
                //根据payeeID查询customerNo
                def customerNo = Customer.findByIdAndStatus(payment.payeeId, 'normal')?.customerNo
                if (!customerNo) {
                    throw new MAPIException('CUSTOMER_NO_NOT_EXIST!')
                }
                def resp = settleClientService.trade('online', 'refund', customerNo, refund.amount as Long, refund.tradeNo,
                        refund.dateCreated ? refund.dateCreated.format('yyyy-MM-dd HH:mm:ss.SSS') : new Date().format('yyyy-MM-dd HH:mm:ss.SSS'),
                        refund.lastUpdated ? refund.lastUpdated.format('yyyy-MM-dd HH:mm:ss.SSS') : new Date().format('yyyy-MM-dd HH:mm:ss.SSS'))
                if (!resp) {
                    writeResponse 'SYSTEM_BUSY'
                } else if (resp.result == 'true') {
                    saveNomaleRefund(refund, acPackage)
                } else {
                    throw new MAPIException(resp.errorMsg)
                }
            }

        } else if (params.orig_order_no && params.amount && royaltyType == '0') {    //正常退款
            // 格式1 orig_out_trade_no, amount, subject
            if (params.order_no && String.valueOf(params.order_no).length() > 64) {
                throw new MAPIException('OUTTRADENO_PARAMETER_OVERFLOW')
            }
            refundData = [
                    origOutTradeNo: params.orig_order_no,
                    fromCustomerNo: params.merchant_ID,
                    outTradeNo: params.order_no ? params.order_no : noGeneratorService.createTradeNo('refund', refundData.time),
                    amount: StringUtil.parseAmountFromStr(params.amount),
                    subject: (params.subject) ? params.subject : '',
                    refundType: 'normal',
                    refundParams: 'n/a'
            ]
        } else {
            throw new MAPIException('ILLEGAL_PARAMETER')
        }
        //到此退款判断结束
        refundData.time = new Date()
        // 用原商户订单号找到原支付交易
        log.info "refund:" + partner.id + "trade no:" + refundData.origOutTradeNo
        //根据原订单号及ID查询支付交易。
        if (royaltyType != '2') {
            //退款订单号不能重复。
            if (params.order_no) {
                def tradeBase = TradeBase.findWhere([outTradeNo: params.order_no, tradeType: 'refund', partnerId: partner.id])
                if (tradeBase) {
                    throw new MAPIException('OUT_TRADE_REPEAT')
                }
            }
            payment = TradePayment.findByPartnerIdAndOutTradeNo(partner.id, refundData.origOutTradeNo)
            if (!payment) {
                log.error "TRADE_NOT_EXIST:" + params.orig_order_no
                throw new MAPIException('TRADE_NOT_EXIST')
            } else if (payment.status != 'completed') {
                log.error "TRADE_STATUS_NOT_ALLOW" + payment.status + params.orig_order_no
                throw new MAPIException('TRADE_STATUS_NOT_ALLOW')
            }
            if (refundData.amount > (payment.amount - payment.refundAmount)) {
                log.error "GREATER_REFUND_MONEY:" + params.orig_order_no
                throw new MAPIException('GREATER_REFUND_MONEY')
            }
            //2011-09-20 退款不用判断余额
//        if (payment.royaltyType != '10') {
//            //检查账户余额是否足够退款
//            def payeeAccount = accountClientService.queryAcc(payment.payeeAccountNo)
//            if (payeeAccount.result == 'true') {
//                if (payeeAccount.balance < refundData.amount) {
//                    log.error "ACCOUNT_REFUND_MONEY:" + params.orig_order_no
//                    throw new MAPIException('GREATER_BALANCE_MONEY')
//                }
//            } else {
//                throw new MAPIException(payeeAccount.errorMsg)
//            }
//        }
            if (payment.royaltyType == '10' && !refundData.royaltyRefunds && tradeRoyalty.size() > 0) {
                log.error "ILLEGAL_PARAMETER:" + params.orig_order_no
                throw new MAPIException('ILLEGAL_PARAMETER')
            } else if (payment.royaltyType == '10' && refundData.royaltyRefunds) {   //分润退款参数明细判断。

                //分润退款金额不能大于分润金额
                if (refundData.amount > payment.amount) {
                    throw new MAPIException('ROYALTY_AMONUT_ENOUGH')
                }
//            if (feeMoney + feeComMoney > payment.feeAmount) {
//                throw new MAPIException('ROYALTY_FEEAMONUT_ENOUGH')
//            }
                if (refundDetail.size() > 0) {
                    refundDetail.each {
                        //分润金额
                        def royaltyAmount = 0
                        //退款金额
                        def refundAmount = 0
                        //已退金额
                        def refundRoyaltyAmount = 0
                        //未分润的账户不允许分润退款
                        //同一次分润退款不允许有相同的账户
                        def customer = Customer.findByCustomerNo(it.fromCustomerNo)
                        if (!customer) {
                            throw new MAPIException(it.fromCustomerNo + '_NOT_EIXT')
                        }
                        //未分润的账户不允许分润退款
                        def tradeBase = TradeBase.findAllWhere(rootId: payment.rootId, status: 'completed', payeeId: customer.id, tradeType: 'royalty')
                        //已做分润退款
                        def refundTradeBase = TradeBase.findAllWhere(rootId: payment.rootId, status: 'completed', payerId: customer.id, tradeType: 'royalty_rfd')
                        if (tradeBase.size() > 0) {
                            tradeBase.each {
                                royaltyAmount += it.amount
                            }
                        } else {
                            throw new MAPIException(it.fromCustomerNo + '_NOT_ROYALTY_NO')
                        }
                        if (refundTradeBase.size() > 0) {
                            refundTradeBase.each {
                                refundRoyaltyAmount += it.amount
                            }
                        }
                        if (it.feeNo == it.fromCustomerNo) {
                            refundAmount = (it.feeAmount as long) + (it.amount as long)
                            if ((it.feeAmount as long) - (it.refundAmount as long) > 0) {
                                throw new MAPIException(it.feeNo + '_FEE_AMOUNT_TOMUCH')
                            }
                        } else {
                            refundAmount = it.amount
                        }
                        if ((royaltyAmount as long) == (refundRoyaltyAmount as long) && (royaltyAmount as long) != 0) {
                            throw new MAPIException(it.fromCustomerNo + '_REFUND_COMPLETED')
                        }
                        if (((royaltyAmount as long) - (refundRoyaltyAmount as long) as long) < (refundAmount as long)) {
                            throw new MAPIException(it.fromCustomerNo + '_REFUND_AMOUNT_TOMUCH')
                        }
                    }
                } else {
                    throw new MAPIException('OUT_TRADE_NO_EXIST')
                }
            }
            // 查看原交易的付款方是否为Guest账户，来判断退款是否直接退到付款人账户里
            refundData.isGuestPayment = (payment.payerAccountNo == InnerAccount.getGuestAccountNo())
            //创建退款交易表

            // refund payee srvAcct reset
            def payeeAccountNo = payment.payeeAccountNo
            if (payment.royaltyType != '10') {
                def payeeAccn = CustomerService.createCriteria().list {
                    eq("customerId", payment.payeeId)
                    eq("serviceCode", "online")
                }?.get(0)?.srvAccNo
                log.info("refund:" + payment.payeeId + " " + payeeAccn)
                if (payeeAccn) payment.payeeAccountNo = payeeAccn
                payment.feeAmount = 0
            }
            refundData.acquirerCode = GwTransaction.findByOrder(GwOrder.get(payment?.paymentRequestId))?.acquirerCode ?: 'n/a'
            refundData.acquirerMerchantNo = GwTransaction.findByOrder(GwOrder.get(payment?.paymentRequestId))?.acquirerMerchant ?: 'n/a'
            refundData.channel = GwTransaction.findByOrder(GwOrder.get(payment?.paymentRequestId))?.channel ?: 'n/a'
            refundData.trxnum = GwTransaction.findByOrder(GwOrder.get(payment?.paymentRequestId))?.bankTransNo ?: 'n/a'
            //在创建refund前要判断现有金额是否够分润退款的，如果不够，且允许垫付的，那么这笔退分润交易由平台垫付，交易状态为已垫付。
            refund = payment.createRefund(refundData)
            payment.payeeAccountNo = payeeAccountNo   //reset the payeeAccountNo
            log.info("payeeAcct:" + refund.payerAccountNo + " payment:" + payment.id)
            //退款手续费按比例来。
            if (payment.royaltyType == '10' && refundData.royaltyRefunds) {
                if (refund.backFee != refundData.feeRfd.amount) {
                    throw new MAPIException('FEE_AMOUNT_ERROR')
                }
            }
            refund.submitType = 'automatic'

            if (refundData.isGuestPayment) {
                // 先退到中间过渡账户的逻辑
                refund.payeeAccountNo = InnerAccount.getMiddleAccountNo()
                refund.handleStatus = 'waiting'
                // 查找原来的交易信息
                def gwTransactions = GwTransaction.find(
                        "from GwTransaction where order.id=? and order.partnerCustomerNo=? and status='1' order by completionTime asc", [payment.tradeNo, params.merchant_ID]
                )
                if (!gwTransactions) {
                    throw new MAPIException('TRADE_NOT_EXIST')
                }
                refund.acquirerCode = gwTransactions.acquirerCode
                refund.acquirerMerchantNo = gwTransactions.acquirerMerchant
                refund.acquirerAccountId = gwTransactions.acquirerInnerAccountName as Long
            }
            if (refundData.refundType == 'royalty') {
                def customerService = CustomerService.find(
                        "from CustomerService where customerId=? and serviceCode=? and enable=true and isCurrent=true",
                        [partner.id, 'royalty'])
                if (!customerService) {
                    throw new MAPIException('SERVICE_NOT_SUPPORT')
                }
                if (advance == '1') {
                    def totalAmount = 0
                    refundData.royaltyRefunds.each {
                        totalAmount += it.amount
                    }
                    def account = AcAccount.findByAccountNo(Customer.get(refund.payerId)?.accountNo)
                    if (!account) {
                        throw new MAPIException('ACCOUNT_NOT_EXIST')
                    }
                    if (totalAmount > account.balance) {
                        throw new MAPIException('AVAILABLE_AMOUNT_NOT_ENOUGH!')
                    }
                }
                tradeList = createAndSaveRoyaltyRefund(partner, refund, refundData, acPackage, refundType, payment, advance)
            } else {
                //非分润退款保存
                def resp = settleClientService.trade('online', 'refund', Customer.get(payment.partnerId).customerNo, refund.amount as Long, refund.tradeNo,
                        refund.dateCreated ? refund.dateCreated.format('yyyy-MM-dd HH:mm:ss.SSS') : new Date().format('yyyy-MM-dd HH:mm:ss.SSS'),
                        refund.lastUpdated ? refund.lastUpdated.format('yyyy-MM-dd HH:mm:ss.SSS') : new Date().format('yyyy-MM-dd HH:mm:ss.SSS'))
                if (!resp) {
                    writeResponse 'SYSTEM_BUSY'
                } else if (resp.result == 'true') {
                    saveNomaleRefund(refund, acPackage)
                } else {
                    throw new MAPIException(resp.errorMsg)
                }
            }
        }
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
                if (royaltyType == '2') {
                    refundData.merges.each {
                        payment = TradePayment.findByTradeNo(it.origOutTradeNo)
                        refund = TradeRefund.findAllByOriginalId(payment.id)
                        if (refundType == '2' || '2'.equals(refundType)) {
                            payment.refundAmount += it.amount
                        }
                        if (payment.amount == payment.refundAmount) {
                            payment.status = 'closed'
                        }
                        payment.save failOnError: true
                        if (refundType == '2' || '2'.equals(refundType)) {
                            refund.each {
                                it.status = (refundData.isGuestPayment) ? 'processing' : 'completed'
                                it.handleStatus = (refundData.isGuestPayment) ? refund.handleStatus : 'completed'
                                it.save failOnError: true
                            }
                        }
                        def isGustAcc = InnerAccount.findByAccountNoAndKey(payment.payerAccountNo, 'guestAcc')
                    }
                } else {
                    if (refundType == '2' || '2'.equals(refundType)) {
                        payment.refundAmount += refundData.amount
                    }
                    if (payment.amount == payment.refundAmount) {
                        payment.status = 'closed'
                    }
                    payment.save failOnError: true
                    if (refundType == '2' || '2'.equals(refundType)) {
                        refund.status = (refundData.isGuestPayment) ? 'processing' : 'completed'
                        refund.handleStatus = (refundData.isGuestPayment) ? refund.handleStatus : 'completed'
                        refund.save failOnError: true
                    }
                    tradeList.each { trade ->
                        trade.status = 'completed'
                        trade.save failOnError: true
                    }
                    //如果交易成功，则把明细状态改为‘completed’
                    if (payment.royaltyType == '10' && refundData.royaltyRefunds) {
                        if (refundDetail.size() > 0) {
                            refundDetail.each {
                                it.status = 'completed'
                                it.save flush: true
                            }
                        }
                    }
                    def isGustAcc = InnerAccount.findByAccountNoAndKey(payment.payerAccountNo, 'guestAcc')
                }
                //2011-11-02修改，清结算在之前做了，这就不用了。
//                if (!isGustAcc && payment.royaltyType != '10') {
//                    javax.jms.MapMessage message = jmsService.createMapMessage()
//                    message.setString('srvCode', 'online')
//                    message.setString('tradeCode', 'refund')
//                    message.setString('customerNo', Customer.get(payment.partnerId).customerNo)
//                    message.setLong('amount', refund.amount as Long)
//                    message.setString('seqNo', refund.tradeNo)
//                    message.setString('tradeDate', refund.dateCreated.format('yyyy-MM-dd HH:mm:ss.SSS'))
//                    message.setString('billDate', refund.lastUpdated ? refund.lastUpdated.format('yyyy-MM-dd HH:mm:ss.SSS') : new Date().format('yyyy-MM-dd HH:mm:ss.SSS'))
//                    jmsService.send(message)
//                }
                writeResponse 'SUCCESS'
            } else {
                if (royaltyType == '2') {
                    refundData.merges.each {
                        payment = TradePayment.findByTradeNo(it.origOutTradeNo)
                        refund = TradeBase.findByOriginalId(payment.id)
                        if (refundType == '2' || '2'.equals(refundType)) {
                            refund.status = 'closed'
                            refund.save failOnError: true
                        }
                    }
                } else {
                    if (refundType == '2' || '2'.equals(refundType)) {
                        refund.status = 'closed'
                        refund.save failOnError: true
                    }
                    tradeList.each { trade ->
                        trade.status = 'closed'
                        trade.save failOnError: true
                    }
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
    protected saveNomaleRefund(TradeRefund refund, acPackage) {
        TradeBase.withTransaction { trx ->
            refund.save flash: true, failOnError: true
            if (refund.backFee > 0) {
                acPackage.append(
                        tradeId: refund.id,
                        tradeNo: refund.tradeNo,
                        outTradeNo: refund.outTradeNo,
                        fromAccountNo: InnerAccount.getFeeAccountNo(),
                        toAccountNo: refund.payerAccountNo,
                        amount: refund.backFee,
                        currency: refund.currency,
                        transferType: 'fee_rfd'
                )
            }
            acPackage.append refund
            acPackage.save()
        }
    }

/**
 * 创建并保存分润退款交易
 * @return
 */
    protected createAndSaveRoyaltyRefund(Customer partner, TradeRefund refund, refundData, acPackage, refundType, payment, advance) {
        def cache = [:]
        def customerCache = { customerNo ->
            Customer cus = cache[customerNo]
            if (!cus) {
                if (customerNo == partner.customerNo) {
                    cus = partner
                } else {
                    cus = Customer.findByCustomerNo(customerNo);
                    if (!cus || cus.status != 'normal') {
                        throw new MAPIException('USER_NOT_EXIST')
                    }
                    def bind = RoyaltyBinding.findWhere([partner: partner, customer: cus, bizType: '10', status: 'bind', nopassRefundFlag: 'T'])
                    if (!bind) {
                        throw new MAPIException('BINDING_NOT_EXIST')
                    }
//                    def bind = RoyaltyBinding.findByPartnerAndCustomer(partner, cus)
//                    if (!bind || (bind.status != 'bind' && bind.bizType == '10') || bind.nopassRefundFlag != 'T') {
//                        throw new MAPIException('BINDING_NOT_EXIST')
//                    }
                }
                cache[customerNo] = cus
            }
            cus
        }
//        def royaltyRefunds = refund.createRoyaltyRefund(refundData, customerCache)
        def royaltyRefunds
        TradeBase.withTransaction { trx ->
            if (refundType == '2' || '2'.equals(refundType)) {refund.save flash: true, failOnError: true}
            royaltyRefunds = refund.createRoyaltyRefund(refundData, customerCache, advance)
            if (!royaltyRefunds) {
                throw new MAPIException('AVAILABLE_AMOUNT_NOT_ENOUGH!')
            }
            if (refundType == '1' || '1'.equals(refundType)) {
                refund.id = payment.id
                refund.outTradeNo = payment.outTradeNo
                refund.tradeNo = payment.tradeNo
            }
            //2011-08-25删除，分润退款在审批通过时收取手续费。
            if (refund.backFee > 0) {
                acPackage.append(
                        tradeId: refund.id,
                        tradeNo: refund.tradeNo,
                        outTradeNo: refund.outTradeNo,
                        fromAccountNo: InnerAccount.getFeeAccountNo(),
                        toAccountNo: customerCache(refundData.feeRfd.fromCustomerNo).accountNo,
                        amount: refund.backFee,
                        currency: refund.currency,
                        transferType: 'fee_rfd'
                )
            }
            royaltyRefunds.each { royaltyRfd ->
                royaltyRfd.save flash: true, failOnError: true
                if (royaltyRfd.amount > 0) {
                    acPackage.append royaltyRfd
                }
            }
            if (refundType == '2' || '2'.equals(refundType)) {
                acPackage.append refund
            }
            acPackage.save()
        }
        royaltyRefunds
    }

/**
 * 支付宝退款回调处理
 */
    def processRespResult =
    {
        println "支付宝返回结果处理开始："
        String backResult = alipayService.processRespResult(request, response)
        response.getWriter().print(backResult)
    }
}
