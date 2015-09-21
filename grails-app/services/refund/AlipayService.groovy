package refund

import com.alipay.util.AlipayFunction
import com.alipay.util.AlipayNotify
import trade.TradeRefund

class AlipayService {

    static transactional = true
    def key = "sowfs0am2six6bc2lo7b08fexsz8oxna"

    /**
     * 处理支付宝返回的退款结果信息
     */
    def processRespResult(def request, def response)  {
            String backResult = "";
            //获取支付宝POST过来反馈信息
            Map params = new HashMap();
            Map requestParams = request.getParameterMap();
            for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext();) {
               String name = (String) iter.next();
               String[] values = (String[]) requestParams.get(name);
               String valueStr = "";
               for (int i = 0; i < values.length; i++){
                   valueStr = (i==values.length-1) ? valueStr+values[i] : valueStr+values[i]+",";
               }
                //乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
                //valueStr = new String(valueStr.getBytes("ISO-8859-1"), "UTF-8");
                params.put(name, valueStr);
            }

            //判断responsetTxt是否为ture，生成的签名结果mysign与获得的签名结果sign是否一致
            //responsetTxt的结果不是true，与服务器设置问题、合作身份者ID、notify_id一分钟失效有关
            //mysign与sign不等，与安全校验码、请求时的参数格式（如：带自定义参数等）、编码格式有关
            String mysign = AlipayNotify.GetMysign(params,key);
            //String responseTxt = AlipayNotify.Verify(request.getParameter("notify_id"));
            String sign = request.getParameter("sign");

            //写日志记录（若要调试，请取消下面两行注释）
            String sWord = "notify_url_log:sign=" + sign + "&mysign=" + mysign + "\n notify回来的参数：" + AlipayFunction.CreateLinkString(params);
            System.out.println(sWord);
            /*AlipayFunction.LogResult(sWord);*/

            //获取支付宝的通知返回参数，可参考技术文档中页面跳转同步通知参数列表(以下仅供参考)
            //获取批次号
            String batch_no = request.getParameter("batch_no");

            //获取批量退款数据中转账成功的笔数
            String success_num = request.getParameter("success_num");

            //获取批量退款数据中的详细信息
            String result_details = new String(request.getParameter("result_details").getBytes("ISO-8859-1"),"UTF-8");
            System.out.println("result_details value is :" + result_details);
            //格式：第一笔交易#第二笔交易#第三笔交易
            //第N笔交易格式：交易退款信息
            //交易退款信息格式：原付款支付宝交易号^退款总金额^处理结果码^结果描述

            //获取支付宝的通知返回参数，可参考技术文档中页面跳转同步通知参数列表(以上仅供参考)//

            //if(mysign.equals(sign) && responseTxt.equals("true")){//验证成功
            if(mysign.equals(sign)){//验证成功
                String[] results = result_details.split("#");
                String[] seqs;
                if(results.size()==1){
                    seqs = new String[1];
                    seqs[0] = results[0].split("\\^")[0];
                }else if(results.size()>1){
                    seqs = new String[results.size()];
                    for(int i=0;i<results.size();i++){
                       seqs[i] = results[i].split("\\^")[0];
                    }
                }
                String ids = "";
                for(int i=0; i<seqs.size(); i++){
                    ids = ids + seqs[i]+",";
                }
                int flag = 0;
                String[] ids2 = ids.substring(0,ids.length()-1).split(",");
                for(int i=0; i<ids2.length; i++){
                    println "batch_no : " +batch_no + "====seq : " + ids2[i]
                    TradeRefund tradeRefund = TradeRefund.findByHandleBatchAndAcquirerSeq(batch_no,ids2[i])
                    try{
                         tradeRefund.status='completed'
                         tradeRefund.handleStatus='completed'
                         tradeRefund.save(failOnError:true)
                    }catch(Exception e){
                         flag = 1;
                         e.printStackTrace();
                    }
                }
                if (flag == 0) {
                    backResult = "success";	//向支付宝反馈的成功标志，请不要修改或删除
                }else{
                    backResult = "fail";
                }
            }else{//验证失败
                backResult = "fail";
            }
        return backResult;
    }


}
