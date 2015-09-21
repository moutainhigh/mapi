package account

import groovyx.net.http.HTTPBuilder
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import ebank.tools.StringUtil
import trade.AccountCommandPackage
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.XML
import static groovyx.net.http.Method.GET
import static groovyx.net.http.ContentType.TEXT
import groovyx.net.http.ContentType

/**
 * 账务系统调用客户端
 * 配置：
 * 1. 安装grails插件rest: grails InstallPlugin rest
 * 2. 在Config.groovy中增加一行
 * account.serverUrl = "http://www.testpay.org:8111/Account/"
 * 错误码表：
 * 00 - 正常
 * ff - 系统未知错误
 * a1 - 交易重复  a2 - 参数错误
 * 01 - 账户不存在， 02 - 账户状态不正常， 03 - 账户余额不足， 04 - 账户不支持冻结
 */
class AccountClientService {

    static transactional = false

    def http = new HTTPBuilder(ConfigurationHolder.config.account.serverUrl)

    /**
     * 开户调用
     * @param accName 账户名,必须参数
     * @param direction 账户方向，debit 为借记账户，credit 为贷记账户,必须参数
     * @return{result: 'true or false',errorCode:'', errorMsg: '', accountNo: ''}* result: true为成功， false 为失败,
     * errorCode: 当result为false时，返回误号
     * errorMsg: 当result为false时，返回误原因,
     * accountNo: 当result为true时，返回账户账号
     * @throws Exception
     */
    def openAcc(accName, direction) throws Exception {
        http.request(POST, JSON) { req ->
            uri.path = 'rpc/openAcc'
            body = [accountName: accName, direction: direction]
            response.success = { resp, json ->
                return json
            }
            response.failure = { resp ->
                throw new Exception('request error')
            }
        }
    }

    /**
     * 交易指令集调用
     * @param commandNo 外部指令序号，不可重复，最好用uuid生成
     * @param commandList : 目前支持3种指令：
     *{commandType:'transfer', fromAccountNo:'', toAccountNo:'', amount:'', transferType:'', tradeNo:'', outTradeNo:'', subject:''},
     *{commandType:'freeze', fromAccountNo:'', amount:'', transferType:'', tradeNo:'', outTradeNo:'', subject:''},
     *{commandType:'unfreeze', fromAccountNo:'', amount:'', transferType:'', tradeNo:'', outTradeNo:'', subject:''},
     *  可用build方法创建
     * @return{result: 'true or false', transCode:'', transIds:['id1', 'id2',...] errorCode:'', errorMsg: ''}* result: true为成功， false 为失败,
     * transCode: 账务事务凭证号
     * transIds: 账户指令id集合
     * errorCode: 当result为false时，返回误号
     * errorMsg: 当result为false时，返回误原因,
     * @throws Exception
     */
    def executeCommands(commandNo, commandList) throws Exception {
        log.info "commandNo: $commandNo \n commandList: $commandList"
//        def now = new Date()
//        def i = 0
//        return [result:'true', transCode:StringUtil.getNumericDate(now), errorCode:'00', transIds:commandList.collect {now.time+(i++)}]
        http.request(POST, JSON) { req ->
            uri.path = 'rpc/batchCommand'
            body = [commandSeqno: commandNo,
                    commandLs: commandList
            ]
            response.success = { resp, json ->
                println "return ok json: $json"
                return json
            }
            response.failure = { resp ->
                println 'execute error'
                throw new Exception('request error')
            }
        }
    }

    /**
     * 查询帐户余额调用
     * @param accNo 账户账号,必须参数
     * @return{result: 'true or false', errorCode: '', errorMsg: '', accName: '', balance: '', freezBal:'', direc:'debit or credit', status:'norm or freeze or closed'}* result: true为成功， false 为失败,
     * errorCode: 当result为false时，返回误号
     * errorMsg: 当result为false时，返回误原因,
     * @throws Exception
     */
    def queryAcc(accNo) throws Exception {
        http.request(POST, JSON) { req ->
            uri.path = 'rpc/queryAcc'
            body = [accountNo: accNo]
            response.success = { resp, json ->
                return json
            }
            response.failure = { resp ->
                throw new Exception('request error')
            }
        }
    }

    def executeCommands(AccountCommandPackage acPackage) {
        executeCommands(acPackage.commandNo, acPackage.commandList.collect {it.toAccountCommandMap()})
    }
}
