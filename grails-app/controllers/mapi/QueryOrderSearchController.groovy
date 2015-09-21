package mapi

import ebank.tools.StringUtil

class QueryOrderSearchController extends BaseController {
    protected setConstraints() {
        required_attr = ['partner', 'fromdate', 'todate', 'page']
    }

    protected execute() {
        log.info 'in query payment'
        //def partner = params._partner
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
        log.info "#params#${params}"
        def query = {
            if (params.fromdate != null && params.fromdate != '') {
                ge('dateCreated', Date.parse('yyyyMMdd', params.fromdate))
            }
            if (params.todate != null && params.todate != '') {
                le('dateCreated', Date.parse('yyyyMMdd', params.todate) + 1)
            } else {
                le('dateCreated', Date.parse('yyyyMMdd', todate) + 1)
            }
            eq('partnerCustomerNo', params.merchant_ID)
            eq('status', status == 'completed' ? '3' : '4')
        }

        //def payment = TradePayment.findAll("from TradePayment where partnerId=? and dateCreated>=? and dateCreated<=? and status=? order by dateCreated desc",[partner.id, Date.parse('yyyyMMdd', params.fromdate), todate, status], [max: perCount, offset: page])
        def payment = gateway.GwOrder.findAll("from GwOrder where partnerCustomerNo=? and dateCreated>=? and dateCreated<=? and status=? order by dateCreated desc",[params.merchant_ID, Date.parse('yyyyMMdd', params.fromdate), todate, status == 'completed' ? '3' : '4'], [max: perCount, offset: page])
        log.info "#payment#${payment}"
        def count = gateway.GwOrder.createCriteria().count(query)
        def pages = Math.ceil(count / perCount).intValue()

        log.info "#count#${count}#pages#${pages}"

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

    private getStatus(String s){
        String rst = ''
        switch (s){
            case '0':
                rst = 'wait'
                break
            case '1':

            case '2':

            case '3':
                rst = 'completed'
                break
            case '4':
                rst = 'close'
                break
            case '5':
                rst = 'failed'
                break
        }

        rst
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
                                    trade_no = _trade.id
                                    out_trade_no = _trade.outTradeNo
                                    trade_type = 'payment'
                                    amount = StringUtil.getAmountFromNum(_trade.amount)
                                    fee_amount = StringUtil.getAmountFromNum(0L)
                                    subject = _trade.subject ? _trade.subject : ''
                                    trade_date = _trade.orderdate
                                    created_time = StringUtil.getFullDateTime(_trade.dateCreated)
                                    status = getStatus(_trade.status)
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
                                    trade_no(_trade.id)
                                    out_trade_no(_trade.outTradeNo)
                                    trade_type('payment')
                                    amount(StringUtil.getAmountFromNum(_trade.amount))
                                    fee_amount(StringUtil.getAmountFromNum(0L))
                                    subject(_trade.subject ? _trade.subject : '')
                                    trade_date(_trade.orderdate)
                                    created_time(StringUtil.getFullDateTime(_trade.dateCreated))
                                    status(getStatus(_trade.status))
                                }
                            }
                        }
                    }
                }
                break
        }
    }
}
