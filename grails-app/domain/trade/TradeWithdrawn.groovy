package trade

class TradeWithdrawn extends TradeBase{

    String  submitType
    Long    customerOperId
    String  submitter
    Long    transferFee
    Long    realTransferAmount
    Long    customerBankAccountId
    String  customerBankCode
    String  customerBankNo
    String  customerBankAccountNo
    String  customerBankAccountName
    Boolean isCorporate
    Long    acquirerAccountId
    String  checkStatus='waiting'
    Long    checkOperatorId
    Date    checkDate
    Long    handleOperId
    String  handleOperName
    String  handleBatch
    String  handleCommandNo
    String  handleStatus
    Date    handleTime

    static constraints = {
        submitType(maxSize: 32,inList: ['manual','automatic'])
        submitter(maxSize: 32)
        customerBankCode(maxSize: 16,nullable: true)
        customerBankNo(maxSize: 16)
        customerBankAccountNo(maxSize: 32)
        customerBankAccountName(maxSize: 40)
        acquirerAccountId(nullable: true)
        checkStatus(maxSize: 16)
        checkOperatorId(nullable: true)
        checkDate(nullable: true)
        handleOperId(nullable: true)
        handleOperName(maxSize:16,nullable: true)
        handleBatch(maxSize:16,nullable: true)
        handleCommandNo(maxSize: 40,nullable: true)
        handleStatus(maxSize:16,inList: ['waiting','fChecked','checked','fRefuse','sRefuse','submited','completed','success','fail','refFail'])
        handleTime(nullable: true)
    }
}
