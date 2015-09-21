package mapi

import ebank.lang.MAPIException
import ebank.tools.StringUtil
import trade.TradeBase
import trade.TradePayment

class AdvanceSearchController extends BaseController {
    protected setConstraints() {
        required_attr = ['partner', 'orig_out_trade_no']
    }

    protected execute() {
        log.info 'in query refund'
        def partner = params._partner
        //根据原订单号查询退款
        def payment = TradePayment.findWhere(partnerId: partner.id, outTradeNo: params.orig_order_no, royaltyType: '10')
        if (!payment) {
            throw new MAPIException('ORIG_OUT_TRADE_NO_NOT_EXIT')
        }
        def query = {
            if (params.order_no != null && params.order_no != '') {
                eq('outTradeNo', params.order_no)
            }
            if (params.trade_no != null && params.trade_no != '') {
                eq('tradeNo', params.trade_no)
                or {
                    eq('tradeType', 'refund')
                    eq('tradeType', 'royalty_rfd')
                }
            } else {
                eq('tradeType', 'refund')
            }
            eq('partnerId', partner.id)
            eq('rootId', payment.rootId)
        }
//        def count = TradeBase.createCriteria().count(query)
        def refund = TradeBase.createCriteria().list(query)
        def que = {
            if (params.order_no != null && params.order_no != '') {
                eq('outTradeNo', params.order_no)
            }
            if (params.trade_no != null && params.trade_no != '') {
                eq('tradeNo', params.trade_no)
            }
            eq('partnerId', partner.id)
            eq('tradeType', 'royalty_rfd')
            eq('rootId', payment.rootId)
        }
        def royRefund = TradeBase.createCriteria().list(que)
        writeTrade((refund ? 'SUCCESS' : 'TRADE_NOT_EXIST'), refund, payment.outTradeNo, royRefund)
        log.info 'query trade end'
    }

    protected writeTrade(String resultCode, List refunds, String tradeNo, List royRefund) {
        def isSuccess = (resultCode == 'SUCCESS') ? 'T' : 'F'
        def charset = params._input_charset
        if (!charset) charset = 'utf8'
        switch (params.return_type) {
            case 'json':
                render(contentType: "text/json", encoding: charset) {
                    ebank {
                        is_success = isSuccess
                        result_code = resultCode
                        timestamp = StringUtil.getFullDateTime()
                        trades = array {
                            for (_trade in refunds) {
                                trade = {
                                    trade_no = _trade.tradeNo
                                    out_trade_no = _trade.outTradeNo
                                    orig_out_trade_no = tradeNo
                                    trade_type = _trade.tradeType
                                    amount = StringUtil.getAmountFromNum(_trade.amount)
                                    created_time = StringUtil.getFullDateTime(_trade.dateCreated)
                                    status = _trade.status
                                    for (_royRefund in royRefund) {

                                        if (_royRefund.originalId == _trade.id) {
                                            royRefund = {
                                                trade_no = _royRefund.tradeNo
                                                out_trade_no = _royRefund.outTradeNo
                                                orig_out_trade_no = tradeNo
                                                payer_code = _royRefund.payerCode
                                                if (_royRefund.note == null) {
                                                    trade_type = _royRefund.tradeType
                                                } else if (_royRefund.note.toString().indexOf('垫付-') != -1) {
                                                    trade_type = 'advance'
                                                } else if (_royRefund.note.toString().indexOf('还款-') != -1) {
                                                    trade_type = 'repayment'
                                                }
                                                amount = StringUtil.getAmountFromNum(_royRefund.amount)
                                                created_time = StringUtil.getFullDateTime(_royRefund.dateCreated)
                                                status = _royRefund.status
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                break
            case 'xml':
            default:
                render(contentType: "text/xml", encoding: charset) {
                    ebank {
                        is_success(isSuccess)
                        result_code(resultCode)
                        timestamp(StringUtil.getFullDateTime())
                        trades {
                            for (_trade in refunds) {
                                trade {
                                    trade_no(_trade.tradeNo)
                                    out_trade_no(_trade.outTradeNo)
                                    orig_out_trade_no(tradeNo)
                                    trade_type(_trade.tradeType)
                                    amount(StringUtil.getAmountFromNum(_trade.amount))
                                    created_time(StringUtil.getFullDateTime(_trade.dateCreated))
                                    status(_trade.status)
                                    for (_royRefund in royRefund) {
                                        if (_royRefund.originalId == _trade.id) {
                                            trade {
                                                trade_no(_royRefund.tradeNo)
                                                out_trade_no(_royRefund.outTradeNo)
                                                orig_out_trade_no(tradeNo)
                                                payer_code(_royRefund.payerCode)
                                                if (_royRefund.note == null) {
                                                    trade_type(_royRefund.tradeType)
                                                } else if (_royRefund.note.toString().indexOf('垫付-') != -1) {
                                                    trade_type('advance')
                                                } else if (_royRefund.note.toString().indexOf('还款-') != -1) {
                                                    trade_type('repayment')
                                                }
                                                amount(StringUtil.getAmountFromNum(_royRefund.amount))
                                                created_time(StringUtil.getFullDateTime(_royRefund.dateCreated))
                                                status(_royRefund.status)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                break
        }
    }
}
