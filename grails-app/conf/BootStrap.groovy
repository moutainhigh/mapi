import grails.util.GrailsUtil
import customer.Customer
import customer.CustomerService
import customer.RoyaltyBinding
import trade.TradePayment
import ebank.tools.StringUtil
import groovy.sql.Sql
import mapi.AsyncNotify
import customer.CustomerOperator
import customer.LoginCertificate
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import com.burtbeckwith.grails.plugin.datasources.DatasourcesUtils

class BootStrap {
    def init = { servletContext ->
        log.info ConfigurationHolder.config
        switch ( GrailsUtil.environment ) {
            case 'development':
            case 'test':
                if (ConfigurationHolder.config.bootstrap.insertInitdata != 'true') return
                createSeq(name: 'seq_trade_no', max: 9999999)
                def now = new Date()
//                def cus01 = new Customer(
//                        name: '平台商户2-过渡', apiKey: '8a8aa55bbd43b440dde4ed57e9c81ae6',
//                        customerNo: '3001', accountNo: '4560000000000034', type: 'A', status: 'normal'
//                )
//                // 分润收款账号1^80^说明1|分润收款账号1^分润收款账号2^5^说明2|分润收款账号3^20^说明3
//                // 3^80^说明1|3^4^10^说明2|2^20^说明3
//                def cus02 = new Customer(name: '平台商户2', type: 'C', customerNo: '5001', accountNo: '4560000000000046', status: 'normal')
//                def cus03 = new Customer(name: '分销商3', type: 'C', customerNo: '5003', accountNo: '4560000000000050', status: 'normal')
//                def cus04 = new Customer(name: '分销商4', type: 'C', customerNo: '5004', accountNo: '4560000000000058', status: 'normal')
//                def cus05 = new Customer(name: '普商户5', apiKey: '8a8aa55bbd43b440dde4ed57e9c81ae6',
//                        type: 'C', customerNo: '5005', accountNo: '4560000000000062', status: 'normal')
//                def cus06 = new Customer(name: '个人6', type: 'C', customerNo: '6001', accountNo: '4560000000000070', status: 'normal')
//                [cus01, cus02, cus03, cus04, cus05, cus06].each { cus ->
//                    cus.save flush: true, failOnError: true
//                    def mail = "${cus.customerNo}@mail"
//                    def oper = new CustomerOperator(customer: cus, name: "op$cus.customerNo", defaultEmail: mail,
//                            status: 'normal', roleSet: 'finance', loginPassword: '1')
//                    oper.save flush: true, failOnError: true
//                    oper.loginPassword = "${oper.id}111".encodeAsSHA1()
//                    oper.payPassword   = "${oper.id}p222".encodeAsSHA1()
//                    oper.save failOnError: true
//                    new LoginCertificate(customerOperator: oper, certificateType: 'email', loginCertificate: mail).save failOnError: true
//                }
                def cus01 = Customer.findByCustomerNo('3001')
                def cus02 = Customer.findByCustomerNo('5001')
                def cus03 = Customer.findByCustomerNo('5003')
                def cus04 = Customer.findByCustomerNo('5004')
                def cus05 = Customer.findByCustomerNo('5005')
                def cus06 = Customer.findByCustomerNo('6001')
//                // CustomerService
//                [
//                        new CustomerService(customerId: cus01.id, contractNo: 'c01', serviceCode: 'online', feeParams: '10',
//                                enable: true, isCurrent: true, startTime: now, endTime: now+300),
//                        new CustomerService(customerId: cus01.id, contractNo: 'c02', serviceCode: 'royalty',
//                                serviceParams: '{"payfee_customer_no":"5001"}', enable: true, isCurrent: true,
//                                startTime: now, endTime: now+300)
//                ].each { service ->
//                    service.save flush: true, failOnError: true
//                }
//                // RoyaltyBinding
//                [
//                        new RoyaltyBinding(partner: cus01, customer: cus02, nopassRefundFlag: 'T', status: 'bind'),
//                        new RoyaltyBinding(partner: cus01, customer: cus03, nopassRefundFlag: 'T', status: 'bind'),
//                        new RoyaltyBinding(partner: cus01, customer: cus04, nopassRefundFlag: 'T', status: 'bind')
//                ].each { bind ->
//                    bind.save flush: true, failOnError: true
//                }
                // Trade
                [
                        new TradePayment(partnerId:cus01.id, tradeNo:'tn01', outTradeNo:'otn01', tradeType:'payment',
                                payerId:cus06.id, payerName: cus06.name, payerAccountNo: cus06.accountNo,
                                payeeId:cus01.id, payeeName: cus01.name, payeeAccountNo: cus01.accountNo,
                                status: 'completed', royaltyType:'10', royaltyStatus:'starting',
                                tradeDate: StringUtil.getNumericDate() as int,
                                amount:10000, feeAmount: 100),
                        new TradePayment(partnerId:cus01.id, tradeNo:'tn02', outTradeNo:'otn02', tradeType:'payment',
                                payerId:cus06.id, payerName: cus06.name, payerAccountNo: cus06.accountNo,
                                payeeId:cus01.id, payeeName: cus01.name, payeeAccountNo: cus01.accountNo,
                                status: 'completed', tradeDate: StringUtil.getNumericDate() as int,
                                amount:20000, feeAmount: 200),
                        new TradePayment(partnerId:cus01.id, tradeNo:'tn03', outTradeNo:'otn03', tradeType:'payment',
                                payerId:cus06.id, payerName: cus06.name, payerAccountNo: cus06.accountNo,
                                payeeId:cus01.id, payeeName: cus01.name, payeeAccountNo: cus01.accountNo,
                                status: 'completed', royaltyType:'10', royaltyStatus:'starting',
                                tradeDate: StringUtil.getNumericDate() as int,
                                amount:30000, feeAmount: 300),
                        new TradePayment(partnerId:cus05.id, tradeNo:'tn04', outTradeNo:'otn04', tradeType:'payment',
                                payerId:cus06.id, payerName: cus06.name, payerAccountNo: cus06.accountNo,
                                payeeId:cus05.id, payeeName: cus05.name, payeeAccountNo: cus05.accountNo,
                                status: 'completed', tradeDate: StringUtil.getNumericDate() as int,
                                amount:50000, feeAmount: 500)
                ].each { trade ->
                    trade.save flush: true, failOnError: true
                }
                // AsyncNotify
                [
                        new AsyncNotify(
                                customer:cus01, signType:'md5', notifyMethod:'http', outputCharset:'utf8',
                                notifyAddress:'http://localhost:8080/mapi/debug',
                                notifyContents:'{"customer_no":"5001", "amount":"340.00"}',
                                notifyId:'111', status:'processing', nextAttemptTime: now, timeExpired:now+1
                        ),
                        new AsyncNotify(
                                customer:cus01, notifyMethod: 'http',
                                notifyAddress:'http://localhost:8080/mapi/debug',
                                notifyContents:'{"customer_no":"5001", "amount":"340.00","note":"中文"}',
                                notifyId:'111', status:'processing', nextAttemptTime: now, timeExpired:now+1
                        ),
                        new AsyncNotify(
                                customer:cus01, signType:'md5', notifyMethod: 'http', outputCharset:'utf8',
                                notifyAddress:'http://localhost:8080/mapi/debug',
                                notifyContents:'{"transfer_type":"payment","amount":"1.20","note":"中文"}',
                                notifyId:'222', status:'processing', nextAttemptTime: now, timeExpired:now+1
                        )
                ].each { notify ->
                    notify.save flush: true, failOnError: true
                }
                break
            case 'production': break
        }
    }
    def destroy = {
    }

    def createSeq( attrs ) {
        def ds = DatasourcesUtils.getDataSource('ismp')
        log.info "get datasource: $ds"
        def sql = new Sql(ds)
        def name = attrs.name
        if (!name) {
            println 'error createSeq name is null'
            return
        }
        try {
            sql.firstRow("select ${name}.nextval seq from dual" as String)
        } catch (e) {
            e.printStackTrace()
            try {
                println "not found seq $name"
                def min = (attrs.min) ? attrs.min : 1
                def max = (attrs.max) ? attrs.max : 999999999999999999999999999
                def start = (attrs.start) ? attrs.start : min
                sql.execute("""
                        create sequence $name
                        minvalue $min
                        maxvalue $max
                        start with $start
                        increment by 1
                        cache 20
                        """ as String)
                println "create sequence $name"
            } catch (e1) {
                e1.printStackTrace()
            }
        }
    }
}
