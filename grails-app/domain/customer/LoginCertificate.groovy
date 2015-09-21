package customer

class LoginCertificate {
    String  loginCertificate
    String  certificateType
    Boolean isVerify = false

    Date    dateCreated
    Date    lastUpdated

    static belongsTo = [customerOperator: CustomerOperator]

    static constraints = {
    }

    static mapping = {
        table 'cm_login_certificate'
        id generator: 'sequence', params: [sequence: 'seq_cm_login_certificate']
    }
}
