package mapi

import net.sf.json.JSONObject
import ebank.tools.FormFunction
import ebank.tools.StringUtil
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.EncoderRegistry
import static groovyx.net.http.Method.POST
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import customer.Customer


class HttpNotifyJob {
    // def timeout = 5000l // execute job once in 5 seconds
    def noGeneratorService
    def concurrent = false

    static triggers = {
        simple name: 'notify', startDelay: 5000, repeatInterval: 60000
    }

    def execute() {
        if (ConfigurationHolder.config.job.httpNotify!='enable') return
        log.info 'in notify job'
        def exe = {AsyncNotify notify ->
            try {
                if (!notify.notifyAddress || !notify.notifyContents) return
                def now = new Date()
                notify.notifyId = noGeneratorService.createNotifyId()
                notify.notifyTime = now
                notify.attemptsCount++
                def params = FormFunction.compressMap(
                        JSONObject.fromObject(notify.notifyContents)
                )
                params.notify_time = StringUtil.getFullDateTime(notify.notifyTime)
                params.notify_id   = notify.notifyId
                if (notify.customer && notify.signType=='md5') {
                    params.sign_type = 'md5'
                    params.sign = FormFunction.createMD5Sign(
                            params, notify.customer.apiKey, notify.outputCharset)
                }
                AsyncNotify.withTransaction { status->
                    log.info status
                    log.info status.hasSavepoint()
                    log.info status.isCompleted()
                    log.info status.isNewTransaction()
                    notify.save flush: true, failOnError: true
                }
//                params.each {k,v-> log.info"${notify.notifyId}: $k -> $v"}
                log.info "notify ${notify.notifyAddress} :\n$params"
                def http = new HTTPBuilder( notify.notifyAddress )
                http.encoderRegistry = new EncoderRegistry( charset: notify.outputCharset )

                http.request(POST) {req->
                    //body=params
                    requestContentType= ContentType.URLENC
                    body=params
                    req.getParams().setParameter("http.connection.timeout", new Integer(60000));
                    req.getParams().setParameter("http.socket.timeout", new Integer(60000));

                    response.success={resp,reader->
                        String responsetxt="";
                        if(reader instanceof java.io.InputStreamReader || reader instanceof java.io.InputStream){
                            responsetxt=reader.text
                        }else{
                           log.debug reader?.class?.metaClass
                           responsetxt=reader;
                        }
                        log.info "${notify.notifyAddress} client ResponseResult:[$responsetxt]"
                        if(responsetxt!=null&&!"".equals(responsetxt)){
                            responsetxt=responsetxt.replaceAll(System.getProperty("line.separator"),"");
                            responsetxt=responsetxt.replaceAll("\n","");
                            responsetxt=responsetxt.replaceAll(" ","")
                        }
                        log.info("after replace responsetxt:"+responsetxt)

                        notify.response=responsetxt?.length()>250?responsetxt?.substring(0,249):responsetxt

                        if ( 'success' == responsetxt||"success".equalsIgnoreCase(responsetxt)||responsetxt?.indexOf('success')>=0 ) {
                            notify.status = 'success'
                        } else {
                            long interval = (notify.attemptsCount > 5) ?
                                3600000 : ((2**notify.attemptsCount) * 60 * 1000)
                            if(notify.attemptsCount>=10) notify.status="fail";
                            notify.nextAttemptTime = new Date(now.time + interval)
                            log.info 'response fail.'
                        }
                        AsyncNotify.withTransaction { status ->
                            log.info status
                            log.info status.hasSavepoint()
                            log.info status.isCompleted()
                            log.info status.isNewTransaction()
                            notify.save failOnError: true
                        }
                    }
                    response.failure = { resp ->
                      log.info  "Something happened: ${resp.statusLine}"+ params
                       notify.response=resp.statusLine
                       if(notify.attemptsCount>=20) notify.status="fail";
                       AsyncNotify.withTransaction { status ->
                            log.info status
                            log.info status.hasSavepoint()
                            log.info status.isCompleted()
                            log.info status.isNewTransaction()
                            notify.save failOnError: true
                        }
                    }
                }

            } catch (e) {
                notify.response=e.message.length()>250?e.message.substring(0,249):e.message
                long interval = (notify.attemptsCount > 5) ?
                                3600000 : ((2**notify.attemptsCount) * 60 * 1000)
                 def now = new Date()
                 notify.nextAttemptTime = new Date(now.time + interval)
                if(notify.attemptsCount>=10) notify.status="fail";
                AsyncNotify.withTransaction { status ->
                            log.info status
                            log.info status.hasSavepoint()
                            log.info status.isCompleted()
                            log.info status.isNewTransaction()
                            notify.save failOnError: true
                }
                log.error e, e
            }
        }

        AsyncNotify.findAll(
                "from AsyncNotify an inner join fetch an.customer c " +
                "where an.notifyMethod='http' and an.status='processing' " +
                "and an.nextAttemptTime < current_timestamp() and an.timeExpired > current_timestamp()"
        ).each { exe(it) }
        log.info 'end notify job'
    }
}
