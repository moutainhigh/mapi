<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <meta http-equiv="Content-Language" content="zh-cn"/>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>绑定</title>
</head>
<STYLE type=text/css>
<!--
body { font-size: 12px; background-color:#d3d3d3;}
ul, li, ol, dl, dd, h1, h2, h3, h4, h5, p, form, fieldset, img { margin: 0; padding: 0; list-style: none; border: 0; font-weight: 400; font-size: 12px; }.clear { clear: both; font-size: 1px; height: 1px; padding: 0; margin: 0; }
a { color: #077ac7; text-decoration: none; }
a:hover { color:#ff6d00; text-decoration:underline;}
a.red { color: #d03410; text-decoration: underline; }

.inp { width:150px; height:14px; border:0px; background-color:#FFFFFF; margin-left:2px; margin-top:1px;}

.box {width:552px; height:226px; background:url(${resource(dir: 'js/image', file: 'bj.gif')}) no-repeat; padding-left:39px; margin:auto; margin-top:100px; padding-top:152px;}
.box1 { height:33px;}
.box1 h1 { float:left; font-size:14px;text-align:left; font-weight:bold;  color: #FFFFFF; width:257px; line-height:33px;}
.box1 h2 { float:left; font-size:12px;text-align:left;  color: #FFFFFF;width:123px; line-height:33px;}
.box1 h3 { float:left; font-size:12px;text-align:left;  color: #FFFFFF;width:120px; line-height:33px;}
.box1 h4 { float:left; font-size:12px; text-align:left; color: #FFFFFF; width:13px; line-height:33px; margin-top: 6px; }

.error { height:14px; line-height:14px; padding-left:26px; background:url(${resource(dir: 'js/image', file: 'd1.gif')}) no-repeat; font-size:12px; color:#b02403; text-align:left; margin-top:13px;}
.login1 { height:21px;}
.login1 h1 { width:92px; float:left;height:21px; line-height:21px; text-align:right; color:#666666;}
.login1 h2 { width:160px; float:left; background:url(${resource(dir: 'js/image', file: 'd3.gif')}) no-repeat left top;height:21px; line-height:21px;}
.login1 h3 { float:left; margin-left:12px;}
.login1 h4 { line-height:21px; margin-left:12px; float:left;}
.login1 h5 { float:left; overflow:hidden; margin-left:47px; margin-top: 10px; }

.div1{
	float:left; font-size:14px;text-align:left; font-weight:bold;  color: #FFFFFF; line-height:33px;
}
.div2{
	 float:left; margin-left:20px; overflow: hidden; white-space : nowrap  ; text-overflow:ellipsis;  font-size:12px; text-align:left; width:240px;  color: #FFFFFF; line-height:33px;
}
.div3{
	float:right; font-size:12px; text-align:left; color: #FFFFFF; width:13px; line-height:33px; margin-top: 6px; margin-right:10px
}
.div4{
	float:right; font-size:12px;text-align:left;  color: #FFFFFF;width:100px; line-height:33px;
}


-->
</STYLE>

<body>
<g:form action="confirm" useToken="true">
    <div class="box">
        <div class="box1">
            <div class="div1">交易绑定协议</div>
            <div class="div2" title="${session.partner.name}">交易平台：${session.partner.name}</div>
            <div class="div3"><input name="nopassRefundFlag" type="checkbox" checked="true" value="true"></div>
             <div class="div4">无障碍退款协议：</div>
        </div>
        <g:if test="${flash.errorMessage}">
            <div class="error">
                    ${flash.errorMessage}
            </div>
        </g:if>

        <div class="login1" style="margin-top:20px;">
            <h1>绑定客户账户：<font color="red">*</font></h1>
            <h2><input class=inp name="login_certificate" type="text"></h2>
        </div>
        <div class="login1" style="margin-top:10px;">
            <h1>支付密码：<font color="red">*</font></h1>
            <h2><INPUT class=inp name=pay_password type="password"></h2>
        </div>

        <div class="login1" style="margin-top:10px;">
            <h1>验&nbsp;证&nbsp;码：<font color="red">*</font></h1>
            <h2><input name="captcha" class=inp type="text">
            </h2>
            <h3><img src="${createLink(controller: 'captcha')}" width="76" height="25" onclick="this.src='${createLink(controller: 'captcha')}?'+new Date().getTime()"></h3>
            %{--<h4><a href="aaa" class="red">看不清？</a></h4>--}%
            <h5><input type="submit" value="确认并提交"></h5>
        </div>

    </div>

</g:form>

</BODY></HTML>
