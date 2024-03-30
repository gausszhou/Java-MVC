package com.example.mvc.framework.dispatcher;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO
import com.example.mvc.controller.IndexController;
import com.example.mvc.controller.UserController;

import com.example.mvc.framework.core.ModelAndView;
import com.example.mvc.framework.core.ViewEngine;
import com.example.mvc.framework.annotation.GetMapping;
import com.example.mvc.framework.annotation.PostMapping;

import com.example.mvc.framework.util.FileUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebServlet(urlPatterns = "/")
public class Dispatcher extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private Map<String, GetDispatcher> getMappings = new HashMap<>();

    private Map<String, PostDispatcher> postMappings = new HashMap<>();


    private List<Class<?>> controllers = List.of(IndexController.class, UserController.class);


    private ViewEngine viewEngine;

    public Dispatcher() throws IOException, ClassNotFoundException {
    }

    private static List<Class<?>> scanControllers() throws IOException, ClassNotFoundException {
        // TODO: 可指定 package 并自动扫描，暂时手动实现 Demo:
        List<Class<?>> controllers = new ArrayList<>();
        System.out.println("scanControllers");
        String path = new File("src").getAbsolutePath();
        List<String> classFileClassNames = FileUtil.getAllClassFileClassNames(path);
        for (String classFileClassName : classFileClassNames) {
            // 定义正则表达式来匹配类名、方法名和行号
            String regex = ".*Controller";
            // 编译正则表达式
            Pattern pattern = Pattern.compile(regex);
            // 创建 Matcher 对象
            Matcher matcher = pattern.matcher(classFileClassName);

            if (matcher.find()) {
                System.out.println("match:" + classFileClassName);
            }
        }
        controllers.add(IndexController.class);
        controllers.add(UserController.class);
        return controllers;
    }


    /**
     * 当 Servlet 容器创建当前 Servlet 实例后，会自动调用 init(ServletConfig) 方法
     */
    @Override
    public void init() throws ServletException {
        logger.info("init {}...", getClass().getSimpleName());
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 依次处理每个Controller:
        for (Class<?> controllerClass : controllers) {
            try {
                Object controllerInstance = controllerClass.getConstructor().newInstance();
                // 依次处理每个Method:
                for (Method method : controllerClass.getMethods()) {
                    if (method.getAnnotation(GetMapping.class) != null) {
                        // 处理@Get:
                        processGet(method, controllerInstance);

                    } else if (method.getAnnotation(PostMapping.class) != null) {
                        // 处理@Post:
                        processPost(method, controllerInstance, objectMapper);
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new ServletException(e);
            }
        }
        // 创建ViewEngine:
        this.viewEngine = new ViewEngine(getServletContext());
    }

    private void processGet(Method method, Object controllerInstance) {
        if (method.getReturnType() != ModelAndView.class && method.getReturnType() != void.class) {
            throw new UnsupportedOperationException(
                    "Unsupported return type: " + method.getReturnType() + " for method: " + method);
        }
        for (Class<?> parameterClass : method.getParameterTypes()) {
            if (!supportedGetParameterTypes.contains(parameterClass)) {
                throw new UnsupportedOperationException(
                        "Unsupported parameter type: " + parameterClass + " for method: " + method);
            }
        }
        String[] parameterNames = Arrays.stream(method.getParameters()).map(p -> p.getName())
                .toArray(String[]::new);
        String path = method.getAnnotation(GetMapping.class).value();
        logger.info("Found GET: {} => {}", path, method);
        this.getMappings.put(path, new GetDispatcher(controllerInstance, method, parameterNames,
                method.getParameterTypes()));
    }

    private void processPost(Method method, Object controllerInstance, ObjectMapper objectMapper) {
        if (method.getReturnType() != ModelAndView.class && method.getReturnType() != void.class) {
            throw new UnsupportedOperationException(
                    "Unsupported return type: " + method.getReturnType() + " for method: " + method);
        }
        Class<?> requestBodyClass = null;
        for (Class<?> parameterClass : method.getParameterTypes()) {
            if (!supportedPostParameterTypes.contains(parameterClass)) {
                if (requestBodyClass == null) {
                    requestBodyClass = parameterClass;
                } else {
                    throw new UnsupportedOperationException("Unsupported duplicate request body type: "
                            + parameterClass + " for method: " + method);
                }
            }
        }
        String path = method.getAnnotation(PostMapping.class).value();
        logger.info("Found POST: {} => {}", path, method);
        this.postMappings.put(path, new PostDispatcher(controllerInstance, method,
                method.getParameterTypes(), objectMapper));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp, this.getMappings, "get");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp, this.postMappings, "post");
    }

    private void process(
            HttpServletRequest req,
            HttpServletResponse resp,
            Map<String, ? extends AbstractDispatcher> dispatcherMap,
            String type
    ) throws ServletException, IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        String path = req.getRequestURI().substring(req.getContextPath().length());
        AbstractDispatcher dispatcher = dispatcherMap.get(path);
        if (dispatcher == null) {
            if (type == "get") {
                dispatcher = dispatcherMap.get("/404");
            } else {
                resp.sendError(404);
                return;
            }
        }
        ModelAndView mv = null;
        try {
            mv = dispatcher.invoke(req, resp);
        } catch (ReflectiveOperationException e) {
            throw new ServletException(e);
        }
        if (mv == null) {
            return;
        }
        if (mv.view.startsWith("redirect:")) {
            resp.sendRedirect(mv.view.substring(9));
            return;
        }
        PrintWriter pw = resp.getWriter();
        this.viewEngine.render(mv, pw);
        pw.flush();
    }

    private static final Set<Class<?>> supportedGetParameterTypes = Set.of(int.class, long.class, boolean.class,
            String.class, HttpServletRequest.class, HttpServletResponse.class, HttpSession.class);

    private static final Set<Class<?>> supportedPostParameterTypes = Set.of(HttpServletRequest.class,
            HttpServletResponse.class, HttpSession.class);
}

