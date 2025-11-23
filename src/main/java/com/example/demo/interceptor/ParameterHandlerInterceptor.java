/**
 *    Copyright 2009-2025 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.example.demo.interceptor;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * MyBatis ParameterHandler拦截器
 *
 * <p>该拦截器用于拦截MyBatis参数处理器(ParameterHandler)的所有核心方法，提供以下功能：</p>
 * <ul>
 *   <li>参数设置前后的预处理和后处理</li>
 *   <li>参数值的监控和日志记录</li>
 *   <li>参数设置异常的处理和记录</li>
 *   <li>参数处理性能监控</li>
 *   <li>参数安全性检查</li>
 * </ul>
 *
 * <p>拦截的方法包括：</p>
 * <ul>
 *   <li>getParameterObject - 获取参数对象</li>
 *   <li>setParameters - 设置PreparedStatement的参数</li>
 * </ul>
 */
@Intercepts({
    // 拦截getParameterObject方法 - 获取参数对象
    @Signature(type = ParameterHandler.class, method = "getParameterObject", args = {}),

    // 拦截setParameters方法 - 设置PreparedStatement参数
    @Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class})
})
public class ParameterHandlerInterceptor implements Interceptor {

    /**
     * 拦截器配置属性
     */
    private Properties properties = new Properties();

    /**
     * 是否启用详细日志记录
     */
    private boolean enableDetailLog = true;

    /**
     * 是否启用性能监控
     */
    private boolean enablePerformanceMonitor = true;

    /**
     * 是否启用参数安全检查
     */
    private boolean enableSecurityCheck = true;

    /**
     * 参数处理慢操作阈值(毫秒)
     */
    private long slowParameterThreshold = 100L;

    /**
     * 拦截方法的核心实现
     *
     * @param invocation 方法调用信息
     * @return 方法执行结果
     * @throws Throwable 执行过程中的异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取方法调用的基本信息
        Object target = invocation.getTarget();
        String methodName = invocation.getMethod().getName();
        Object[] args = invocation.getArgs();

        // 记录开始时间用于性能监控
        long startTime = System.currentTimeMillis();

        if (enableDetailLog) {
            System.out.println("=== ParameterHandler拦截器 - 方法执行开始 ===");
            System.out.println("目标对象: " + target.getClass().getSimpleName());
            System.out.println("执行方法: " + methodName);
            System.out.println("开始时间: " + new java.util.Date(startTime));
        }

        // 根据不同的方法执行不同的预处理逻辑
        preProcess(methodName, args, target);

        Object result = null;
        Throwable exception = null;

        try {
            // 执行原始方法
            result = invocation.proceed();

            // 方法执行成功后的处理
            postProcessSuccess(methodName, args, result, startTime, target);

        } catch (Throwable e) {
            // 方法执行异常时的处理
            exception = e;
            postProcessException(methodName, args, e, startTime);
            throw e;
        } finally {
            // 最终处理逻辑
            finalProcess(methodName, args, result, exception, startTime);
        }

        return result;
    }

    /**
     * 方法执行前的预处理逻辑
     *
     * @param methodName 方法名
     * @param args 方法参数
     * @param target 目标对象
     */
    private void preProcess(String methodName, Object[] args, Object target) {
        switch (methodName) {
            case "getParameterObject":
                handleGetParameterObjectPreProcess(target);
                break;
            case "setParameters":
                handleSetParametersPreProcess(args, target);
                break;
            default:
                if (enableDetailLog) {
                    System.out.println("未知方法: " + methodName);
                }
                break;
        }
    }

    /**
     * 方法执行成功后的处理逻辑
     *
     * @param methodName 方法名
     * @param args 方法参数
     * @param result 执行结果
     * @param startTime 开始时间
     * @param target 目标对象
     */
    private void postProcessSuccess(String methodName, Object[] args, Object result, long startTime, Object target) {
        long executionTime = System.currentTimeMillis() - startTime;

        if (enableDetailLog) {
            System.out.println("--- 方法执行成功 ---");
            System.out.println("执行结果类型: " + (result != null ? result.getClass().getSimpleName() : "null"));

            // 根据不同方法显示特定的结果信息
            displayMethodResult(methodName, result, target);
        }

        // 性能监控
        if (enablePerformanceMonitor) {
            if (executionTime > slowParameterThreshold) {
                System.out.println("⚠️  参数处理慢操作警告: " + methodName + " 执行时间 " + executionTime + "ms 超过阈值 " + slowParameterThreshold + "ms");
            }
        }
    }

    /**
     * 方法执行异常时的处理逻辑
     *
     * @param methodName 方法名
     * @param args 方法参数
     * @param exception 异常信息
     * @param startTime 开始时间
     */
    private void postProcessException(String methodName, Object[] args, Throwable exception, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;

        System.err.println("❌ ParameterHandler方法执行异常:");
        System.err.println("方法名: " + methodName);
        System.err.println("执行时间: " + executionTime + "ms");
        System.err.println("异常类型: " + exception.getClass().getSimpleName());
        System.err.println("异常信息: " + exception.getMessage());

        // 特殊处理SQL异常
        if (exception instanceof SQLException) {
            SQLException sqlException = (SQLException) exception;
            System.err.println("SQL错误代码: " + sqlException.getErrorCode());
            System.err.println("SQL状态: " + sqlException.getSQLState());
        }

        // 记录详细的异常堆栈信息
        if (enableDetailLog) {
            exception.printStackTrace();
        }
    }

    /**
     * 最终处理逻辑(无论成功还是异常都会执行)
     *
     * @param methodName 方法名
     * @param args 方法参数
     * @param result 执行结果
     * @param exception 异常信息
     * @param startTime 开始时间
     */
    private void finalProcess(String methodName, Object[] args, Object result, Throwable exception, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;

        if (enableDetailLog) {
            System.out.println("--- 方法执行完成 ---");
            System.out.println("总执行时间: " + executionTime + "ms");
            System.out.println("执行状态: " + (exception == null ? "成功" : "异常"));
            System.out.println("=== ParameterHandler拦截器 - 方法执行结束 ===\n");
        }
    }

    // ==================== 各种方法的预处理逻辑 ====================

    /**
     * 处理getParameterObject方法的预处理逻辑
     *
     * @param target 目标对象
     */
    private void handleGetParameterObjectPreProcess(Object target) {
        if (enableDetailLog) {
            System.out.println("📋 获取参数对象:");
            System.out.println("  ParameterHandler类型: " + target.getClass().getSimpleName());
        }
    }

    /**
     * 处理setParameters方法的预处理逻辑
     *
     * @param args 方法参数 [PreparedStatement]
     * @param target 目标对象
     */
    private void handleSetParametersPreProcess(Object[] args, Object target) {
        if (enableDetailLog && args.length >= 1) {
            PreparedStatement ps = (PreparedStatement) args[0];

            System.out.println("⚙️  设置PreparedStatement参数:");
            System.out.println("  ParameterHandler类型: " + target.getClass().getSimpleName());
            System.out.println("  PreparedStatement类型: " + ps.getClass().getSimpleName());

            // 尝试获取参数对象进行安全检查
            if (enableSecurityCheck && target instanceof ParameterHandler) {
                try {
                    ParameterHandler parameterHandler = (ParameterHandler) target;
                    Object parameterObject = parameterHandler.getParameterObject();
                    performSecurityCheck(parameterObject);
                } catch (Exception e) {
                    System.err.println("参数安全检查失败: " + e.getMessage());
                }
            }
        }
    }

    // ==================== 显示方法执行结果 ====================

    /**
     * 根据不同方法显示特定的结果信息
     *
     * @param methodName 方法名
     * @param result 执行结果
     * @param target 目标对象
     */
    private void displayMethodResult(String methodName, Object result, Object target) {
        switch (methodName) {
            case "getParameterObject":
                if (result != null) {
                    System.out.println("  参数对象类型: " + result.getClass().getSimpleName());
                    System.out.println("  参数对象值: " + result.toString());

                    // 显示参数的详细信息
                    displayParameterDetails(result);
                } else {
                    System.out.println("  参数对象: null");
                }
                break;
            case "setParameters":
                System.out.println("  参数设置完成");
                // 尝试获取参数对象显示设置的参数信息
                if (target instanceof ParameterHandler) {
                    try {
                        ParameterHandler parameterHandler = (ParameterHandler) target;
                        Object parameterObject = parameterHandler.getParameterObject();
                        if (parameterObject != null) {
                            System.out.println("  设置的参数: " + parameterObject.toString());
                        }
                    } catch (Exception e) {
                        System.out.println("  无法获取参数详情: " + e.getMessage());
                    }
                }
                break;
            default:
                if (result != null) {
                    System.out.println("  返回值: " + result.toString());
                }
                break;
        }
    }

    /**
     * 显示参数的详细信息
     *
     * @param parameterObject 参数对象
     */
    private void displayParameterDetails(Object parameterObject) {
        if (parameterObject == null) {
            return;
        }

        try {
            // 如果是基本类型或字符串，直接显示
            if (isSimpleType(parameterObject)) {
                System.out.println("  参数详情: " + parameterObject);
            } else if (parameterObject instanceof java.util.Map) {
                // 如果是Map类型，显示键值对
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) parameterObject;
                System.out.println("  参数详情(Map): ");
                for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                    System.out.println("    " + entry.getKey() + " = " + entry.getValue());
                }
            } else {
                // 对于复杂对象，显示类名和toString结果
                System.out.println("  参数详情(对象): " + parameterObject.getClass().getSimpleName() + " -> " + parameterObject.toString());
            }
        } catch (Exception e) {
            System.out.println("  参数详情获取失败: " + e.getMessage());
        }
    }

    /**
     * 判断是否为简单类型
     *
     * @param obj 对象
     * @return 是否为简单类型
     */
    private boolean isSimpleType(Object obj) {
        return obj instanceof String ||
               obj instanceof Number ||
               obj instanceof Boolean ||
               obj instanceof Character ||
               obj instanceof java.util.Date ||
               obj.getClass().isPrimitive();
    }

    /**
     * 执行参数安全检查
     *
     * @param parameterObject 参数对象
     */
    private void performSecurityCheck(Object parameterObject) {
        if (parameterObject == null) {
            return;
        }

        String paramStr = parameterObject.toString().toLowerCase();

        // 检查SQL注入风险关键词
        String[] sqlInjectionKeywords = {
            "drop", "delete", "truncate", "alter", "create",
            "insert", "update", "exec", "execute", "script",
            "union", "select", "from", "where", "--", "/*", "*/"
        };

        for (String keyword : sqlInjectionKeywords) {
            if (paramStr.contains(keyword)) {
                System.out.println("⚠️  安全警告: 参数中包含潜在的SQL注入关键词: " + keyword);
                System.out.println("   参数内容: " + parameterObject.toString());
                break;
            }
        }

        // 检查参数长度
        if (paramStr.length() > 10000) {
            System.out.println("⚠️  安全警告: 参数长度过长(" + paramStr.length() + "字符)，可能存在安全风险");
        }
    }

    /**
     * 设置拦截器属性
     *
     * @param properties 配置属性
     */
    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;

        // 读取配置参数
        String enableDetailLogStr = properties.getProperty("enableDetailLog", "true");
        this.enableDetailLog = Boolean.parseBoolean(enableDetailLogStr);

        String enablePerformanceMonitorStr = properties.getProperty("enablePerformanceMonitor", "true");
        this.enablePerformanceMonitor = Boolean.parseBoolean(enablePerformanceMonitorStr);

        String enableSecurityCheckStr = properties.getProperty("enableSecurityCheck", "true");
        this.enableSecurityCheck = Boolean.parseBoolean(enableSecurityCheckStr);

        String slowParameterThresholdStr = properties.getProperty("slowParameterThreshold", "100");
        try {
            this.slowParameterThreshold = Long.parseLong(slowParameterThresholdStr);
        } catch (NumberFormatException e) {
            System.err.println("警告: slowParameterThreshold配置无效，使用默认值100ms");
            this.slowParameterThreshold = 100L;
        }

        if (enableDetailLog) {
            System.out.println("=== ParameterHandlerInterceptor 配置信息 ===");
            System.out.println("详细日志记录: " + this.enableDetailLog);
            System.out.println("性能监控: " + this.enablePerformanceMonitor);
            System.out.println("安全检查: " + this.enableSecurityCheck);
            System.out.println("慢操作阈值: " + this.slowParameterThreshold + "ms");
            System.out.println("===============================================");
        }
    }

    /**
     * 创建代理对象
     *
     * @param target 目标对象
     * @return 代理对象
     */
    @Override
    public Object plugin(Object target) {
        // 只对ParameterHandler类型的对象进行代理
        if (target instanceof ParameterHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }
}
