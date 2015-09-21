package mapi

import customer.CustomerService
import net.sf.json.JSONObject
import customer.Customer
import groovyx.net.http.HTTPBuilder
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import groovy.sql.Sql
import java.sql.Timestamp
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.ContentType.JSON
import ebank.tools.FormFunction
import static ebank.tools.FormFunction.createMD5Sign

class SyncRoyaltyJob {
    def http = new HTTPBuilder(ConfigurationHolder.config.royalty.serverUrl)
    def dataSource_ismp
    def concurrent = false
    static triggers = {
        simple name: 'syncRoyalty', startDelay: 1000, repeatInterval: 10000
    }

    def execute() {
        if (ConfigurationHolder.config.job.syncRoyalty != 'enable') return
        log.info 'in sync royalty'
        def sql = new Sql(dataSource_ismp)
        def rows = sql.rows("""
            select b.out_trade_no,
                b.partner_id,
                a.royalty_params,
                d.royalty_parameters,
                d.charsets
              from trade_payment a,trade_base b,gworders d
             where a.id=b.id
             and b.trade_no=d.id
               and a.royalty_type = '10'
               and a.royalty_status='starting'
               and b.status='completed'
               and b.date_created < ?
          order by b.date_created """,
                [new Timestamp(System.currentTimeMillis() - 12000)]
        )
        rows.each { row ->
            try {
                def cmCustomer
                def args
//                def customerService = CustomerService.find(
//                        "from CustomerService where customerId=? and serviceCode=? and enable=true and isCurrent=true",
//                        [row.PARTNER_ID as Long, 'royalty'])
//                def serviceParams = JSONObject.fromObject(customerService.serviceParams)
//                //手续费承担商户号  例如：“100000000000033”
//                def royaltymode = serviceParams.royaltymode
                if (row.ROYALTY_PARAMETERS != 'null' && row.ROYALTY_PARAMETERS != '' && row.ROYALTY_PARAMETERS != null) {
                    cmCustomer = Customer.findByIdAndStatus(row.PARTNER_ID as Integer, 'normal')
                    if (cmCustomer) {
                        def args1= [partner: cmCustomer.customerNo, _input_charset: row.CHARSETS, sign_type: 'MD5', return_type: 'xml', royalty_type: '10', out_trade_no: row.OUT_TRADE_NO, royalty_parameters: row.ROYALTY_PARAMS]
                        def sign=createMD5Sign(args1, cmCustomer.apiKey, row.CHARSETS)
                        args = [partner: cmCustomer.customerNo, sign: sign, _input_charset: row.CHARSETS, sign_type: 'MD5', return_type: 'xml', royalty_type: '10', out_trade_no: row.OUT_TRADE_NO, royalty_parameters: row.ROYALTY_PARAMS]
                        openRoyalty(args)
                    }
                }
            } catch (e) {
                log.error e, e
            }
        }
    }

    def openRoyalty(args) throws Exception {
        http.request(GET, TEXT) {
            uri.path = 'service/royalty'
            uri.query = args
            response.success = { resp, text ->
                return text.text
            }
            response.failure = { resp ->
                println 'execute error' + resp
                throw new Exception('request error')
            }
        }
    }
}
