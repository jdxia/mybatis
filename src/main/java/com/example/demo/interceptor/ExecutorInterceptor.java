package com.example.demo.interceptor;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * MyBatis Executor拦截器
 *
 * <p>该拦截器用于拦截MyBatis执行器(Executor)的所有核心方法，提供以下功能：</p>
 * <ul>
 *   <li>SQL执行前后的预处理和后处理</li>
 *   <li>执行时间统计和性能监控</li>
 *   <li>异常处理和错误日志记录</li>
 *   <li>事务管理监控</li>
 *   <li>缓存操作监控</li>
 * </ul>
 *
 * <p>拦截的方法包括：</p>
 * <ul>
 *   <li>update - 执行增删改操作</li>
 *   <li>query - 执行查询操作(带缓存)</li>
 *   <li>query - 执行查询操作(不带缓存)</li>
 *   <li>queryCursor - 执行游标查询</li>
 *   <li>flushStatements - 刷新批处理语句</li>
 *   <li>commit - 提交事务</li>
 *   <li>rollback - 回滚事务</li>
 *   <li>getTransaction - 获取事务对象</li>
 *   <li>close - 关闭执行器</li>
 *   <li>isClosed - 检查执行器是否已关闭</li>
 * </ul>
 */
@Intercepts({
    // 拦截update方法 - 用于增删改操作
    @Signature(type = Executor.class, method = "update",
               args = {MappedStatement.class, Object.class}),

    // 拦截query方法 - 带缓存的查询操作
    @Signature(type = Executor.class, method = "query",
               args = {MappedStatement.class, Object.class, RowBounds.class,
                      ResultHandler.class, CacheKey.class, BoundSql.class}),

    // 拦截query方法 - 不带缓存的查询操作
    @Signature(type = Executor.class, method = "query",
               args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),

    // 拦截queryCursor方法 - 游标查询操作
    @Signature(type = Executor.class, method = "queryCursor",
               args = {MappedStatement.class, Object.class, RowBounds.class}),

    // 拦截flushStatements方法 - 刷新批处理语句
    @Signature(type = Executor.class, method = "flushStatements", args = {}),

    // 拦截commit方法 - 事务提交
    @Signature(type = Executor.class, method = "commit", args = {boolean.class}),

    // 拦截rollback方法 - 事务回滚
    @Signature(type = Executor.class, method = "rollback", args = {boolean.class}),

    // 拦截getTransaction方法 - 获取事务对象
    @Signature(type = Executor.class, method = "getTransaction", args = {}),

    // 拦截close方法 - 关闭执行器
    @Signature(type = Executor.class, method = "close", args = {boolean.class}),

    // 拦截isClosed方法 - 检查执行器状态
    @Signature(type = Executor.class, method = "isClosed", args = {})
})
public class ExecutorInterceptor implements Interceptor {

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
     * 慢查询阈值(毫秒)
     */
    private long slowQueryThreshold = 1000L;

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
            System.out.println("=== Executor拦截器 - 方法执行开始 ===");
            System.out.println("目标对象: " + target.getClass().getSimpleName());
            System.out.println("执行方法: " + methodName);
            System.out.println("开始时间: " + new java.util.Date(startTime));
        }

        // 根据不同的方法执行不同的预处理逻辑
        preProcess(methodName, args);

        Object result = null;
        Throwable exception = null;

        try {
            // 执行原始方法
            result = invocation.proceed();

            // 方法执行成功后的处理
            postProcessSuccess(methodName, args, result, startTime);

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
     */
    private void preProcess(String methodName, Object[] args) {
        switch (methodName) {
            case "update":
                handleUpdatePreProcess(args);
                break;
            case "query":
                handleQueryPreProcess(args);
                break;
            case "queryCursor":
                handleQueryCursorPreProcess(args);
                break;
            case "flushStatements":
                handleFlushStatementsPreProcess();
                break;
            case "commit":
                handleCommitPreProcess(args);
                break;
            case "rollback":
                handleRollbackPreProcess(args);
                break;
            case "getTransaction":
                handleGetTransactionPreProcess();
                break;
            case "close":
                handleClosePreProcess(args);
                break;
            case "isClosed":
                handleIsClosedPreProcess();
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
     */
    private void postProcessSuccess(String methodName, Object[] args, Object result, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;

        if (enableDetailLog) {
            System.out.println("--- 方法执行成功 ---");
            System.out.println("执行结果类型: " + (result != null ? result.getClass().getSimpleName() : "null"));

            // 根据不同方法显示特定的结果信息
            displayMethodResult(methodName, result);
        }

        // 性能监控
        if (enablePerformanceMonitor) {
            if (executionTime > slowQueryThreshold) {
                System.out.println("⚠️  慢操作警告: " + methodName + " 执行时间 " + executionTime + "ms 超过阈值 " + slowQueryThreshold + "ms");
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

        System.err.println("❌ Executor方法执行异常:");
        System.err.println("方法名: " + methodName);
        System.err.println("执行时间: " + executionTime + "ms");
        System.err.println("异常类型: " + exception.getClass().getSimpleName());
        System.err.println("异常信息: " + exception.getMessage());

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
            System.out.println("=== Executor拦截器 - 方法执行结束 ===\n");
        }
    }

    // ==================== 各种方法的预处理逻辑 ====================

    /**
     * 处理update方法的预处理逻辑
     *
     * @param args 方法参数 [MappedStatement, Object]
     */
    private void handleUpdatePreProcess(Object[] args) {
        if (enableDetailLog && args.length >= 2) {
            MappedStatement ms = (MappedStatement) args[0];
            Object parameter = args[1];

            System.out.println("📝 执行更新操作:");
            System.out.println("  SQL ID: " + ms.getId());
            System.out.println("  SQL类型: " + ms.getSqlCommandType());
            System.out.println("  参数对象: " + (parameter != null ? parameter.getClass().getSimpleName() : "null"));

            // 显示绑定的SQL语句
            BoundSql boundSql = ms.getBoundSql(parameter);
            if (boundSql != null) {
                System.out.println("  SQL语句: " + boundSql.getSql().replaceAll("\\s+", " ").trim());
            }
        }
    }

    /**
     * 处理query方法的预处理逻辑
     *
     * @param args 方法参数 [MappedStatement, Object, RowBounds, ResultHandler] 或
     *             [MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql]
     */
    private void handleQueryPreProcess(Object[] args) {
        if (enableDetailLog && args.length >= 4) {
            MappedStatement ms = (MappedStatement) args[0];
            Object parameter = args[1];
            RowBounds rowBounds = (RowBounds) args[2];
            ResultHandler resultHandler = (ResultHandler) args[3];

            System.out.println("🔍 执行查询操作:");
            System.out.println("  SQL ID: " + ms.getId());
            System.out.println("  参数对象: " + (parameter != null ? parameter.getClass().getSimpleName() : "null"));
            System.out.println("  分页信息: offset=" + rowBounds.getOffset() + ", limit=" + rowBounds.getLimit());
            System.out.println("  结果处理器: " + (resultHandler != null ? "自定义" : "默认"));

            // 如果是带缓存的查询，显示缓存信息
            if (args.length >= 6) {
                CacheKey cacheKey = (CacheKey) args[4];
                System.out.println("  缓存键: " + (cacheKey != null ? cacheKey.toString() : "null"));
                System.out.println("  查询类型: 带缓存查询");
            } else {
                System.out.println("  查询类型: 普通查询");
            }

            // 显示绑定的SQL语句
            BoundSql boundSql = ms.getBoundSql(parameter);
            if (boundSql != null) {
                System.out.println("  SQL语句: " + boundSql.getSql().replaceAll("\\s+", " ").trim());
            }
        }
    }

    /**
     * 处理queryCursor方法的预处理逻辑
     *
     * @param args 方法参数 [MappedStatement, Object, RowBounds]
     */
    private void handleQueryCursorPreProcess(Object[] args) {
        if (enableDetailLog && args.length >= 3) {
            MappedStatement ms = (MappedStatement) args[0];
            Object parameter = args[1];
            RowBounds rowBounds = (RowBounds) args[2];

            System.out.println("📄 执行游标查询:");
            System.out.println("  SQL ID: " + ms.getId());
            System.out.println("  参数对象: " + (parameter != null ? parameter.getClass().getSimpleName() : "null"));
            System.out.println("  分页信息: offset=" + rowBounds.getOffset() + ", limit=" + rowBounds.getLimit());

            // 显示绑定的SQL语句
            BoundSql boundSql = ms.getBoundSql(parameter);
            if (boundSql != null) {
                System.out.println("  SQL语句: " + boundSql.getSql().replaceAll("\\s+", " ").trim());
            }
        }
    }

    /**
     * 处理flushStatements方法的预处理逻辑
     */
    private void handleFlushStatementsPreProcess() {
        if (enableDetailLog) {
            System.out.println("🔄 刷新批处理语句");
        }
    }

    /**
     * 处理commit方法的预处理逻辑
     *
     * @param args 方法参数 [boolean]
     */
    private void handleCommitPreProcess(Object[] args) {
        if (enableDetailLog && args.length >= 1) {
            boolean required = (Boolean) args[0];
            System.out.println("✅ 提交事务:");
            System.out.println("  是否必须提交: " + required);
        }
    }

    /**
     * 处理rollback方法的预处理逻辑
     *
     * @param args 方法参数 [boolean]
     */
    private void handleRollbackPreProcess(Object[] args) {
        if (enableDetailLog && args.length >= 1) {
            boolean required = (Boolean) args[0];
            System.out.println("❌ 回滚事务:");
            System.out.println("  是否必须回滚: " + required);
        }
    }

    /**
     * 处理getTransaction方法的预处理逻辑
     */
    private void handleGetTransactionPreProcess() {
        if (enableDetailLog) {
            System.out.println("🔗 获取事务对象");
        }
    }

    /**
     * 处理close方法的预处理逻辑
     *
     * @param args 方法参数 [boolean]
     */
    private void handleClosePreProcess(Object[] args) {
        if (enableDetailLog && args.length >= 1) {
            boolean forceRollback = (Boolean) args[0];
            System.out.println("🔒 关闭执行器:");
            System.out.println("  强制回滚: " + forceRollback);
        }
    }

    /**
     * 处理isClosed方法的预处理逻辑
     */
    private void handleIsClosedPreProcess() {
        if (enableDetailLog) {
            System.out.println("❓ 检查执行器状态");
        }
    }

    // ==================== 显示方法执行结果 ====================

    /**
     * 根据不同方法显示特定的结果信息
     *
     * @param methodName 方法名
     * @param result 执行结果
     */
    private void displayMethodResult(String methodName, Object result) {
        switch (methodName) {
            case "update":
                if (result instanceof Integer) {
                    System.out.println("  影响行数: " + result);
                }
                break;
            case "query":
                if (result instanceof List) {
                    List<?> list = (List<?>) result;
                    System.out.println("  查询结果数量: " + list.size());
                    if (!list.isEmpty()) {
                        System.out.println("  结果类型: " + list.get(0).getClass().getSimpleName());
                    }
                }
                break;
            case "queryCursor":
                if (result instanceof Cursor) {
                    System.out.println("  游标对象: " + result.getClass().getSimpleName());
                }
                break;
            case "flushStatements":
                if (result instanceof List) {
                    List<?> batchResults = (List<?>) result;
                    System.out.println("  批处理结果数量: " + batchResults.size());
                }
                break;
            case "getTransaction":
                if (result instanceof Transaction) {
                    Transaction transaction = (Transaction) result;
                    System.out.println("  事务对象: " + transaction.getClass().getSimpleName());
                }
                break;
            case "isClosed":
                if (result instanceof Boolean) {
                    System.out.println("  执行器状态: " + (((Boolean) result) ? "已关闭" : "运行中"));
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

        String slowQueryThresholdStr = properties.getProperty("slowQueryThreshold", "1000");
        try {
            this.slowQueryThreshold = Long.parseLong(slowQueryThresholdStr);
        } catch (NumberFormatException e) {
            System.err.println("警告: slowQueryThreshold配置无效，使用默认值1000ms");
            this.slowQueryThreshold = 1000L;
        }

        if (enableDetailLog) {
            System.out.println("=== ExecutorInterceptor 配置信息 ===");
            System.out.println("详细日志记录: " + this.enableDetailLog);
            System.out.println("性能监控: " + this.enablePerformanceMonitor);
            System.out.println("慢查询阈值: " + this.slowQueryThreshold + "ms");
            System.out.println("=====================================");
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
        // 只对Executor类型的对象进行代理
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        }
        return target;
    }
}
