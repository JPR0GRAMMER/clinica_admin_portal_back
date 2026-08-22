package com.upn.gestion_clinica.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/info")
public class InfoController {

    private final RequestMappingHandlerMapping handlerMapping;

    public InfoController(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @GetMapping("/endpoints")
    public List<Map<String, String>> getAllEndpoints() {
        List<Map<String, String>> endpoints = new ArrayList<>();
        
        Map<RequestMappingInfo, HandlerMethod> map = handlerMapping.getHandlerMethods();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : map.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            HandlerMethod method = entry.getValue();
            

            if (method.getBeanType().getName().startsWith("org.springframework")) {
                continue;
            }
            
            if (info.getPatternValues() != null && !info.getPatternValues().isEmpty()) {
                for (String path : info.getPatternValues()) {
                    Map<String, String> endpointData = new HashMap<>();
                    String httpMethod = info.getMethodsCondition().getMethods().isEmpty() ? "ALL" : info.getMethodsCondition().getMethods().iterator().next().name();
                    endpointData.put("method", httpMethod);
                    endpointData.put("path", path);
                    endpointData.put("controller", method.getBeanType().getSimpleName());
                    endpoints.add(endpointData);
                }
            }
        }
        
        return endpoints;
    }
}
