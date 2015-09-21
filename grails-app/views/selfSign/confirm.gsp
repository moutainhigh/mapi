<%@ page contentType="text/html;charset=UTF-8" %>
<html>
  <head><title>自助签约</title></head>
  <body>
  <p>
      ${slave.name}已经成为${session.partner.name}的自助签约商户。<br/>
  </p>
  <p>
      <form action="${return_url}" method="get">
      <g:each in="${resp_params.keySet()}" var="req_key">
          <input name="${req_key}" readonly value="${resp_params[req_key]}" type="text" size="64"> ${req_key}<br/>
      </g:each>
      <hr/>
      <input value="通知${session.partner.name}已经签约成功" type="submit">
      </form>
  </p>
  <% session.invalidate() %>
  </body>
</html>