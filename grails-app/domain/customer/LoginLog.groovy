package customer

class LoginLog {
    String  loginCertificate
    String  loginIp
    String  loginResult
    Date    dateCreated

    static belongsTo = [customerOperator: CustomerOperator, customer: Customer]

    static constraints = {
        customer()
        customerOperator()
        loginCertificate(maxSize:64,blank: false)
        loginIp(maxSize:20,blank: false)
        loginResult(maxSize:8,blank: false)
    }

    static mapping = {
        table 'cm_login_log'
        id generator: 'sequence', params: [sequence: 'seq_cm_login_log']
    }
}
