package mapi

import java.sql.Timestamp

class NotifyVerifyController extends BaseController {
    protected setConstraints() {
        required_attr = ['merchant_ID', 'notify_id']
        verifySign = false
    }

    protected execute() {
        log.info 'in notify verify'
        def okDate = new Timestamp(System.currentTimeMillis() - 60000)
        println "verfiy:"+params.notify_id
        log.info "notifyId: ${params.notify_id}, okData: $okDate, customerNo: ${params.merchant_ID}"
        def notify = AsyncNotify.find(
                "from AsyncNotify where notifyId=? and lastUpdated>sysdate-3/(24*60) and customer.customerNo=? and isVerify=false",
                [params.notify_id, params.merchant_ID]
        )
        log.info "notify: $notify"
        if (notify) {
            notify.isVerify = true
            render 'true'
        } else {
            render 'false'
        }
        log.info 'notify verify end'
    }
}
