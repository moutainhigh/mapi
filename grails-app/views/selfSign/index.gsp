<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <meta http-equiv="Content-Language" content="zh-cn"/>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>自助签约</title>
</head>
<STYLE type=text/css>
<!--
body {
    font-size: 12px;
    background-color: #d3d3d3;
}

ul, li, ol, dl, dd, h1, h2, h3, h4, h5, p, form, fieldset, img {
    margin: 0;
    padding: 0;
    list-style: none;
    border: 0;
    font-weight: 400;
    font-size: 12px;
}

.clear {
    clear: both;
    font-size: 1px;
    height: 1px;
    padding: 0;
    margin: 0;
}

a {
    color: #077ac7;
    text-decoration: none;
}

a:hover {
    color: #ff6d00;
    text-decoration: underline;
}

a.red {
    color: #d03410;
    text-decoration: underline;
}

.inp {
    width: 150px;
    height: 14px;
    border: 0px;
    background-color: #FFFFFF;
    margin-left: 2px;
    margin-top: 1px;
}

.box {
    width: 552px;
    height: 226px;
    background: url(${resource(dir: 'js/image', file: 'bj.gif')}) no-repeat;
    padding-left: 39px;
    margin: auto;
    margin-top: 100px;
    padding-top: 152px;
}

.box1 {
    height: 33px;
}

.box1 h1 {
    float: left;
    font-size: 14px;
    text-align: left;
    font-weight: bold;
    color: #FFFFFF;
    width: 257px;
    line-height: 33px;
}

.box1 h2 {
    float: left;
    font-size: 12px;
    text-align: left;
    color: #FFFFFF;
    width: 123px;
    line-height: 33px;
}

.box1 h3 {
    float: left;
    font-size: 12px;
    text-align: left;
    color: #FFFFFF;
    width: 110px;
    line-height: 33px;
}

.box1 h4 {
    float: left;
    font-size: 12px;
    text-align: left;
    color: #FFFFFF;
    width: 23px;
    line-height: 33px;
    margin-top: 6px;
}

.error {
    height: 14px;
    line-height: 14px;
    padding-left: 26px;
    background: url(${resource(dir: 'js/image', file: 'd1.gif')}) no-repeat;
    font-size: 12px;
    color: #b02403;
    text-align: left;
    margin-top: 13px;
}

.login1 {
    height: 21px;
}

.login1 h1 {
    width: 92px;
    float: left;
    height: 21px;
    line-height: 21px;
    text-align: right;
    color: #666666;
}

.login1 h2 {
    width: 160px;
    float: left;
    background: url(${resource(dir: 'js/image', file: 'd3.gif')}) no-repeat left top;
    height: 21px;
    line-height: 21px;
}

.login1 h3 {
    float: left;
    margin-left: 12px;
}

.login1 h4 {
    line-height: 21px;
    margin-left: 12px;
    float: left;
}

.login1 h5 {
    float: left;
    overflow: hidden;
    margin-left: 47px;
    margin-top: 10px;
}

-->
</STYLE>

<body>
<g:form action="confirm" useToken="true">
    <div class="box">
        <div class="box1">
            <h1>自助签约协议</h1>
            <h2>交易平台：${session.partner.name}</h2>
            <h3>开通委托服务：</h3>
            <h4><input name="nopassRefundFlag" type="checkbox" checked="true" value="true"></h4>
        </div>
        <g:if test="${flash.errorMessage}">
            <div class="error">
                ${flash.errorMessage}
            </div>
        </g:if>

        <div class="login1" style="margin-top:20px;">
            <h1>签约客户账户：<font color="red">*</font></h1>
            <font size="3" style="font-weight:bold;color:#666666">${session.qy_params.email}</font>
            <input type='hidden' name=login_certificate value="${session.qy_params.email}"/>
        </div>
        <div class="login1" style="margin-top:10px;">
            <h1>支付密码：<font color="red">*</font></h1>
            <h2><INPUT class=inp name=pay_password type="password"></h2>
        </div>
        <div class="login1" style="margin-top:10px;">
            <h1>单笔支付限额：</h1>
            <h2><INPUT class=inp name=amount type="text"></h2>
        </div>
        <div class="login1" style="margin-top:10px;">
            <h1>验&nbsp;证&nbsp;码：<font color="red">*</font></h1>
            <h2><input name="captcha" class=inp type="text">
            </h2>
            <h3><img src="${createLink(controller: 'captcha')}" width="76" height="25" onclick="this.src = '${createLink(controller: 'captcha')}?' + new Date().getTime()"></h3>
            %{--<h4><a href="aaa" class="red">看不清？</a></h4>--}%
            <h5><input type="submit" value="确认并提交"></h5>
        </div>

    </div>

</g:form>

</BODY></HTML>