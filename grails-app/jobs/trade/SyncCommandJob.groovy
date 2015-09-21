package trade

import groovy.sql.Sql
import java.sql.Timestamp
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import boss.InnerAccount

class SyncCommandJob {
    def dataSource_ismp
    def accountClientService
    def concurrent = false

    static triggers = {
        simple name: 'syncCommand', startDelay: 2000, repeatInterval: 10000
    }

    def execute () {
        if (ConfigurationHolder.config.job.syncCommand!='enable') return
        log.info 'in sync command'
        def sql = new Sql(dataSource_ismp)
        def rows = sql.rows("""
            select command_no
              from trade_account_command_saf
             where sync_flag = 'F'
               and sync_time < ?
               and sub_seqno = 0
          order by date_created """,
                [new Timestamp(System.currentTimeMillis()-12000)]
        )
        rows.each { row ->
            try {
                def commandNo = row.COMMAND_NO
                log.info "processing $commandNo"
                def acPackage = AccountCommandPackage.findByCommandNo(commandNo)
                if ( !acPackage ) return
                log.info "account command package: $acPackage.commandList"
                def now = new Date()
                def trades = []
                AccountCommandSaf.withTransaction {
                    acPackage.commandList.each{ command ->
                        command.syncTime = now
                        command.save()
                        if (trades.any{ it?.id == command.tradeId }) return
                        trades << TradeBase.get( command.tradeId )
                    }
                }
                def resp = accountClientService.executeCommands(acPackage)
                switch (trades[0]?.tradeType) {
                    case 'transfer':   // 转帐
                    case 'withdrawn':  // 提现
                    case 'frozen':     // 冻结
                        onDefaultResp(trades, acPackage, resp)
                        break
                    case 'refund':        // 退款
                    case 'royalty_rfd':  // 退分润
                        onRefundResp(trades, acPackage, resp)
                        break
                    case 'unfrozen':  // 解冻结
                        onUnfrozenResp(trades, acPackage, resp)
                        break
                    case 'payment':  // 支付
                    case 'charge':   // 充值
                    case 'royalty':  // 分润
                        TradeBase.withTransaction { acPackage.update resp }
                        break
                    default:
                        break
                }
            } catch (e) {
                log.error e, e
            }
        }
        log.info 'sync command end'
    }

    def onDefaultResp(trades, acPackage, resp, okStatus='completed') {
        TradeBase.withTransaction {
            trades.each { trade ->
                trade.status = (resp.result=='true') ? okStatus : 'closed'
                trade.save failOnError: true
            }
            acPackage.update resp
        }
    }

    def onRefundResp(trades, acPackage, resp) {
        if ( !resp ) return
        TradeBase.withTransaction {
            if ( resp.result == 'true' ) {
                trades.each { TradeBase trade ->
                    if (trade.tradeType == 'refund') {
                        def payment = TradePayment.get(trade.originalId)
                        payment.refundAmount += trade.amount
                        if (payment.amount == payment.refundAmount) {
                            payment.status = 'closed'
                        }
                        payment.save failOnError: true
                        // isGuestPayment
                        if (trade.payeeAccountNo == InnerAccount.getMiddleAccountNo()) {
                            trade.status = 'processing'
                        } else {
                            trade.status = 'completed'
                        }
                    } else {
                        trade.status = 'completed'
                    }
                    trade.save failOnError: true
                }
            } else {
                trades.each { TradeBase trade ->
                    trade.status = 'closed'
                    trade.save failOnError: true
                }
            }
            acPackage.update resp
        }
    }

    def onUnfrozenResp(trades, acPackage, resp) {
        if ( !resp ) return
        TradeBase.withTransaction {
            if ( resp.result == 'true' ) {
                trades.each { TradeBase trade ->
                    def frozen = TradeFrozen.get(trade.originalId)
                    frozen.unfrozenAmount += trade.amount
                    if (frozen.amount == frozen.unfrozenAmount) {
                        frozen.status = 'closed'
                    }
                    frozen.save failOnError: true
                    trade.status = 'completed'
                    trade.save failOnError: true
                }
            } else {
                trades.each { TradeBase trade ->
                    trade.status = 'closed'
                    trade.save failOnError: true
                }
            }
            acPackage.update resp
        }
    }
}
