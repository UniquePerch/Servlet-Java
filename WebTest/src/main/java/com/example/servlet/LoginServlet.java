package com.example.servlet;

import com.example.entity.User;
import com.example.mapper.UserMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import lombok.SneakyThrows;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;


import java.io.IOException;
import java.util.Map;

@WebServlet(value = "/login",loadOnStartup = 1)
public class LoginServlet extends HttpServlet {
    SqlSessionFactory factory;
    @Override
    @SneakyThrows
    public void init() throws ServletException {
        factory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader("mybatis-config.xml"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Cookie[] cookies = req.getCookies();
        if(cookies != null){
            String username = null;
            String password = null;
            for (Cookie cookie : cookies) {
                if(cookie.getName().equals("username")) username = cookie.getValue();
                if(cookie.getName().equals("password")) password = cookie.getValue();
            }
            if(username != null && password != null){
                //登陆校验
                try (SqlSession sqlSession = factory.openSession(true)){
                    UserMapper mapper = sqlSession.getMapper(UserMapper.class);
                    User user = mapper.getUser(username, password);
                    if(user != null){
                        resp.sendRedirect("time"); //如果cookie的登录信息有效,直接重定向到主页面
                        return;   //直接返回
                    }
                    else{//如果登录校验失败，清除cookie内容
                        Cookie cookie_username = new Cookie("username", username);
                        Cookie cookie_password = new Cookie("password", password);
                        cookie_password.setMaxAge(0);
                        cookie_username.setMaxAge(0);
                        resp.addCookie(cookie_username);
                        resp.addCookie(cookie_password);
                    }
                }
            }
        }
        req.getRequestDispatcher("/").forward(req, resp);   //正常情况还是转发给默认的Servlet帮我们返回静态页面
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        //获取POST请求携带的表单数据
        Map<String, String[]> map = request.getParameterMap();
        //判断表单是否完整
        if(map.containsKey("username") && map.containsKey("password")) {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            try (SqlSession sqlSession = factory.openSession(true)){
                UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
                User user = userMapper.getUser(username,password);
                if(user!=null){
                    if(map.containsKey("remember-me")){   //若勾选了勾选框，那么将表单信息存入cookie中
                        Cookie cookie_username = new Cookie("username", username);
                        cookie_username.setMaxAge(30);
                        Cookie cookie_password = new Cookie("password", password);
                        cookie_password.setMaxAge(30);
                        response.addCookie(cookie_username);
                        response.addCookie(cookie_password);
                    }
                    HttpSession session = request.getSession();
                    session.setAttribute("user",user);
                    response.sendRedirect("time"); //重定向，不会传递数据
//                    getServletContext().setAttribute("text","我是全局数据");
//                    request.getRequestDispatcher("/time").forward(request,response);
//                    response.getWriter().write("用户"+username+",登录成功");
                }
                else{
                    response.getWriter().write("登录失败");
                }
            }
            //权限校验（待完善）
        }else {
            response.getWriter().write("错误，您的表单数据不完整！");
        }
    }
}
