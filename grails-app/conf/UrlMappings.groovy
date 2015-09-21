class UrlMappings {

    static mappings = {
        "/service/$controller" {
            constraints {
                controller matches: /royalty|refund|asynRefund|refundSearch|advanceSearch|repayment|frozen|royaltyFrozen|royaltyFrozenSearch|unfrozen|royaltyUnfrozen/
            }
        }

        "/query/payment"(controller: 'queryPayment')

        "/query/paymentSearch"(controller: 'queryPaymentSearch')

        "/query/orderSearch"(controller: 'queryOrderSearch')

        "/verify/notify"(controller: 'notifyVerify')

        "/selfservice/binding/$action?"(controller: 'binding')
        //自助签约
        "/selfservice/selfSign/$action?"(controller: 'selfSign')
        //解约
        "/selfservice/unwind/$action?"(controller: 'unwind')
        //签约查询
        "/selfservice/signSearch/$action?"(controller: 'signSearch')
        //委托代扣
        "/selfservice/entrust"(controller: 'entrust')
        //异步通知
        "/verify/mapiAsyncNotify"(controller: 'mapiAsyncNotify')
        //单独商户绑定
        "/selfservice/directBinding"(controller: 'directBinding')

        "/captcha"(controller: 'captcha')

        "/debug"(controller: 'debug')

        //支付宝退款
        "/bankRefund/refund/$action?"(controller: 'refund')

//		"/$controller/$action?"{
//			constraints {
//				// apply constraints here
//			}
//		}

        "/"(view: "/index")
        "500"(view: '/error')
        "403"(view: '/forbidden')
        "404"(view: '/error')
        "405"(view: '/forbidden')
    }
}
