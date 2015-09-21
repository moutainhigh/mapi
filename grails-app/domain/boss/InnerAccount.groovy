package boss

import ebank.lang.MAPIException

class InnerAccount {
    String key
    String accountNo

    static constraints = {}

    static mapping = {
        table   'bo_inner_account'
        cache   usage: 'read-only'
        id      generator:'assigned'
        version false
    }

    static accountNoCache = [:]
    static getAccountNoByKeyName(String key) {
        def accountNo = accountNoCache[key]
        if ( !accountNo ) {
            def innerAccount = InnerAccount.findByKey(key)
            if (innerAccount) {
                accountNo = innerAccount.accountNo
                accountNoCache[key] = accountNo
            }
        }
        if (!accountNo) throw new MAPIException('GENERAL_FAIL', "can't find inner account $key")
        accountNo
    }

    static getMiddleAccountNo() {
        getAccountNoByKeyName('middleAcc')
    }
    static getFeeAccountNo() {
        getAccountNoByKeyName('feeAcc')
    }
    static getGuestAccountNo() {
        getAccountNoByKeyName('guestAcc')
    }
}
