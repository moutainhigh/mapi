package customer

class Customer {
    String name
    String customerNo
    String type
    String status
    String apiKey
    String accountNo
    Date dateCreated
    Date lastUpdated

    static hasMany = [operators: CustomerOperator]

    static constraints = {
        name(maxSize:32,blank: false)
        customerNo(maxSize:24,blank: false)
        type inList:['P','C','A','S']
        status(maxSize:16,inList:['normal','disabled'],blank: false)
        apiKey(maxSize:64,nullable: true)
        accountNo(maxSize:24,nullable:true)
    }

    String toString() {
        return "${name}(${id})"
    }

    static mapping = {
        table 'cm_customer'
        id generator: 'sequence', params: [sequence: 'seq_cm_customer']
    }
}
