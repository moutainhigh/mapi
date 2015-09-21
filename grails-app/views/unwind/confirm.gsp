<%@ page contentType="text/html;charset=UTF-8" %>
<html>
  <head><title>自助解约</title></head>
  <body>
  <p>
      ${slave.name}已经成功和${session.partner.name}解除签约。<br/>
  </p>
  <p>
      <form action="${return_url}" method="get">
      <g:each in="${resp_params.keySet()}" var="req_key">
          <input name="${req_key}" readonly="true" value="${resp_params[req_key]}" type="text" size="64"> ${req_key}<br/>
      </g:each>
      <hr/>
      <input value="通知${session.partner.name}已经解约成功" type="submit">
      </form>
  </p>
  <% session.invalidate() %>
  </body>
</html>