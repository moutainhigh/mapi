package mapi

import customer.CustomerOperator
import customer.RoyaltyBinding
import ebank.lang.MAPIException
import customer.Customer
import ebank.tools.FormFunction
import ebank.tools.StringUtil
import customer.LoginCertificate

class DirectBindingController extends BaseController{

    /*def index = {

    }*/
     protected execute() {
         try {
            if (!params.merchant_ID) {
                 writeResponse("操作超时啦，请返回分销平台重新发起绑定")
                 return
            }
            log.info params
            def partner = Customer.findByCustomerNo(params.merchant_ID)
            def partnerOperator
            if(partner){
                 partnerOperator = CustomerOperator.findByCustomer(partner)
            }
            if(partner && partnerOperator){
                if(!params.seller_email.equals(partnerOperator.defaultEmail)){
                    writeResponse("商户的账户输入错误，请重新输入")
                    return
                }
            }
            if(!partner || !partnerOperator){
                 writeResponse("商户的账户输入错误，请重新输入")
                 return
            }
            def customerOperator = CustomerOperator.findByDefaultEmail(params.out_customer_email)
            def partnerLogin
            if(customerOperator){
                partnerLogin = LoginCertificate.findByCustomerOperator(customerOperator)
            }else{
                writeResponse("外部商户email输入错误，请重新输入")
                return
            }
            def operator
            if(partnerLogin){
                 operator = CustomerOperator.find(
                         "from CustomerOperator o inner join fetch o.loginCertificate lc where lc.loginCertificate=?  and o.status='normal'",
                         [partnerLogin.loginCertificate])
            }else{
                writeResponse("外部商户email输入错误，请重新输入")
                return
            }
            if (operator && operator.status != 'normal') {
                writeResponse("操作员(${partnerLogin.loginCertificate})被锁定或禁用!")
                return
            }
            def slave
            if(operator){
                 slave = operator.customer
            }else{
                writeResponse("外部商户email输入错误，请重新输入")
                return
            }
            if (slave && slave.status in ['disabled', 'deleted']) {
                writeResponse("客户(${slave.name})已经被停用，请联系客户解决!")
                return
            }
            if(!slave){
                writeResponse("外部商户email输入错误，请重新输入")
                return
            }
            def binding = RoyaltyBinding.findWhere([partner: partner, customer: slave, bizType: '10', status: 'bind', nopassRefundFlag: 'T'])
            if (binding) {
                writeResponse("您(${params.merchant_ID})已经和${params.out_customer_email}绑定过了!")
                return
            }
            binding = new RoyaltyBinding(
                    partner: partner,
                    customer: slave,
                    nopassRefundFlag: 'T',
                    outCustomerCode: params.out_customer_email,
                    status: 'bind',
                    bizType: '10'
            )
            binding.save failOnError: true
            Map<String,Object> map = new HashMap<String,Object>()
            map.put("_input_charset","gbk")
            map.put("partner",params.merchant_ID)
            map.put("seller_email",params.seller_email)
            map.put("out_customer_email",params.out_customer_email)
            map.put("sign_type","MD5")
            map.put("sign",params.sign)

            def resp_params = FormFunction.compressMap(map)
            resp_params.is_success = 'T'
            resp_params.customer_no = slave.customerNo
            resp_params.timestamp = StringUtil.getNumericDateTime()
            resp_params.email = map.get("seller_email")
            resp_params._input_charset = map.get("input_charset")
            resp_params.sign = params.sign
            resp_params.sign_type = map.get("sign_type")
            boolean flag = FormFunction.verifyMD5Sign(map,partner.apiKey)
            if(flag){
               writeResult("SUCCESS",resp_params)
            }else{
               writeResponse("FAILURE")
            }
            return
        } catch (MAPIException e) {
             log.error e, e
             writeResponse("提交错误，请联系技术人员!")
             return
        } catch (e) {
            log.error e, e
             writeResponse("系统忙请稍候再试")
             return
        }
     }

     protected writeResult(String resultCode,Map resp_params) {
        def isSuccess = (resultCode == 'SUCCESS') ? 'T' : 'F'
        def charset =  'utf8'
        render(contentType: "text/xml", encoding: charset) {
            result {
                 result_code(resultCode)
                 is_success(resp_params.is_success)
                 customer_no(resp_params.customer_no)
                 timestamp(resp_params.timestamp)
                 email(resp_params.email)
                 sign(resp_params.sign)
                 sign_type(resp_params.sign_type)
            }
        }
    }
}
