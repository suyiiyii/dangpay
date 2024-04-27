package top.suyiiyii.su;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.su.exception.Http_400_BadRequestException;
import top.suyiiyii.su.validator.Validator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;

/**
 * 用来处理请求和响应的工具类
 * <p>
 * 封装了几个经常要用到的方法，比如读取请求体和写响应体
 *
 * @author suyiiyii
 * @version 1.5
 * @date 2023.3.25
 */
@Slf4j
public class WebUtils {

    public static final ObjectMapper MAPPER = new ObjectMapper();


    /**
     * 读取请求体（json格式），返回字符串
     *
     * @param req HttpServletRequest
     * @return String
     * @throws IOException IOException
     */
    public static String readRequestBody(HttpServletRequest req) {
        try {
            BufferedReader reader = req.getReader();
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            throw new Http_400_BadRequestException("请求体格式错误");
        }
    }

    /**
     * 读取请求体（json格式），返回对象
     *
     * @param req       HttpServletRequest
     * @param valueType Class<T>
     * @param <T>       T
     * @return T
     */
    public static <T> T readRequestBody2Obj(HttpServletRequest req, Class<T> valueType) {
        BufferedReader reader = null;
        try {
            reader = req.getReader();
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String str = stringBuilder.toString();
            if (str.isEmpty()) {
                throw new IOException("请求体为空");
            }
            // 检查是否有过大的数字
            checkIntSizeInJson(str);
            T t = MAPPER.readValue(str, valueType);
            Validator.check(t);
            return t;
        } catch (IOException e) {
            log.error("请求体格式错误", e);
            throw new Http_400_BadRequestException("请求体格式错误");
        }
    }

    public static void checkIntSizeInJson(String jsonString) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonString);

        // 遍历JSON树
        checkNode(rootNode);
    }

    private static void checkNode(JsonNode node) {
        if (node.isInt()) {
            BigInteger bigInteger = node.bigIntegerValue();
            if (bigInteger.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new Http_400_BadRequestException("传入的数字过大");
            }
        } else if (node.isContainerNode()) {
            for (JsonNode subNode : node) {
                checkNode(subNode);
            }
        }
    }

    /**
     * 写响应体（json格式）
     * <p>
     * 把传入的对象转换成json字符串并写入到响应体，设置响应头的Content-Type为application/json
     *
     * @param resp   HttpServletResponse
     * @param object Object
     * @throws IOException IOException
     */

    public static void respWrite(HttpServletResponse resp, Object object) throws IOException {
        // 设置允许跨域的响应头
        // 允许任何域名发起请求，也可以指定具体的域名
        resp.setHeader("Access-Control-Allow-Origin", "*");
        // 允许的HTTP方法
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE, PATCH");
        // 预检请求缓存时间（单位：秒）
        resp.setHeader("Access-Control-Max-Age", "3600");
        // 允许自定义请求头
        resp.setHeader("Access-Control-Allow-Headers", "*");

        resp.setContentType("application/json");
        PrintWriter pw = resp.getWriter();

        String jsonStr = MAPPER.writeValueAsString(object);
        pw.write(jsonStr);
        pw.flush();
    }


    /**
     * 从请求参数中获取整数值
     *
     * @param req HttpServletRequest 请求对象
     * @param key String 参数名
     * @return int 参数值
     */

    public static int getIntParam(HttpServletRequest req, String key) {
        String param = req.getParameter(key);
        if (param == null) {
            return -1;
        }
        return Integer.parseInt(param);
    }
}
