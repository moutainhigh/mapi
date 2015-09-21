package customer

class RefundDetail {
    // 支付订单号
    String outTradeNo
    //单笔退款总金额
    String refundAmount
    //退款原因
    String refundNote
    //手续费承担商户
    String feeNo
    //手续费金额
    String feeAmount
    //手续费说明
    String feeNote
    //退款支付商户
    String fromCustomerNo
    //退款收款商户
    String toCustomerNo
    //退款金额
    String amount
    //退款说明
    String note
    //退款时间
    Date refundDate
    //退款状态 [completed:退款记录完成，closed：退款记录关闭]
    String status
    //是否即时退款
    String refundType
    //退款订单号
    String refundNo
    static constraints = {
    }
    static mapping = {
        table 'cm_refund_detail'
        id generator: 'sequence', params: [sequence: 'seq_cm_refund_detail']
    }
}
