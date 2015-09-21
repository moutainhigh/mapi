package mapi

import grails.util.GrailsUtil
import customer.Customer
import customer.CustomerService
import ebank.tools.FormFunction
import ebank.tools.StringUtil
import trade.TradeBase
import java.nio.charset.Charset
import ebank.lang.MAPIException
import org.codehaus.groovy.grails.commons.ConfigurationHolder

abstract class BaseController {
    def charsetKeyName = "_input_charset"
    def defaultCharset = Charset.forName("utf8")
//    def beforeInterceptor = [action:this.&check]

    // 必选参数
    def required_attr = []
    // 服务码
    def serviceCode = ''
    // 验证签名
    def verifySign = true
    // 验证外部订单号重复
    def verifyOutTradeNoRepeat = false

    def index = {
        try {
            log.debug 'before check v5'
            setConstraints()
            decodeUrlQuery()
            check()
            log.debug "before execute $params.controller"
            execute()
            log.debug "after execute $params.controller"
        } catch (MAPIException e) {
            writeResponse e.errorCode
            log.error e, e
        } catch (e) {
            writeResponse 'GENERAL_FAIL'
            log.error e, e
        }
    }

    protected setConstraints() {}

    protected execute() {}

    protected check() {
        switch (GrailsUtil.environment) {
            case 'development':
                break
            case 'test':
            case 'production':
                if (verifySign) required_attr += ['sign', 'sign_type']
                break
        }
        def isComplete = required_attr.every { attr ->
            switch (attr) {
                case String:
                    return params[attr]
                case List:
                    return attr.any {params[it]}
            }
        }
        if (!isComplete) {
            throw new MAPIException('ILLEGAL_PARAMETER')
        }
        def partner = Customer.findByCustomerNo(params.merchant_ID);
        if (!partner || partner.status != 'normal') {
            throw new MAPIException('ILLEGAL_PARTNER')
        }
        params._partner = partner
        if (GrailsUtil.environment != 'development' && verifySign
                && ConfigurationHolder.config.mapi.verifySign != 'false') {
            def req_params = [:]
            for (def attr: request.parameterNames) {
                req_params[attr] = params[attr]
            }
            log.info "verify sign data: $req_params"
            if (!FormFunction.verifyMD5Sign(req_params, partner.apiKey)) {
                throw new MAPIException('ILLEGAL_SIGN')
            }
        }

        if (serviceCode) {
            def customerService = CustomerService.find(
                    "from CustomerService where customerId=? and serviceCode=? and enable=true and isCurrent=true",
                    [partner.id, serviceCode])
            if (!customerService) {
                throw new MAPIException('SERVICE_NOT_SUPPORT')
            }
            params._customerService = customerService
        }
        if (verifyOutTradeNoRepeat && params.order_no) {
            def trade = TradeBase.findByPartnerIdAndOutTradeNo(partner.id, params.order_no)
            if (trade) {
                throw new MAPIException('OUT_TRADE_NO_REPEAT')
            }
        }
    }

    protected decodeUrlQuery() {
        def queryString = request.getQueryString()
        if (!queryString) return
        log.info "decode url query: $queryString"
        def charset = null
        try {
            if (params[charsetKeyName]){
                if(!(params[charsetKeyName] instanceof String)){
                    charset= Charset.forName(params[charsetKeyName][0])
                }else {
                    charset = Charset.forName(params[charsetKeyName])
                }
            }else{
                charset = defaultCharset
            }
        } catch (e) {
            log.error e, e
            throw new MAPIException('ILLEGAL_CHARSET', e)
        }
        log.info "decode url query. use charset: $charset ."
        //params.remove('_input_charset')
        //params._input_charset = charset.name()
        params.each {key, val ->
            key = URLDecoder.decode(key, charset.name())
            switch (val) {
                case String:
                    val = URLDecoder.decode(val, charset.name())
                    break
                case List:
                    val = val.collect { URLDecoder.decode(it, charset.name()) }
                    break
            }
            log.debug("params set attribute $key : $val")
            params[key] = val
        }
        params.remove("controller")
    }

    protected writeResponse(resultCode) {
        def isSuccess = (resultCode == 'SUCCESS') ? 'T' : 'F'
        def charset = params._input_charset
        if (!charset) charset = 'utf8'
        switch (params.return_type) {
            case 'json':
                render(contentType: "text/json", encoding: charset) {
                    ebank(
                            is_success: isSuccess,
                            result_code: resultCode,
                            timestamp: StringUtil.getFullDateTime()
                    )
                }
                break
            case 'xml':
            default:
                render(contentType: "text/xml", encoding: charset) {
                    ebank {
                        is_success(isSuccess)
                        result_code(resultCode)
                        timestamp(StringUtil.getFullDateTime())
                    }
                }
                break
        }
    }
}
