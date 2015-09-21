<%@ page contentType="text/html;charset=UTF-8" %>
<html>
  <head><title>分润系统</title></head>


<style type="text/css">
.box{
	width:896px; height:396px; background:url(${resource(dir: 'images', file: 'bj.gif')}) no-repeat; border-collapse:collapse;
}
.fong1{
	font-size:14px; color:#3393c9; font-weight: bold; line-height:35px; margin:0px; padding:0px;
}
.fong2{
	font-size:12px; color:#666;  line-height:15px; margin:0px; padding:0px;
}
.font3{
	font-size:12px; color:#900; font-weight:bold;line-height:15px; margin:0px; padding:0px;
}
</style>
</head>
<body>
 <table width="100%" height="100%" border="0" cellspacing="0" cellpadding="0">
     <form action="${return_url}" method="get" name="resultfm">
      <g:each in="${resp_params.keySet()}" var="req_key">
          <input name="${req_key}" value="${resp_params[req_key]}" readonly type="hidden" size="64"><br/>
      </g:each>
      <hr/>
  <tr>
    <td align="center">
    	<table width="800" class="box">
          <tr>
            <td valign="top" ><table width="863" border="0" cellspacing="0" cellpadding="0">
              <tr>
                <td height="60" colspan="3" style="border-bottom: dashed 1px #ccc;"><p class=" fong1" style="padding-left:20px;">${slave.name}绑定提示</p></td>
              </tr>
              <tr>
                <td height="191" colspan="3" align="center" class="font3" style="font-size:24px; line-height:50px;">与${session.partner.name}已经绑定成功</td>
              </tr>
              <tr>
                <td width="33%" height="25" align="right" class="fong2">&nbsp;</td>
                <td colspan="2"  class="fong2">&nbsp;

                </td>
              </tr>
              <tr>
                <td align="right">&nbsp;</td>
                <td width="24%" ><input  type= "image"  border=0 src="${resource(dir: 'images', file:'fstz.gif')}" width="137" height="26" >&nbsp;</td>
                <td width="43%" class="fong2" >系统将于<span class="font3" id=msg></span>秒后自动发送</td>
              </tr>
              <tr>
                <td height="56" colspan="3" align="left" valign="bottom"><p class="fong2">&nbsp;您有任何建议或疑问，请告诉我们发送邮件至：<a href="mailto:rongpayservice@126.com">rongpayservice@126.com</a></p></td>
              </tr>
            </table></td>
          </tr>
        </table>
        </form>
        <% session.invalidate() %>
    </td>
   </tr>
</table>
<script language= "JavaScript">
    var i=10;
    function clock(){
	   document.all.msg.innerText=i;
       i=i-1;
       if(i>0) setTimeout( "clock(); ",1000);
       else document.forms["resultfm"].submit();
    }
    clock();
</script>


</body>
</html>