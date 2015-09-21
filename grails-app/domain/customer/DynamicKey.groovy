package customer

class DynamicKey {
    String  sendType
    String  sendTo
    String  parameter
    String  key
    String  procMethod
    String  verification
    Date    timeExpired
    Date    timeUsed
    Boolean isUsed
    String  useType

    Date    dateCreated

    static belongsTo = [customer: Customer]

    static constraints = {
        customer()
        sendTo(maxSize:32,blank: false)
        sendType(maxSize:8,blank: false)
        parameter(maxSize:36,blank: false)
        key(maxSize:32,blank: false)
        procMethod(maxSize:8,blank: false)
        verification(maxSize:36,blank: false)
        timeUsed(nullable: true)
        useType(maxSize: 16,blank:false)
    }

    static mapping = {
        table 'cm_dynamic_key'
        id generator: 'sequence', params: [sequence: 'seq_cm_dynamic_key']
    }
}
