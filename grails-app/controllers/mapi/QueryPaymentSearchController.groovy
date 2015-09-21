package mapi

import ebank.tools.StringUtil
import trade.TradeBase
import trade.TradePayment

class QueryPaymentSearchController extends BaseController {
    protected setConstraints() {
        required_attr = ['partner', 'fromdate', 'todate', 'page']
    }

    protected execute() {
        log.info 'in query payment'
        def partner = params._partner
        params.max = Math.min(params.max ? params.int('max') : 0, 100)
        def page = 0
        def perCount = 2000 //每页显示条数
        if (params.page) {
            page = (Integer.parseInt(params.page) - 1) * perCount
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd")
        def todate = sdf.format(new Date()) + 1
        if (params.todate != null && params.todate != '') {
            todate = Date.parse('yyyyMMdd', params.todate) + 1
        }
        def status = 'completed'
        if (params.status) {
            status = params.status
        }
        def query = {
            if (params.fromdate != null && params.fromdate != '') {
                ge('dateCreated', Date.parse('yyyyMMdd', params.fromdate))
            }
            if (params.todate != null && params.todate != '') {
                le('dateCreated', Date.parse('yyyyMMdd', params.todate) + 1)
            } else {
                le('dateCreated', Date.parse('yyyyMMdd', todate) + 1)
            }
            eq('partnerId', partner.id)
            eq('status', status)

        }
        def payment = TradePayment.findAll("from TradePayment where partnerId=? and dateCreated>=? and dateCreated<=? and status=? order by dateCreated desc",
                [partner.id, Date.parse('yyyyMMdd', params.fromdate), todate, status], [max: perCount, offset: page])
        def count = TradePayment.createCriteria().count(query)
        def pages = Math.ceil(count / perCount).intValue()
        def money = 0.00

        if (payment != null) {
            payment.each {
                money += it.amount
            }
            def mon= (money/100).toString()
            writeTrade((payment ? 'SUCCESS' : 'TRADE_NOT_EXIST'), payment, count, pages, params.merchant_ID, mon)
            log.info 'query trade end'
        }
    }


    protected writeTrade(String resultCode, List payments, int count, int pages, String no, String money) {
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
                        totalcount = count
                        totalpage = pages
                        totalamount = money
                        partnerNo = no
                        trades = array {
                            for (_trade in payments) {
                                trade = {
                                    trade_no = _trade.tradeNo
                                    out_trade_no = _trade.outTradeNo
                                    trade_type = _trade.tradeType
                                    amount = StringUtil.getAmountFromNum(_trade.amount)
                                    fee_amount = StringUtil.getAmountFromNum(_trade.feeAmount)
                                    subject = _trade.subject ? _trade.subject : ''
                                    trade_date = _trade.tradeDate
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
                        totalcount(count)
                        totalpage(pages)
                        totalamount(money)
                        partnerNo(no)
                        trades {
                            for (_trade in payments) {
                                trade {
                                    trade_no(_trade.tradeNo)
                                    out_trade_no(_trade.outTradeNo)
                                    trade_type(_trade.tradeType)
                                    amount(StringUtil.getAmountFromNum(_trade.amount))
                                    fee_amount(StringUtil.getAmountFromNum(_trade.feeAmount))
                                    subject(_trade.subject ? _trade.subject : '')
                                    trade_date(_trade.tradeDate)
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
