package com.dounine.corgi.rpc.proxy;

import com.alibaba.fastjson.JSON;
import com.dounine.corgi.rpc.container.Container;
import com.dounine.corgi.rpc.container.HttpContainer;
import com.dounine.corgi.rpc.http.rep.ResponseText;
import com.dounine.corgi.rpc.http.utils.InputStreamUtils;
import com.dounine.corgi.rpc.invoke.HttpInvoke;
import com.dounine.corgi.rpc.invoke.Invoke;
import com.dounine.corgi.rpc.invoke.config.Provider;
import com.dounine.corgi.rpc.serialize.Request;
import com.dounine.corgi.rpc.spring.ApplicationBeanUtils;
import com.dounine.corgi.rpc.utils.ParserUtils;
import org.apache.commons.lang3.StringUtils;
import org.mortbay.jetty.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dounine.corgi.rpc.serialize.Constant.*;

/**
 * 服务注册
 */
public class ProviderProxyFactory extends AbstractHandler {

    private static ProviderProxyFactory PROVIDERPROXYFACTORY;
    private final Invoke invoke = HttpInvoke.instanct();
    private final Map<Class, Object> providers = new ConcurrentHashMap<>();
    private Provider provider;
    private static final int ERR_CODE = 1;

    public ProviderProxyFactory(Map<Class, Object> providers, Provider provider) {
        this.provider = provider;
        if (!Container.isStart) {
            new HttpContainer(this, provider).start();
        }
        if (null != providers) {
            for (Map.Entry<Class, Object> entry : providers.entrySet()) {
                register(entry.getKey(), entry.getValue());
            }
        }
        PROVIDERPROXYFACTORY = this;
    }

    @Override
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        String data = null;
        if (request.getMethod().equals(METHOD_GET)) {
            data = request.getParameter(RPC_NAME);
        } else if (request.getMethod().equals(METHOD_POST)) {
            data = InputStreamUtils.istreamToString(request.getInputStream());
            if (null != data) {
                if(data.startsWith(RPC_NAME+"=")){
                    data = data.substring(RPC_NAME_LEN);
                }else{
                    data = null;
                }
            }
        }
        ResponseText responseText = new ResponseText();
        responseText.setErrno(ERR_CODE);
        if (StringUtils.isNotBlank(data)) {
            data = URLDecoder.decode(data, "utf-8");
            Request req = JSON.parseObject(data, Request.class);
            Object bean = providers.get(req.getClazz());
            try {
                this.utf8Chartset(response);
                if (null == req.getClazz()) {
                    responseText.setMsg("clazz not empty");
                } else if (StringUtils.isBlank(req.getMethodName())) {
                    responseText.setMsg("invoke method not empty");
                } else {
                    int len = req.getParameterTypes().length;
                    Object[] argsObj = new Object[len];
                    for(int i =0;i<len;i++){
                        Class<?> clazz = req.getParameterTypes()[i];
                        argsObj[i] = ParserUtils.parseObject(req.getArgs()[i],clazz);
                    }
                    Object oo = ApplicationBeanUtils.getAac().getBean(req.getClazz());
                    for(Method method : oo.getClass().getMethods()){
                        if(method.getName().equals(req.getMethodName())){
                            Object object = method.invoke(oo, argsObj);
                            if(null!=object){
                                responseText.setData(object);
                            }
                            break;
                        }
                    }
//                    Object object = oo.getClass().getMethod(req.getMethodName(), req.getParameterTypes()).invoke(oo, argsObj);
//                    if(null!=object){
//                        responseText.setData(object);
//                    }
                }
                responseText.setErrno(ResponseText.SUCCESS_CODE);
            }catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                responseText.setMsg(e.getTargetException().getMessage());
                e.printStackTrace();
//            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
//                responseText.setMsg("not such method");
//            }
            }
            invoke.push(responseText, response.getOutputStream());
        }else{
            responseText.setMsg(RPC_NAME+" attr not empty");
            invoke.push(responseText, response.getOutputStream());
        }
    }

    public void utf8Chartset(HttpServletResponse response) {
        response.setHeader("Accept", "*");
        response.setHeader("Server", "CORGI-RPC(1.0.0)");
        response.setContentType("text/html;charset=utf-8");
    }

    public void register(Class clazz, Object obj) {
        providers.put(clazz, obj);
        if(null!=provider){
            provider.register(clazz);
        }
    }

    public static ProviderProxyFactory instance() {
        return PROVIDERPROXYFACTORY;
    }
}
