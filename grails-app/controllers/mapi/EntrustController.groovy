package mapi

import customer.Customer
import customer.CustomerOperator
import customer.RoyaltyBinding
import ebank.lang.MAPIException
import ebank.tools.FormFunction
import ebank.tools.StringUtil
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import groovyx.net.http.ContentType
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.TEXT
import groovyx.net.http.URIBuilder
import trade.TradePayment
import gateway.Gwpayments
import gateway.GwOrder
import java.util.regex.Pattern
import java.util.regex.Matcher
import customer.CustomerService
//import grails.converters.JSON


class EntrustController extends BaseController {
    protected setConstraints() {
        required_attr = ['partner', 'out_trade_no', 'amount', 'buyer_email', 'seller_email']
        serviceCode = 'selfSign'
        verifyOutTradeNoRepeat = true
    }

    def httpInvokeClientService
    def http = new HTTPBuilder(ConfigurationHolder.config.cashier.serverUrl)

    protected execute() {
        log.info 'in entrust'
        def partner = params._partner
        def outOperator = CustomerOperator.find(
                "from CustomerOperator o inner join fetch o.loginCertificate lc where lc.loginCertificate=? and o.status='normal'",
                [params.buyer_email]
        )
        def inOperator = CustomerOperator.find(
                "from CustomerOperator o inner join fetch o.loginCertificate lc where lc.loginCertificate=? and o.status='normal'",
                [params.seller_email]
        )
        if (!outOperator) {
            // 账号错误
            throw new MAPIException('转出账号不存在！')
        } else if (outOperator.status != 'normal') {
            // 用户被锁定或禁用
            throw new MAPIException("操作员(${params.buyer_email})被锁定或禁用!")
        }
        if (!inOperator) {
            // 账号错误
            throw new MAPIException('转入账号不存在！')
        } else if (inOperator.status != 'normal') {
            // 用户被锁定或禁用
            throw new MAPIException("操作员(${params.seller_email})被锁定或禁用!")
        }
        if (!(params.amount ==~ /^(\d{0,8}+)(\.\d{1,2})?$/)) {
            throw new MAPIException("转出金额书写错误!")
        }
        def tradeNo = /^[0-9a-zA-Z_-]*$/
        Pattern patternNo = Pattern.compile(tradeNo);
        Matcher matcherNo = patternNo.matcher(params.order_no);
        if (!matcherNo.find()) {
            throw new MAPIException("订单号只允许包含数字、字母、下划线!")
        }
        if (Double.parseDouble(params.amount) < 0.01) {
            throw new MAPIException("转出金额应大于零!")
        }
        if (params.amount.toString().indexOf('.') != -1) {
            if (params.amount.toString().substring(params.amount.toString().indexOf('.') + 1).length() > 2) {

                throw new MAPIException("转出金额小数位应小于两位!")
            }
        }
        def outSlave = outOperator.customer
        def inSlave = inOperator.customer
        if (outSlave == inSlave) {
            throw new MAPIException("转入、转出为同一个账户!")
        }
        if (outSlave.status in ['disabled', 'deleted']) {
            // 客户已经被停用
            throw new MAPIException("客户(${outSlave.name})已经被停用，请联系客户解决!")
        }
        if (inSlave.status in ['disabled', 'deleted']) {
            // 客户已经被停用
            throw new MAPIException("客户(${inSlave.name})已经被停用，请联系客户解决!")
        }
        def par = Customer.get(partner.id)
        def outBinding = RoyaltyBinding.findWhere([partner: par, customer: outSlave, bizType: '11', outCustomerCode: params.buyer_email, status: 'sign', nopassRefundFlag: 'T'])
//        def inBinding = RoyaltyBinding.findWhere([partner: par, customer: inSlave, bizType: '11', status: 'sign', nopassRefundFlag: 'T'])
        if (!outBinding) {
            throw new MAPIException("您(${outSlave.name})尚未和${par.name}绑定!")
        }
        if (outBinding.amount > 0 && outBinding.amount < Double.parseDouble(params.amount)) {
            throw new MAPIException("单笔支付金额不能多于${outBinding.amount}")
        }
//        if (!inBinding) {
//            throw new MAPIException("您(${inSlave.name})尚未和${par.name}绑定!")
//        }
        //查询转入账户是否支持在线支付服务
        def onlineService = CustomerService.find(
                "from CustomerService where customerId=? and serviceCode=? and enable=true and isCurrent=true",
                [inSlave.id, 'online'])
        log.info "customer(online) service: $onlineService"
        if (!onlineService) {
            throw new MAPIException('SERVICE_NOT_SUPPORT')
        }

        params.remove('return_type')
        params.remove('_partner')
        params.remove('_customerService')
        params.remove('controller')
        params.gmt_out_order_create = params.gmt_out_order_create.toString().replaceAll('-', '')
        params.gmt_out_order_create = params.gmt_out_order_create.toString().replaceAll(' ', '')
        params.gmt_out_order_create = params.gmt_out_order_create.toString().replaceAll(':', '')
        params._input_charset='utf-8'
//        params.subject = URLEncoder.encode(params.subject, params._input_charset)
//        if (params.royalty_parameters != '' && params.royalty_parameters != null) {
//            params.royalty_parameters = URLEncoder.encode(params.royalty_parameters, params._input_charset)
//        }
        def password = outOperator.payPassword
        if (par && String.valueOf(params.sign_type).toLowerCase() == 'md5') {
            params.sign = FormFunction.createMD5Sign(
                    params, par.apiKey, params._input_charset)
        }
        def reps = BuildPaymentSearchForm(params)

        if (!reps) {
            writeResponse 'SYSTEM_BUSY'
        }
        else {
            def json = grails.converters.JSON.parse(reps)
            if (json.resmsg == 'failure') {
                if (json.respcode.toString().substring(0, 1) == '4') {
                    writeResponse '金额不足'
                } else {
                    writeResponse 'PAYMENT_FAILURE_' + json.respmsg
                }
            } else if (json.resmsg == 'success') {
                def orderId = json.tradeid
                def rps0 = httpInvokeClientService.WrapDirectPayByOrderid(orderId.toString(), password)
                if (!rps0) {
                    writeResponse 'SYSTEM_BUSY'
                } else {
                    def rps = grails.converters.JSON.parse(rps0)
                    if (rps?.result == '00') {
                        def payments = Gwpayments.findByIdAndPaysts(rps.paymentid as Long, '1')
                        if (payments) {
                            def payment = GwOrder.findById(payments.paynum)
                            writeTrade((payment ? 'SUCCESS' : 'TRADE_NOT_EXIST'), payment as List, params.sign, params.sign_type, params._input_charset, params.amount)
                        } else {
                            writeResponse 'TRADE_NOT_EXIST'
                        }
                    } else {
                        if (rps.errorcode.toString().substring(0, 1) == '4') {
                            writeResponse '账户余额不足！'
                        } else if (rps.errorcode.toString() == '530012') {
                            writeResponse '买家状态异常！'
                        } else if (rps.errorcode.toString() == '530011') {
                            writeResponse '会话时间过长！'
                        } else if (rps.errorcode.toString() == '530010') {
                            writeResponse '账户查询失败！'
                        } else if (rps.errorcode.toString() == '530007') {
                            writeResponse '客户服务状态异常！'
                        } else if (rps.errorcode.toString() == '530003') {
                            writeResponse '订单状态已改变,不能支付！'
                        } else if (rps.errorcode.toString() == '530002') {
                            writeResponse '交易处理失败！'
                        } else if (rps.errorcode.toString() == '530001') {
                            writeResponse '信息错误,买家未找到！'
                        } else if (rps.errorcode.toString() == '520001') {
                            writeResponse '信息错误,卖家未找到！'
                        } else if (rps.errorcode.toString() == '520002') {
                            writeResponse '卖家信息不能空！'
                        } else if (rps.errorcode.toString() == '520003') {
                            writeResponse '买家和卖家信息为同一用户！'
                        } else if (rps.errorcode.toString() == '500903') {
                            writeResponse '订单号过长！'
                        } else if (rps.errorcode.toString() == '500712') {
                            writeResponse '订单已存在！'
                        } else if (rps.errorcode.toString() == '500706') {
                            writeResponse '状态更新失败！'
                        } else if (rps.errorcode.toString() == '500705') {
                            writeResponse '订单支付信息未找到！'
                        } else if (rps.errorcode.toString() == '500704') {
                            writeResponse '商户状态错误！'
                        } else if (rps.errorcode.toString() == '500703') {
                            writeResponse '商户找不到！'
                        } else if (rps.errorcode.toString() == '500702') {
                            writeResponse '服务未提供！'
                        } else if (rps.errorcode.toString() == '500701') {
                            writeResponse '订单不存在！'
                        } else if (rps.errorcode.toString().substring(0, 1) == '1') {
                            writeResponse '系统错误，请联系技术人员！'
                        } else {
                            writeResponse rps.errorcode
                        }
                    }
                }
            }
        }

        log.info 'entrust end'
    }

    def BuildPaymentSearchForm(params) throws Exception {
        http.request(GET, TEXT) {
            uri.path = '/ProtocalPay.do'
            uri.query = params
            response.success = { resp, text ->
                return text.text
            }
            response.failure = { resp ->
                println 'execute error' + resp
                throw new Exception('request error')
            }
        }
    }

    protected writeTrade(String resultCode, List payments, String no, String type, String chartSet, String money) {
        def isSuccess = (resultCode == 'SUCCESS') ? 'T' : 'F'
        def charset = params._input_charset
        if (!charset) charset = 'utf-8'
        render(contentType: "text/xml", encoding: charset) {
            ebank {
                is_success(isSuccess)
                request {
                    for (_trade in payments) {
                        amount(money)
                        charge_type(_trade.body)
                        gmt_out_order_create(_trade.orderdate)
                        notify_url(_trade.notifyUrl)
                        out_trade_no(_trade.outTradeNo)
                        partner(_trade.partnerCustomerNo)
                        royalty_parameters(_trade.royaltyParams)
                        royalty_type(_trade.royaltyType)
                        service(_trade.service)
                        subject(_trade.subject)
                        input_charset(chartSet)
                        buyer_name(_trade.buyerCode)
                        seller_name(_trade.sellerCode)
                        type_code('11')
                        payment_type('1')
                    }
                }
                response {
                    for (_trade in payments) {
                        out_trade_no(_trade.outTradeNo)
                        subject(_trade.subject)
                        trade_no(_trade.id)
                        pay_date(_trade.dateCreated)
                        status('TRADE_SUCCESS')
                    }
                }
                sign(no)
                sign_type(type)
            }
        }
    }

}
