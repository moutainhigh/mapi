package gateway

class Gwsuborders {
    String gwordersid // not null,

    String outtradeno//not null

    String sellerName //

    String sellerCustno //  varchar(18) not null,

    String sellerCode//not null

    String sellerExt

    long amount // amount not null,

    Date createdate// date not null,
    static constraints = {
    }
    static mapping={
        table 'gwsuborders'
        version false
    }
}
