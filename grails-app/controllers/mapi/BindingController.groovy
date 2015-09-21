package mapi

import ebank.lang.MAPIException
import customer.Customer
import customer.RoyaltyBinding
import ebank.tools.StringUtil
import ebank.tools.FormFunction
import customer.LoginCertificate
import customer.CustomerOperator

class BindingController extends BaseController {
    protected setConstraints() {
        required_attr = ['partner', 'out_customer_code', 'return_url']
        serviceCode = 'royalty'
    }

    def index = {
        log.info 'in binding'
        try {
            setConstraints()
            decodeUrlQuery()
            check()
            def partner = params._partner
            session.partner = [
                    id: partner.id,
                    name: partner.name,
                    apiKey: partner.apiKey
            ]
            session.bd_params = [
                    partner: params.merchant_ID,
                    out_customer_code: params.out_customer_code,
                    return_url: params.return_url,
                    sign_type:params.sign_type
            ]
        } catch (MAPIException e) {
            log.error e, e
            flash.putAll(errorCode: e.errorCode, errorMessage: '参数提交错误，请联系技术人员')
            render view: '/errorPage'
            return
        } catch (e) {
            log.error e, e
            flash.putAll(errorCode: 'ILLEGAL_PARAMETER', errorMessage: '参数提交错误，请联系技术人员')
            render view: '/errorPage'
            return
        }
        log.info 'binding end'
    }

    def confirm = {
        try {
            if (!session.partner || !session.bd_params) {
                flash.putAll(errorCode: 'WEBERR_TIMEOUT', errorMessage: '操作超时啦，请返回分销平台重新发起绑定')
                render view: '/errorPage'
                return
            }
            if (params.nopassRefundFlag != 'true') {
                flash.errorMessage = '参加绑定必须同时签订无故障退款协议，请确认!'
                render view: 'index'
                return
            }
            log.info params
            // TODO 验证码
            def formVerify = withForm { session.captcha?.isCorrect(params.captcha) }.invalidToken { false }
            if (!formVerify) {
                flash.errorMessage = '验证码输入错误，请重新输入!'
                render view: 'index'
                return
            }
            def operator = CustomerOperator.find(
                    "from CustomerOperator o inner join fetch o.loginCertificate lc where lc.loginCertificate=?  and o.status='normal'",
                    [params.login_certificate]
            )
            if (!operator || !operator.verifyPayPassword(params.pay_password)) {
                // 用户名密码错误
                flash.errorMessage = '登录名称或者密码错误，请重新输入!'
                render view: 'index'
                return
            } else if (operator.status != 'normal') {
                // 用户被锁定或禁用
                flash.errorMessage = "操作员(${params.login_certificate})被锁定或禁用!"
                render view: 'index'
                return
            }
            def slave = operator.customer
            if (slave.status in ['disabled', 'deleted']) {
                // 客户已经被停用
                flash.errorMessage = "客户(${slave.name})已经被停用，请联系客户解决!"
                render view: 'index'
                return
            }
            def partner = Customer.get(session.partner.id)
//            def binding = RoyaltyBinding.findByPartnerAndCustomer(partner, slave)
//            if (binding) {
//                flash.errorMessage = "您(${slave.name})已经和${partner.name}绑定过了!"
//                render view: 'index'
//                return
//            }
            def binding = RoyaltyBinding.findWhere([partner: partner, customer: slave, bizType: '10', status: 'bind', nopassRefundFlag: 'T'])
            if (binding) {
                flash.errorMessage = "您(${slave.name})已经和${partner.name}绑定过了!"
                render view: 'index'
                return
            }
            binding = new RoyaltyBinding(
                    partner: partner,
                    customer: slave,
                    nopassRefundFlag: 'T',
                    outCustomerCode: session.bd_params.out_customer_code,
                    status: 'bind',
                    bizType: '10'
            )
            binding.save failOnError: true
            def resp_params = FormFunction.compressMap(session.bd_params)
            def return_url = resp_params.remove('return_url')
            resp_params.is_success = 'T'
            resp_params.customer_no = slave.customerNo
            resp_params.timestamp = StringUtil.getNumericDateTime()
//            resp_params.charset = resp_params.remove('_input_charset')
            resp_params.email = params.login_certificate
            resp_params.sign = FormFunction.createMD5Sign(resp_params, session.partner.apiKey, resp_params.charset)
            resp_params.sign_type = session.bd_params.sign_type
            [slave: slave, resp_params: resp_params, return_url: return_url]
        } catch (MAPIException e) {
            log.error e, e
            flash.putAll(errorCode: e.errorCode, errorMessage: '提交错误，请联系技术人员!')
            render view: '/errorPage'
            return
        } catch (e) {
            log.error e, e
            flash.putAll(errorCode: 'WEBERR', errorMessage: '系统忙请稍候再试!')
            render view: '/errorPage'
            return
        }
    }
}
