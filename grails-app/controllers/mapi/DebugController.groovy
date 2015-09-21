package mapi

class DebugController extends BaseController {
    def index = {
        decodeUrlQuery()
        params.each {k, String v ->
            log.info "${k.padLeft(16, ' ')} : $v"
        }
        def resp_string = (params.fail) ? 'fail' : 'success'
        render resp_string
    }
}
