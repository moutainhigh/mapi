package mapi

import customer.Customer

class AsyncNotify {
    Customer    customer
    String  recordTable
    Long    recordId
    String  notifyMethod
    String  notifyAddress
    String  signType
    String  outputCharset = 'gbk'
    String  notifyContents
    Date    notifyTime
    String  notifyId
    Date    nextAttemptTime = new Date()
    String  status
    Boolean isVerify = false
    Integer attemptsCount = 0
    Date    timeExpired
    Date    dateCreated
    Date    lastUpdated

    String response            //record the client response

    static constraints = {
        customer    nullable: true
        recordTable nullable: true
        recordId    nullable: true
        notifyMethod    inList: ['http', 'email', 'mobile']
        signType    nullable: true, inList: ['md5']
        outputCharset   nullable: true
        notifyContents  maxSize: 4096
        notifyTime  nullable: true
        notifyId    nullable: true
        nextAttemptTime nullable: true
        status      inList: ['processing', 'success', 'fail']
        timeExpired     nullable: true
        response   nullable:true,maxSize:256
    }

    static mapping = {
        table 'mapi_async_notify'
        version false
    }
}
