package mapi

import ebank.lang.MAPIException
import ebank.tools.StringUtil
import trade.TradeBase
import trade.TradePayment
import customer.CustomerOperator
import customer.Customer
import customer.LoginCertificate
import customer.RoyaltyBinding

class RoyaltyFrozenSearchController extends BaseController {
    protected setConstraints() {
        required_attr = ['partner', 'orig_out_trade_no', 'out_trade_no', 'customer_no']
    }

    protected execute() {
        log.info 'in query royaltyFrozen'
        def partner = params._partner
        def customer
        def unFrozen = false
        def fro = false
        def customerNo
        if (params.customer_no.toString().indexOf('@') != -1) {
            customer = Customer.get(CustomerOperator.findByIdAndStatus(LoginCertificate.findByLoginCertificate(params.customer_no).customerOperator.id, 'normal').customer.id)
        } else {
            customer = Customer.findByCustomerNo(params.customer_no)
        }
        if (!customer || customer.status != 'normal') {
            throw new MAPIException('USER_NOT_EXIST')
        }
        def bind = RoyaltyBinding.findWhere(partner: partner, customer: customer, bizType: '10', status: 'bind', nopassRefundFlag: 'T')
        if (!bind) {
            throw new MAPIException('BINDING_NOT_EXIST')
        }
        //
        def tradePayment=TradePayment.findWhere(outRoyaltyTradeNo:params.orig_order_no,royaltyType: '10')
        if(!tradePayment){
            throw new MAPIException('ORIG_OUT_TRADE_NO_NOT_EXIST')
        }
        //根据原分润支付订单号查询分润交易，判断冻结金额不能大于分润金额
        def royalty = TradeBase.findAllWhere(partnerId: partner.id, outTradeNo: params.order_no, status: 'completed',rootId:tradePayment?.rootId )
        if (royalty.size() > 0) {
            royalty.each {
                if (it.tradeType == 'unfrozen') {
                    unFrozen = true
                    customerNo=it.payeeId
                } else if (it.tradeType == 'frozen') {
                    fro = true
                    customerNo=it.payeeId
                }
            }
        } else {
            throw new MAPIException('OUT_TRADE_NO_NOT_EXIST')
        }
        if(customerNo!=customer.id) {
            throw new MAPIException('CUSTOMER_NO_NOT_EXIST')
        }
        //根据原订单号查询退款
        def type
        if (unFrozen) {
            type = 'unfrozen'
            writeTrade((royalty ? 'SUCCESS' : 'TRADE_NOT_EXIST'), royalty, type,params.orig_order_no)
        } else if(fro){
            type = 'frozen'
            writeTrade((royalty ? 'SUCCESS' : 'TRADE_NOT_EXIST'), royalty, type,params.orig_order_no)
        }else {
            throw new MAPIException('OUT_TRADE_NO_NOT_EXIST')
        }
        log.info 'query trade end'
    }

    protected writeTrade(String resultCode, List royalty, String type,String tradeNo) {
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
                            for (_trade in royalty) {
                                trade = {
                                    trade_no = _trade.tradeNo
                                    out_trade_no = _trade.outTradeNo
                                    orig_out_trade_no = tradeNo
                                    trade_type = type
                                    amount = StringUtil.getAmountFromNum(_trade.amount)
                                    created_time = StringUtil.getFullDateTime(_trade.dateCreated)
                                    status = _trade.status
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
                            for (_trade in royalty) {
                                trade {
                                    trade_no(_trade.tradeNo)
                                    out_trade_no(_trade.outTradeNo)
                                    orig_out_trade_no(tradeNo)
                                    trade_type(type)
                                    amount(StringUtil.getAmountFromNum(_trade.amount))
                                    created_time(StringUtil.getFullDateTime(_trade.dateCreated))
                                    status(_trade.status)
                                }
                            }
                        }
                    }
                }
                break
        }
    }

}
