package mapi

import customer.Customer
import customer.CustomerOperator
import customer.RoyaltyBinding
import ebank.tools.FormFunction
import ebank.tools.StringUtil
import customer.BindingMoney
import ebank.lang.MAPIException

class SignSearchController extends BaseController {
    protected setConstraints() {
        required_attr = ['partner', 'email']
        serviceCode = 'selfSign'
    }

    protected execute() {

        log.info 'in signSearch'
        def partner = params._partner
        def qy_params = [
                partner: params.merchant_ID,
                email: params.email,
                return_url: params.return_url,
                sign_type: params.sign_type,
                input_charset: params._input_charset
        ]

        def operator = CustomerOperator.find(
                "from CustomerOperator o inner join fetch o.loginCertificate lc where lc.loginCertificate=?",
                [params.email]
        )
        if (!operator) {
            // 用户名密码错误
            writeResponse 'EMAIL_FAULT'
            return
        } else if (operator.status != 'normal') {
            // 用户被锁定或禁用
            writeResponse 'EMAIL_LOCKED'
            return
        }
        def slave = operator.customer
        if (slave.status in ['disabled', 'deleted']) {
            // 客户已经被停用
            writeResponse 'CUSTOMER_STOPPED'
            return
        }
        def par = Customer.get(partner.id)
//        def binding = RoyaltyBinding.findAllWhere([partner: par, customer: slave, bizType: '11', nopassRefundFlag: 'T'])
        def bind = RoyaltyBinding.findWhere(partner: par, customer: slave, bizType: '11', nopassRefundFlag: 'T', status: 'sign',outCustomerCode:params.email )
        if (!bind) {
            bind = RoyaltyBinding.findWhere(partner: par, customer: slave, bizType: '11', nopassRefundFlag: 'T', status: 'del',outCustomerCode:params.email)
        }
        if (bind != null) {
            writeTrade((bind ? 'SUCCESS' : 'TRADE_NOT_EXIST'), bind, params.sign_type, params.sign.toString())
            log.info 'query signSearch end'
        }
        if(bind==null){
            writeResponse 'BINDING_NOT_EXIST'
        }
    }

    protected writeTrade(String resultCode, RoyaltyBinding binding, String signType, String Sign) {
        def isSuccess = (resultCode == 'SUCCESS') ? 'T' : 'F'
        def charset = params._input_charset
        if (!charset) charset = 'utf8'
        render(contentType: "text/xml", encoding: charset) {
            ebank {
                is_success(isSuccess)
                result_code(resultCode)
                timestamp(StringUtil.getFullDateTime())
                trades {
                    for (_trade in binding) {
                        trade {
                            customer_no(_trade.outCustomerCode)
                            is_protocol(_trade.status == 'sign' ? '已签约' : '已解约')
                            sign(Sign)
                            sign_type(signType)
                        }
                    }
                }
            }
        }
    }
}
