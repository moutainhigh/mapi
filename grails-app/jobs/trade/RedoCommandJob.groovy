package trade

import java.sql.Timestamp
import groovy.sql.Sql
import org.codehaus.groovy.grails.commons.ConfigurationHolder

/**
 * 处理没有后续逻辑的重做交易
 */
class RedoCommandJob {
    def dataSource_ismp
    def accountClientService
    def concurrent = false

    static triggers = {
        simple name: 'redoCommand', startDelay: 2000, repeatInterval: 60000
    }

    def execute() {
        if (ConfigurationHolder.config.job.redoCommand!='enable') return
        log.info 'redo command'
        def sql = new Sql(dataSource_ismp)
        def rows = sql.rows("""
            select command_no
              from trade_account_command_saf
             where redo_flag = 'T'
               and redo_count < 5
               and sync_flag = 'S'
               and sync_time < sysdate-2/24*60
               and sub_seqno = 0
          order by date_created """
//                [new Timestamp(System.currentTimeMillis()-60000)]
        )
        rows.each { row ->
            try {
                def commandNo = row.COMMAND_NO
                log.info "redo command no: $commandNo"
                AccountCommandPackage acPackage = null
                AccountCommandSaf.withTransaction {
                    acPackage = AccountCommandPackage.createRedoByCommandNo(commandNo)
                }
                if ( !acPackage ) return
                log.info "account command package: $acPackage.commandList"
                def resp = accountClientService.executeCommands(acPackage)
                AccountCommandSaf.withTransaction {
                    acPackage.update resp
                }
            } catch (e) {
                log.error e, e
            }
        }
        log.info 'redo command end'
    }
}
