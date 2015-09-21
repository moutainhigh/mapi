import ebank.tools.StringUtil
import net.sf.json.JSONObject
import grails.converters.XML
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.Method
import javax.swing.text.AbstractDocument.Content

def respMap = [ebank:[
        is_success  : 'T',
        result_code : 'SUCCESS',
        timestamp   : StringUtil.getFullDateTime(),
        trades      : [
                [trade_no:'t01', trade_type:'payment', out_trade_no:'ou01', amount:StringUtil.getAmountFromNum(23400)],
                [trade_no:'t02', trade_type:'payment', out_trade_no:'ou02', amount:StringUtil.getAmountFromNum(570)]
        ]
]]

//println respMap
//def json = new JSONObject(respMap)
//println json.toString()
//def xml = respMap as XML
//println xml

def invokeGwInnerApi = {String _uri, Map _args ->
    println "invoke Gateway inner api: $_uri \n args: $_args"
    def result = ''
    try {
        def http = new HTTPBuilder('http://www.testpay.org:18185/')
        http.request( Method.POST, ContentType.TEXT ) { req ->
            uri.path = _uri
            body = _args
            response.success = { resp, reader ->
                println "My response handler got response: ${resp.statusLine}"
                println "Response length: ${resp.headers.'Content-Length'}"
//                System.out << reader
                result = reader.text
            }
        }
//        http.post( path: uri, body: args,
//                requestContentType: ContentType.URLENC ) { resp ->
//            println "gateway return $resp"
//            result = resp.getData()
//        }
    } catch ( e ) {
        e.printStackTrace()
        result = e.getMessage()
    }
    result
}

println invokeGwInnerApi('/ISMSApp/postFaultTrx/create', [id:'qq', iniSts:'0', changeSts:'1', trxamount:500, oper:'cz'])
// println invokeGwInnerApi('postFaultTrx/create', [test1:'aaa', api:'bbb'])