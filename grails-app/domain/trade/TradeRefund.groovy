package trade

import account.AcAccount
import customer.CustomerOperator
import customer.Customer

class TradeRefund extends TradeBase {
    def noGeneratorService

    String acquirerCode = 'n/a'
    String acquirerMerchantNo = 'n/a'
    Long acquirerAccountId = 0
    String submitBatch = 'n/a'
    String submitType = 'automatic'
    String submitter = 'n/a'
    Long backFee = 0
    Long realRefundAmount = 0
    String refundType = 'normal'
    String refundParams = 'n/a'
    String royaltyRefundStatus = 'starting'
    String checkStatus = 'starting'
    String handleStatus = 'waiting'
    String refundFlag
    //2012-01-09新增
    String channel
    String trxnum
    //交易表支付宝流水号
    String acquirerSeq
    String  handleBatch

    static constraints = {
        submitBatch nullable: true
        submitType inList: ['manual', 'automatic']
        submitter nullable: true
        backFee min: 0L
        realRefundAmount nullable: true, min: 0L
        refundType inList: ['normal', 'royalty']
        refundParams nullable: true
        royaltyRefundStatus inList: ['starting', 'processing', 'completed', 'closed'], nullable: true
        handleStatus inList: ['waiting', 'checked', 'submited', 'completed'], nullable: true
        refundFlag nullable: true
        channel(nullable: true)
        trxnum(nullable: true)
        acquirerSeq(nullable: true)
        handleBatch(maxSize: 16,nullable: true)
    }

    def createRoyaltyRefund(refundData, customerCache, advance) {
        refundData.royaltyRefunds.collect { item ->
            def _payeeId = customerCache(item.toCustomerNo).id
            def _payeeAccountNo = (_payeeId == partnerId) ? payerAccountNo : customerCache(item.toCustomerNo).accountNo
            def _payerAccountNo = customerCache(item.fromCustomerNo).accountNo
            def _payerCode = CustomerOperator.findByCustomerAndStatus(customerCache(item.fromCustomerNo), 'normal').defaultEmail
            def _payerId = customerCache(item.fromCustomerNo).id
            def _payerName = customerCache(item.fromCustomerNo).name
            def flag
            def sign = '0'
            def money = AcAccount.findByAccountNoAndStatus(_payerAccountNo, 'norm').balance
            def amount = AcAccount.findByAccountNo(Customer.get(_payeeId)?.accountNo).balance
            if (advance == '1' && money < item.amount && amount >= item.amount) {
                flag = '垫付-' + _payerAccountNo
                _payerAccountNo = Customer.findByCustomerNo(item.toCustomerNo)?.accountNo
                sign = '1'
            }
            new TradeRefund(
                    rootId: rootId,
                    originalId: id,
                    outTradeNo: refundData.outTradeNo ? refundData.outTradeNo : noGeneratorService.createTradeNo('refund', refundData.time),
                    tradeType: 'royalty_rfd',
                    tradeNo: noGeneratorService.createTradeNo('royalty_rfd', refundData.time),
                    partnerId: partnerId,
                    payerId: _payerId,
                    payerName: _payerName,
                    payerAccountNo: _payerAccountNo,
                    payerCode: _payerCode,
                    payeeId: _payeeId,
                    payeeName: customerCache(item.toCustomerNo).name,
                    payeeAccountNo: _payeeAccountNo,
                    amount: item.amount,
                    currency: currency,
                    status: 'starting',
                    subject: item.subject,
                    tradeDate: tradeDate,
                    note: flag,
                    refundType: 'royalty',
                    refundFlag: sign,
                    acquirerCode: acquirerCode,
                    acquirerMerchantNo: acquirerMerchantNo,
                    channel:channel,
                    trxnum:trxnum
            )
        }
    }
}
