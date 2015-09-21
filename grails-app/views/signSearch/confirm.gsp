<%@ page contentType="text/html;charset=UTF-8" %>
<html>
  <head><title>自助签约查询</title></head>
  <body>
  <p>
      ${slave.name}自助签约查询已成功。<br/>
  </p>
  <p>
      <form action="${return_url}" method="get">
      <g:each in="${resp_params.keySet()}" var="req_key">
          <input name="${req_key}" value="${resp_params[req_key]}" type="text" size="64"> ${req_key}<br/>
      </g:each>
      <hr/>
      <input value="通知${session.partner.name}签约查询成功" type="submit">
      </form>
  </p>
  <% session.invalidate() %>
  </body>
</html>