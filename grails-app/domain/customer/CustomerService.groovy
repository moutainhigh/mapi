package customer

class CustomerService {
    Long    customerId
    String  contractNo
    String  serviceCode
    Date    startTime
    Date    endTime
    Long    customerManagerOperatorId
    String  checkStatus
    Long    checkOperatorId
    Date    checkDate
    String  feeParams
    String  serviceParams
	String 	srvAccNo
    Boolean isCurrent
    Boolean enable

    Date    dateCreated
    Date    lastUpdated

    static constraints = {
        customerId nullable: true
        contractNo(maxSize:20,blank: false)
        serviceCode(maxSize:20,blank: false)
        startTime nullable: true
        endTime nullable: true
        checkStatus(maxSize:16,blank: false, nullable: true)
        feeParams(maxSize:64,blank: false, nullable: true)
        serviceParams(maxSize:128,blank: false, nullable: true)
        checkOperatorId(nullable: true)
        checkDate(nullable: true)
        customerManagerOperatorId(nullable: true)
		srvAccNo(nullable: true)
    }


    static mapping = {
        table 'bo_customer_service'
        id generator: 'sequence', params: [sequence: 'seq_bo_customerservice']
    }
}
