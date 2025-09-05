package com.example.demo.interceptor;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

/**
 * MyBatis ResultSetHandler拦截器
 *
 * <p>该拦截器用于拦截MyBatis结果集处理器(ResultSetHandler)的所有核心方法，提供以下功能：</p>
 * <ul>
 *   <li>结果集处理前后的预处理和后处理</li>
 *   <li>结果集映射过程的监控和日志记录</li>
 *   <li>结果集处理异常的处理和记录</li>
 *   <li>结果集处理性能监控</li>
 *   <li>结果数据的统计和分析</li>
 * </ul>
 *
 * <p>拦截的方法包括：</p>
 * <ul>
 *   <li>handleResultSets - 处理查询结果集</li>
 *   <li>handleCursorResultSets - 处理游标结果集</li>
 *   <li>handleOutputParameters - 处理存储过程输出参数</li>
 * </ul>
 */
@Intercepts({
    // 拦截handleResultSets方法 - 处理查询结果集
    @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class}),

    // 拦截handleCursorResultSets方法 - 处理游标结果集
    @Signature(type = ResultSetHandler.class, method = "handleCursorResultSets", args = {Statement.class}),

    // 拦截handleOutputParameters方法 - 处理存储过程输出参数
    @Signature(type = ResultSetHandler.class, method = "handleOutputParameters", args = {CallableStatement.class})
})
public class ResultSetHandlerInterceptor implements Interceptor {

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
     * 是否启用结果统计
     */
    private boolean enableResultStatistics = true;

    /**
     * 结果集处理慢操作阈值(毫秒)
     */
    private long slowResultSetThreshold = 500L;

    /**
     * 大结果集警告阈值(行数)
     */
    private int largeResultSetThreshold = 10000;

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
            System.out.println("=== ResultSetHandler拦截器 - 方法执行开始 ===");
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
            case "handleResultSets":
                handleResultSetsPreProcess(args, target);
                break;
            case "handleCursorResultSets":
                handleCursorResultSetsPreProcess(args, target);
                break;
            case "handleOutputParameters":
                handleOutputParametersPreProcess(args, target);
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
            if (executionTime > slowResultSetThreshold) {
                System.out.println("⚠️  结果集处理慢操作警告: " + methodName + " 执行时间 " + executionTime + "ms 超过阈值 " + slowResultSetThreshold + "ms");
            }
        }

        // 结果统计
        if (enableResultStatistics) {
            performResultStatistics(methodName, result);
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

        System.err.println("❌ ResultSetHandler方法执行异常:");
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
            System.out.println("=== ResultSetHandler拦截器 - 方法执行结束 ===\n");
        }
    }

    // ==================== 各种方法的预处理逻辑 ====================

    /**
     * 处理handleResultSets方法的预处理逻辑
     *
     * @param args 方法参数 [Statement]
     * @param target 目标对象
     */
    private void handleResultSetsPreProcess(Object[] args, Object target) {
        if (enableDetailLog && args.length >= 1) {
            Statement stmt = (Statement) args[0];

            System.out.println("📊 处理查询结果集:");
            System.out.println("  ResultSetHandler类型: " + target.getClass().getSimpleName());
            System.out.println("  Statement类型: " + stmt.getClass().getSimpleName());

            try {
                // 尝试获取Statement的一些信息
                System.out.println("  最大行数限制: " + stmt.getMaxRows());
                System.out.println("  查询超时时间: " + stmt.getQueryTimeout() + "秒");
                System.out.println("  获取方向: " + stmt.getFetchDirection());
                System.out.println("  获取大小: " + stmt.getFetchSize());
            } catch (SQLException e) {
                System.out.println("  无法获取Statement详细信息: " + e.getMessage());
            }
        }
    }

    /**
     * 处理handleCursorResultSets方法的预处理逻辑
     *
     * @param args 方法参数 [Statement]
     * @param target 目标对象
     */
    private void handleCursorResultSetsPreProcess(Object[] args, Object target) {
        if (enableDetailLog && args.length >= 1) {
            Statement stmt = (Statement) args[0];

            System.out.println("📄 处理游标结果集:");
            System.out.println("  ResultSetHandler类型: " + target.getClass().getSimpleName());
            System.out.println("  Statement类型: " + stmt.getClass().getSimpleName());

            try {
                // 尝试获取Statement的一些信息
                System.out.println("  结果集类型: " + stmt.getResultSetType());
                System.out.println("  结果集并发性: " + stmt.getResultSetConcurrency());
                System.out.println("  结果集可保持性: " + stmt.getResultSetHoldability());
            } catch (SQLException e) {
                System.out.println("  无法获取Statement详细信息: " + e.getMessage());
            }
        }
    }

    /**
     * 处理handleOutputParameters方法的预处理逻辑
     *
     * @param args 方法参数 [CallableStatement]
     * @param target 目标对象
     */
    private void handleOutputParametersPreProcess(Object[] args, Object target) {
        if (enableDetailLog && args.length >= 1) {
            CallableStatement cs = (CallableStatement) args[0];

            System.out.println("🔧 处理存储过程输出参数:");
            System.out.println("  ResultSetHandler类型: " + target.getClass().getSimpleName());
            System.out.println("  CallableStatement类型: " + cs.getClass().getSimpleName());

            try {
                // 尝试获取CallableStatement的一些信息
                System.out.println("  最大行数限制: " + cs.getMaxRows());
                System.out.println("  查询超时时间: " + cs.getQueryTimeout() + "秒");
            } catch (SQLException e) {
                System.out.println("  无法获取CallableStatement详细信息: " + e.getMessage());
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
            case "handleResultSets":
                if (result instanceof List) {
                    List<?> resultList = (List<?>) result;
                    System.out.println("  结果集处理完成:");
                    System.out.println("    结果数量: " + resultList.size());

                    if (!resultList.isEmpty()) {
                        Object firstResult = resultList.get(0);
                        System.out.println("    结果类型: " + firstResult.getClass().getSimpleName());

                        // 显示第一个结果的示例
                        if (enableDetailLog) {
                            System.out.println("    第一个结果示例: " + firstResult.toString());
                        }
                    }
                } else {
                    System.out.println("  结果集处理完成: " + (result != null ? result.toString() : "null"));
                }
                break;
            case "handleCursorResultSets":
                if (result instanceof Cursor) {
                    System.out.println("  游标结果集处理完成:");
                    System.out.println("    游标类型: " + result.getClass().getSimpleName());
                } else {
                    System.out.println("  游标结果集处理完成: " + (result != null ? result.toString() : "null"));
                }
                break;
            case "handleOutputParameters":
                System.out.println("  输出参数处理完成");
                break;
            default:
                if (result != null) {
                    System.out.println("  返回值: " + result.toString());
                }
                break;
        }
    }

    /**
     * 执行结果统计
     *
     * @param methodName 方法名
     * @param result 执行结果
     */
    private void performResultStatistics(String methodName, Object result) {
        switch (methodName) {
            case "handleResultSets":
                if (result instanceof List) {
                    List<?> resultList = (List<?>) result;
                    int resultCount = resultList.size();

                    // 大结果集警告
                    if (resultCount > largeResultSetThreshold) {
                        System.out.println("⚠️  大结果集警告: 查询返回 " + resultCount + " 行数据，超过阈值 " + largeResultSetThreshold + " 行");
                        System.out.println("   建议: 考虑使用分页查询或优化查询条件");
                    }

                    // 结果集统计信息
                    System.out.println("📈 结果集统计:");
                    System.out.println("  总行数: " + resultCount);

                    if (resultCount > 0) {
                        // 分析结果类型分布
                        analyzeResultTypes(resultList);
                    }
                }
                break;
            case "handleCursorResultSets":
                System.out.println("📈 游标结果集统计: 游标对象已创建");
                break;
            case "handleOutputParameters":
                System.out.println("📈 输出参数统计: 输出参数处理完成");
                break;
        }
    }

    /**
     * 分析结果类型分布
     *
     * @param resultList 结果列表
     */
    private void analyzeResultTypes(List<?> resultList) {
        if (resultList.isEmpty()) {
            return;
        }

        try {
            // 统计不同类型的数量
            java.util.Map<String, Integer> typeCount = new java.util.HashMap<>();

            for (Object item : resultList) {
                if (item != null) {
                    String typeName = item.getClass().getSimpleName();
                    typeCount.put(typeName, typeCount.getOrDefault(typeName, 0) + 1);
                } else {
                    typeCount.put("null", typeCount.getOrDefault("null", 0) + 1);
                }
            }

            System.out.println("  类型分布:");
            for (java.util.Map.Entry<String, Integer> entry : typeCount.entrySet()) {
                System.out.println("    " + entry.getKey() + ": " + entry.getValue() + " 个");
            }

        } catch (Exception e) {
            System.out.println("  类型分析失败: " + e.getMessage());
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

        String enableResultStatisticsStr = properties.getProperty("enableResultStatistics", "true");
        this.enableResultStatistics = Boolean.parseBoolean(enableResultStatisticsStr);

        String slowResultSetThresholdStr = properties.getProperty("slowResultSetThreshold", "500");
        try {
            this.slowResultSetThreshold = Long.parseLong(slowResultSetThresholdStr);
        } catch (NumberFormatException e) {
            System.err.println("警告: slowResultSetThreshold配置无效，使用默认值500ms");
            this.slowResultSetThreshold = 500L;
        }

        String largeResultSetThresholdStr = properties.getProperty("largeResultSetThreshold", "10000");
        try {
            this.largeResultSetThreshold = Integer.parseInt(largeResultSetThresholdStr);
        } catch (NumberFormatException e) {
            System.err.println("警告: largeResultSetThreshold配置无效，使用默认值10000行");
            this.largeResultSetThreshold = 10000;
        }

        if (enableDetailLog) {
            System.out.println("=== ResultSetHandlerInterceptor 配置信息 ===");
            System.out.println("详细日志记录: " + this.enableDetailLog);
            System.out.println("性能监控: " + this.enablePerformanceMonitor);
            System.out.println("结果统计: " + this.enableResultStatistics);
            System.out.println("慢操作阈值: " + this.slowResultSetThreshold + "ms");
            System.out.println("大结果集阈值: " + this.largeResultSetThreshold + "行");
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
        // 只对ResultSetHandler类型的对象进行代理
        if (target instanceof ResultSetHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }
}
