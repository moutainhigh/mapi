package mapi

import trade.TradePayment
import ebank.tools.StringUtil
import trade.TradeBase
import gateway.GwOrder

class QueryPaymentController extends BaseController {
    protected setConstraints() {
        required_attr = ['merchant_ID', ['trade_no', 'order_no']]
    }

    protected execute() {
        log.info 'in query payment'
        def partner = params._partner
        log.info "#partner#${partner}"
        def payment = null
        if (params.trade_no) {
            payment = TradePayment.findByPartnerIdAndTradeNo(partner.id, params.trade_no)
            writeTrade( (payment ? 'SUCCESS' : 'TRADE_NOT_EXIST'), payment, null, '1')
            return
        } else if (params.order_no) {
            payment = TradePayment.findByPartnerIdAndOutTradeNo(partner.id, params.order_no)
            if(payment && payment.royaltyType == '12'){// 不查询合单支付子订单
                payment = null
            }else if(payment){
                writeTrade('SUCCESS', payment, null, '1')
                return
            }
        }

        log.info "#payment1#${payment}"
        if(!payment && params.order_no){
            log.info "#params.merchant_ID#${params.merchant_ID}#params.order_no#${params.order_no}"
            payment = GwOrder.findByOutTradeNoAndPartnerCustomerNo(params.order_no, params.merchant_ID) //GwOrder.findByPartnerCustomerNoAndOutTradeNo(params.merchant_ID, params.order_no)
            log.info "#payment2#${payment}"
            //payment = GwOrder.findById('101106200000218')
            //log.info "#payment#test_ismp#${payment}"
            //payment = GwOrder.findById('101202280021270')
            //log.info "#payment#ismp#${payment}"
            if(payment){
                writeTrade('SUCCESS', null, payment, '2')
            }else{
                writeTrade('TRADE_NOT_EXIST', null, null, '2')
            }
        }

        log.info 'query trade end'
    }

    private getStatus(String s){
        String rst = ''
        switch (s){
            case '0':
                rst = 'wait'
                break
            case '1':

            case '2':

            case '3':
                rst = 'completed'
                break
            case '4':
                rst = 'close'
                break
            case '5':
                rst = 'failed'
                break
        }

        rst
    }

    protected writeTrade(String resultCode, TradeBase _trade, GwOrder _order, String type) {
        log.info "writeTrade start"

        def isSuccess = (resultCode=='SUCCESS') ? 'T' : 'F'
        def charset = params._input_charset
        if (!charset) charset = 'utf8'

        if('1' == type){
            log.info "#_trade#${_trade}#type#${type}"
            switch ( params.return_type ) {
                case 'json':
                    log.info "_trade#json"
                    render (contentType: "text/json", encoding: charset) {
                        ebank {
                            is_success  = isSuccess
                            result_code = resultCode
                            timestamp   = StringUtil.getFullDateTime()
                            if (_trade) {
                                trade = [
                                        trade_no     : _trade.tradeNo,
                                        out_trade_no : _trade.outTradeNo,
                                        trade_type   : _trade.tradeType,
                                        amount       : StringUtil.getAmountFromNum(_trade.amount),
                                        fee_amount   : StringUtil.getAmountFromNum(_trade.feeAmount),
                                        subject      : _trade.subject ? _trade.subject : '',
                                        trade_date   : _trade.tradeDate,
                                        created_time : StringUtil.getFullDateTime(_trade.dateCreated),
                                        status       : _trade.status
                                ]
                            }
                        }
                    }
                    break
                case 'xml':
                default:
                    log.info "_trade#xml"
                    render (contentType: "text/xml", encoding: charset) {
                        ebank {
                            is_success ( isSuccess )
                            result_code ( resultCode )
                            timestamp ( StringUtil.getFullDateTime() )
                            if (_trade) {
                                trade {
                                    trade_no        (_trade.tradeNo)
                                    out_trade_no    (_trade.outTradeNo)
                                    trade_type      (_trade.tradeType)
                                    amount          (StringUtil.getAmountFromNum(_trade.amount))
                                    fee_amount      (StringUtil.getAmountFromNum(_trade.feeAmount))
                                    subject         (_trade.subject ? _trade.subject : '')
                                    trade_date      (_trade.tradeDate)
                                    created_time    (StringUtil.getFullDateTime(_trade.dateCreated))
                                    status          (_trade.status)
                                }
                            }
                        }
                    }
                    break
            }
        }else if('2' == type){
            log.info "#_order#${_order}#type#${type}"
            switch ( params.return_type ) {
                case 'json':
                    log.info "_order#json"
                    render (contentType: "text/json", encoding: charset) {
                        ebank {
                            is_success  = isSuccess
                            result_code = resultCode
                            timestamp   = StringUtil.getFullDateTime()
                            if (_order) {
                                trade = [
                                        trade_no     : _order.id,
                                        out_trade_no : _order.outTradeNo,
                                        trade_type   : 'payment',
                                        amount       : StringUtil.getAmountFromNum(_order.amount),
                                        fee_amount   : StringUtil.getAmountFromNum(0L),
                                        subject      : _order.subject ? _order.subject : '',
                                        trade_date   : _order.orderdate,
                                        created_time : StringUtil.getFullDateTime(_order.dateCreated),
                                        status       : getStatus(_order.status)
                                ]
                            }
                        }
                    }
                    break
                case 'xml':
                default:
                    render (contentType: "text/xml", encoding: charset) {
                        ebank {
                            is_success ( isSuccess )
                            result_code ( resultCode )
                            timestamp ( StringUtil.getFullDateTime() )
                            if (_order) {
                                trade {
                                    trade_no        (_order.id)
                                    out_trade_no    (_order.outTradeNo)
                                    trade_type      ('payment')
                                    amount          (StringUtil.getAmountFromNum(_order.amount))
                                    fee_amount      (StringUtil.getAmountFromNum(0L))
                                    subject         (_order.subject ? _order.subject : '')
                                    trade_date      (_order.orderdate)
                                    created_time    (StringUtil.getFullDateTime(_order.dateCreated))
                                    status          (getStatus(_order.status))
                                }
                            }
                        }
                    }
                    break
            }
        }

        log.info "writeTrade end"
    }
}
