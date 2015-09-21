package trade

import ebank.tools.StringUtil

class TradePayment extends TradeBase {
    def noGeneratorService

    Long    paymentRequestId
    Long    refundAmount    = 0
    Long    frozenAmount    = 0
    String  paymentIp
    String  showUrl
    String  body
    String  outRoyaltyTradeNo
    String  royaltyType
    String  royaltyParams
    String  royaltyStatus

    static constraints = {
        paymentRequestId(nullable: true)
        paymentIp(nullable: true)
        showUrl(nullable: true)
        body(nullable: true)
        outRoyaltyTradeNo nullable: true
        royaltyType nullable: true
        royaltyParams nullable: true
        royaltyStatus nullable: true, inList: ['starting','processing','completed','closed']
    }

    static mapping = {
    }

    def createRefund( refundData ) {
        new TradeRefund(
                rootId          : rootId,
                originalId      : id,
                tradeType       : 'refund',
                tradeNo         : noGeneratorService.createTradeNo('refund', refundData.time),
                outTradeNo      : refundData?.outTradeNo ,
                partnerId       : partnerId,
                payerId         : payeeId,
                payerCode       : payeeCode,
                payerName       : payeeName,
                payerAccountNo  : payeeAccountNo,
                payeeId         : payerId,
                payeeCode       : payerCode,
                payeeName       : payerName,
                payeeAccountNo  : payerAccountNo,
                amount          : refundData.amount,
                currency        : currency,
                status          : 'starting',
                subject         : refundData.subject,
                tradeDate       : StringUtil.getNumericDate(refundData.time) as int,
                backFee         : (refundData.amount * feeAmount/amount) as Long,
                refundType      : refundData.refundType,
                refundParams    : refundData.refundParameters,
                acquirerCode    : refundData.acquirerCode,
                acquirerMerchantNo     : refundData.acquirerMerchantNo,
                channel         :refundData.channel,
                trxnum          :refundData.trxnum
        )
    }
}
