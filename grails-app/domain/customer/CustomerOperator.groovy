package customer

class CustomerOperator {
    String  name
    String  loginPassword
    String  payPassword
    String  defaultEmail
    String  defaultMobile
    Integer loginErrorTime = 0
    Date    lastLoginTime
    String  status
    String  roleSet

    Date    dateCreated
    Date    lastUpdated

    static belongsTo = [customer: Customer]
    static hasMany = [loginCertificate: LoginCertificate]

    static constraints = {
        loginPassword   nullable: true
        payPassword     nullable: true
        defaultMobile   nullable: true
        lastLoginTime   nullable: true
        status  inList: ['normal', 'disabled', 'locked', 'deleted']
        roleSet inList: ['user', 'finance', 'admin']
    }

    static mapping = {
        table 'cm_customer_operator'
        id generator: 'sequence', params: [sequence: 'seq_cm_customer_operator']
    }

    def verifyLoginPassword(loginPwd) {
        if (loginPassword && loginPwd) {
            loginPassword == "${id}${loginPwd}".encodeAsSHA1()
        } else {
            false
        }
    }
    def verifyPayPassword(payPwd) {
        if (payPassword && payPwd) {
            payPassword == "${id}p${payPwd}".encodeAsSHA1()
        } else {
            false
        }
    }
}
