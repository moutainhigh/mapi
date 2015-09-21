package mapi

import ebank.tools.StringUtil
import groovy.sql.Sql

class NoGeneratorService {

    static transactional = true
    def dataSource_ismp

    def tradeTypeMap = [
            payment     : '10',
            charge      : '11',
            transfer    : '12',
            refund      : '13',
            withdrawn   : '14',
            frozen      : '15',
            unfrozen    : '16',
            royalty     : '20',
            royalty_rfd : '21'
    ]

    def createTradeNo(tradeType, time = new Date()) {
        def prefix = tradeTypeMap[tradeType]
        def middle = StringUtil.getNumericDate(time)[2..-1] // yymmdd
        def sql = new Sql(dataSource_ismp)
        def seq = sql.firstRow("select seq_trade_no.nextval seq from dual").seq as String
        prefix + middle + seq.padLeft(7, '0')
    }

    def createAccountCommandNo() {
        UUID.randomUUID().toString()
    }

    def createNotifyId() {
        UUID.randomUUID().toString().replaceAll('-', '')
    }
}
