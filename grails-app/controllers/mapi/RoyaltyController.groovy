package mapi

import net.sf.json.JSONObject
import org.codehaus.groovy.grails.commons.ConfigurationHolder

import trade.TradePayment
import customer.Customer
import customer.RoyaltyBinding
import trade.TradeBase
import ebank.tools.StringUtil
import trade.AccountCommandPackage
import ebank.lang.MAPIException
import boss.InnerAccount
import customer.CustomerService

class RoyaltyController extends BaseController {
    def accountClientService
    def noGeneratorService

    protected setConstraints() {
        required_attr = ['partner', 'out_trade_no', 'royalty_type', 'royalty_parameters']
        serviceCode = 'royalty'
    }

    protected execute() {
        log.debug 'in royalty'
        def partner = params._partner as Customer
        def royaltyService = params._customerService as CustomerService
        def money = 0
        // 用外部订单号查找分润交易，如果找到就返回上次的执行结果，如果没找到就继续执行
        def payment = TradePayment.find(
                "from TradePayment where partnerId=? and outTradeNo=? and status='completed'",
                [partner.id, params.order_no]
        )
        if (!payment) {
            throw new MAPIException('TRADE_NOT_EXIST')
        } else if (payment.royaltyStatus == 'completed') {
            throw new MAPIException('ROYALTY_COMPLETED')
        }
//        else if (payment.royaltyStatus != 'starting') {
//           writeResponse 'SUCCESS'
//            return
//        }
        else if (payment.royaltyType != '10') {
            throw new MAPIException('TRADE_NOT_ROYALTY')
        }
        //如果做过分润，查询订单余额，不为零，可以继续分润。
        //根据订单号查询分润交易
        def tradeBase = TradeBase.findAllByOutTradeNoAndTradeType(params.order_no, 'royalty')
        //得到所有分润过的交易金额
        if (tradeBase.size() > 0) {
            tradeBase.each {
                money += it.amount
            }
        }
        //剩余分润金额
        def amo = payment.amount - money
        // 解析参数 <-- s1
        def (royaltyItems, customers) = ParameterParser.parseRoyaltyParams(
                partner.customerNo, params.royalty_parameters, payment.amount);
        log.info "royalty info: \n params: ${params.royalty_parameters} \n royaltyItems: $royaltyItems \n customers: $customers";
        //查询分润余额是否够
        def amount = 0
        def toCustomerNo = ''
        //得到当前的分润金额
        if (royaltyItems.size() > 0) {
            royaltyItems.each {
                amount += it.amount
                toCustomerNo = it.toCustomerNo + ',' + toCustomerNo
            }
        }
        if (amo - amount < 0) {
            throw new MAPIException('AMOUNT_NOT_ENOUGH')
        }
        //同一笔分润中不能有相同的分润商户
        def list = new ArrayList()
        list = toCustomerNo.split(',')
        for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j < i; j++) {
                if (list[i] == list[j] && i != j) {
                    throw new MAPIException('CUSTONER_NO_SAME')
                }
            }
        }
        // 如果错误更新，并返回错误 <-- s1
        // 检查平台过度账户的分润配置信息 手续费的收取账户, 检查手续费是否可以继续扣除
        log.info "customer(royalty) service: $royaltyService"
        def onlineService = CustomerService.find(
                "from CustomerService where customerId=? and serviceCode=? and enable=true and isCurrent=true",
                [partner.id, 'online'])
        log.info "customer(online) service: $onlineService"
        if (!onlineService) {
            throw new MAPIException('SERVICE_NOT_SUPPORT')
        }
        // 手续费是百分之
        def feeRate = new BigDecimal(onlineService.feeParams)
        def feeAmount = 0
        def serviceParams = JSONObject.fromObject(royaltyService.serviceParams)
        String payfeeCusno = serviceParams.payfee_customer_no
        //只在第一次分润时扣除手续费
        if (!tradeBase) {
            feeAmount = (payment.amount * feeRate / 100) as Long
            if (customers[payfeeCusno] < 0 || customers[payfeeCusno] < feeAmount) {
                throw new MAPIException('ILLEGAL_ROYALTY_AMOUNT')
            }
        }

        // 查询指令中涉及到的客户
        // 读取关联账户号
        // 检查客户、账户是否正常、绑定是否正常
        def royCustomer = [:]
        for (customerNo in customers.keySet()) {
            if (customerNo == partner.customerNo) {
                royCustomer[customerNo] = partner
                continue
            }
            def cus = Customer.findByCustomerNo(customerNo)
            if (!cus || cus.status != 'normal') {
                throw new MAPIException('USER_NOT_EXIST')
            }
//            def bind = RoyaltyBinding.findByPartnerAndCustomer(partner, cus)
//            if (!bind || (bind.status!='bind' && bind.bizType=='10')  || bind.nopassRefundFlag!='T') {
//                throw new MAPIException( 'BINDING_NOT_EXIST' )
//            }
            def bind = RoyaltyBinding.findWhere([partner: partner, customer: cus, bizType: '10', status: 'bind', nopassRefundFlag: 'T'])
            if (!bind) {
                throw new MAPIException('BINDING_NOT_EXIST')
            }
            royCustomer[customerNo] = cus
        }
        def now = new Date()
        def tradeList = []
        def acPackage = new AccountCommandPackage(redoMode: true)
        TradeBase.withTransaction { trx ->
            // 更新分润交易 指令序号，指令JSON 发送状态，发送时间
            if (!tradeBase) {
                payment.feeAmount = feeAmount
            }
            payment.royaltyParams = params.royalty_parameters
            payment.royaltyStatus = 'processing'
            payment.save failOnError: true
            // 将指令中的客户ID转换成帐号
            for (item in royaltyItems) {
                def payerAccountNo = (partner.customerNo == item.fromCustomerNo) ? royaltyService.srvAccNo : royCustomer[item.fromCustomerNo].accountNo
                if (!payerAccountNo) {
                    log.error "can't found customer(${item.fromCustomerNo}) main/royalty account!"
                    throw new MAPIException('GENERAL_FAIL')
                }
                if (item.amount > 0) {
                    def trade = new TradeBase(
                            rootId: payment.rootId,
                            originalId: payment.id,
                            tradeType: 'royalty',
                            partnerId: partner.id,
                            payerId: royCustomer[item.fromCustomerNo].id,
                            payerName: royCustomer[item.fromCustomerNo].name,
                            payerAccountNo: payerAccountNo,
                            payerCode: item.fromEmail,
                            payeeId: royCustomer[item.toCustomerNo].id,
                            payeeName: royCustomer[item.toCustomerNo].name,
                            payeeCode: item.toEmail,
                            payeeAccountNo: royCustomer[item.toCustomerNo].accountNo,
                            tradeNo: noGeneratorService.createTradeNo('royalty', now),
                            outTradeNo: payment.outTradeNo,
                            amount: item.amount,
                            currency: payment.currency,
                            subject: item.subject,
                            status: 'processing',
                            tradeDate: StringUtil.getNumericDate(now) as int
                    ).save(flush: true, failOnError: true)
                    tradeList << trade
                    acPackage.append trade

                    if (item.toCustomerNo == payfeeCusno && trade.amount >= feeAmount && feeAmount > 0) {
                        // 在原有的指令上追加一个手续费扣除指令
                        acPackage.append(
                                fromAccountNo: trade.payeeAccountNo,
                                toAccountNo: InnerAccount.getFeeAccountNo(),
                                amount: feeAmount,
                                subject: "${payment.tradeNo}",
                                transferType: 'fee'
                        )
                    }
                }
            }
            acPackage.save()
        } // 提交第一个事务
        // 这种是必成功的交易，做到这里就认为交易已经成功了

        try {
            TradePayment.withTransaction { trx ->
                // 发送指令, 接受指令结果
                def resp = accountClientService.executeCommands(acPackage)
                // 根据结果更新
                if (resp.result == 'true') {
                    //分润完成
                    if (amo - amount == 0) {
                        payment.royaltyStatus = 'completed'
                    } else {
                        //可以多次分润。
                        payment.royaltyStatus = 'starting'
                    }
                    payment.save failOnError: true
                    tradeList.each { trade ->
                        trade.status = 'completed'
                        trade.save failOnError: true
                    }
                }
                acPackage.update resp
            }
        } catch (e) {
            log.error e, e
        } // 提交第二个事务
        // 返回
        writeResponse 'SUCCESS'
        log.info 'royalty ok'
    }
}
