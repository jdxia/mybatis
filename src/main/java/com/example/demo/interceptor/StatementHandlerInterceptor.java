package com.example.demo.interceptor;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * MyBatis StatementHandler拦截器
 *
 * <p>该拦截器用于拦截MyBatis语句处理器(StatementHandler)的所有核心方法，提供以下功能：</p>
 * <ul>
 *   <li>SQL语句准备和执行过程的监控</li>
 *   <li>参数设置和SQL执行的日志记录</li>
 *   <li>SQL执行异常的处理和记录</li>
 *   <li>SQL执行性能监控和分析</li>
 *   <li>SQL安全性检查和优化建议</li>
 * </ul>
 *
 * <p>拦截的方法包括：</p>
 * <ul>
 *   <li>prepare - 准备SQL语句</li>
 *   <li>parameterize - 设置SQL参数</li>
 *   <li>batch - 批处理操作</li>
 *   <li>update - 执行更新操作</li>
 *   <li>query - 执行查询操作</li>
 *   <li>queryCursor - 执行游标查询</li>
 *   <li>getBoundSql - 获取绑定SQL</li>
 *   <li>getParameterHandler - 获取参数处理器</li>
 * </ul>
 */
@Intercepts({
  // 拦截prepare方法 - 准备SQL语句
  @Signature(type = StatementHandler.class, method = "prepare",
    args = {Connection.class, Integer.class}),

  // 拦截parameterize方法 - 设置SQL参数
  @Signature(type = StatementHandler.class, method = "parameterize", args = {Statement.class}),

  // 拦截batch方法 - 批处理操作
  @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class}),

  // 拦截update方法 - 执行更新操作
  @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),

  // 拦截query方法 - 执行查询操作
  @Signature(type = StatementHandler.class, method = "query",
    args = {Statement.class, ResultHandler.class}),

  // 拦截queryCursor方法 - 执行游标查询
  @Signature(type = StatementHandler.class, method = "queryCursor", args = {Statement.class}),

  // 拦截getBoundSql方法 - 获取绑定SQL
  @Signature(type = StatementHandler.class, method = "getBoundSql", args = {}),

  // 拦截getParameterHandler方法 - 获取参数处理器
  @Signature(type = StatementHandler.class, method = "getParameterHandler", args = {})
})
public class StatementHandlerInterceptor implements Interceptor {

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
   * 是否启用SQL安全检查
   */
  private boolean enableSqlSecurityCheck = true;

  /**
   * 是否启用SQL优化建议
   */
  private boolean enableSqlOptimizationAdvice = true;

  /**
   * SQL执行慢操作阈值(毫秒)
   */
  private long slowSqlThreshold = 1000L;

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

    // 1) 取真实的目标对象（如用了 MyBatis-Plus，可用其工具拿 real target）
    StatementHandler sh = (StatementHandler) invocation.getTarget();
    MetaObject meta = SystemMetaObject.forObject(sh);

    // 2) 取 BoundSql（拿 SQL 文本），和 MappedStatement（拿 statementId）

    /**
     * 报错信息：There is no getter for property named 'delegate' in 'class com.sun.proxy.$Proxy211'
     * 这种是多个插件之间有先后顺序依赖，别的插件先行执行，影响了delegate的获取，调整 SQLMarking Plugin 的位置，向上或向下调整，可解决冲突。
     */

    BoundSql boundSql = (BoundSql) meta.getValue("delegate.boundSql");
    MappedStatement ms = (MappedStatement) meta.getValue("delegate.mappedStatement");

    // select * from tbl_department where id = ?
    String sql = boundSql.getSql();         // 原始 SQL（带 ?）
    // com.example.demo.mapper.DepartmentMapper.findById
    String statementId = ms.getId();        // mapperNamespace + sqlId

    // 3) 组装“染色注释”，也可以带上 ThreadLocal 的自定义信息
    String traceId = getTraceId();              // 你自己的生成逻辑
    Map<String, String> extras = SqlMarkingThreadLocal.get(); // 你自己维护
    String comment = buildMark(statementId, traceId, extras);

    // 4) 将注释追加到 SQL（通常放在末尾更稳妥）
    String newSql = sql + " /* [SQLMarking] " + comment + " */";

    // 5) 反射回写到 BoundSql.sql 字段
    Field sqlField = BoundSql.class.getDeclaredField("sql");
    sqlField.setAccessible(true);
    sqlField.set(boundSql, newSql);


    // 记录开始时间用于性能监控
    long startTime = System.currentTimeMillis();

    if (enableDetailLog) {
      System.out.println("=== StatementHandler拦截器 - 方法执行开始 ===");
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
   * @param args       方法参数
   * @param target     目标对象
   */
  private void preProcess(String methodName, Object[] args, Object target) {
    switch (methodName) {
      case "prepare":
        handlePreparePreProcess(args, target);
        break;
      case "parameterize":
        handleParameterizePreProcess(args, target);
        break;
      case "batch":
        handleBatchPreProcess(args, target);
        break;
      case "update":
        handleUpdatePreProcess(args, target);
        break;
      case "query":
        handleQueryPreProcess(args, target);
        break;
      case "queryCursor":
        handleQueryCursorPreProcess(args, target);
        break;
      case "getBoundSql":
        handleGetBoundSqlPreProcess(target);
        break;
      case "getParameterHandler":
        handleGetParameterHandlerPreProcess(target);
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
   * @param args       方法参数
   * @param result     执行结果
   * @param startTime  开始时间
   * @param target     目标对象
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
      if (executionTime > slowSqlThreshold) {
        System.out.println("⚠️  SQL执行慢操作警告: " + methodName + " 执行时间 " + executionTime + "ms 超过阈值 " + slowSqlThreshold + "ms");

        // 提供优化建议
        if (enableSqlOptimizationAdvice) {
          provideSqlOptimizationAdvice(methodName, target);
        }
      }
    }
  }

  /**
   * 方法执行异常时的处理逻辑
   *
   * @param methodName 方法名
   * @param args       方法参数
   * @param exception  异常信息
   * @param startTime  开始时间
   */
  private void postProcessException(String methodName, Object[] args, Throwable exception, long startTime) {
    long executionTime = System.currentTimeMillis() - startTime;

    System.err.println("❌ StatementHandler方法执行异常:");
    System.err.println("方法名: " + methodName);
    System.err.println("执行时间: " + executionTime + "ms");
    System.err.println("异常类型: " + exception.getClass().getSimpleName());
    System.err.println("异常信息: " + exception.getMessage());

    // 特殊处理SQL异常
    if (exception instanceof SQLException) {
      SQLException sqlException = (SQLException) exception;
      System.err.println("SQL错误代码: " + sqlException.getErrorCode());
      System.err.println("SQL状态: " + sqlException.getSQLState());

      // 提供SQL错误的解决建议
      provideSqlErrorAdvice(sqlException);
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
   * @param args       方法参数
   * @param result     执行结果
   * @param exception  异常信息
   * @param startTime  开始时间
   */
  private void finalProcess(String methodName, Object[] args, Object result, Throwable exception, long startTime) {
    long executionTime = System.currentTimeMillis() - startTime;

    if (enableDetailLog) {
      System.out.println("--- 方法执行完成 ---");
      System.out.println("总执行时间: " + executionTime + "ms");
      System.out.println("执行状态: " + (exception == null ? "成功" : "异常"));
      System.out.println("=== StatementHandler拦截器 - 方法执行结束 ===\n");
    }
  }

  // ==================== 各种方法的预处理逻辑 ====================

  /**
   * 处理prepare方法的预处理逻辑
   *
   * @param args   方法参数 [Connection, Integer]
   * @param target 目标对象
   */
  private void handlePreparePreProcess(Object[] args, Object target) {
    if (enableDetailLog && args.length >= 2) {
      Connection connection = (Connection) args[0];
      Integer transactionTimeout = (Integer) args[1];

      System.out.println("🔧 准备SQL语句:");
      System.out.println("  StatementHandler类型: " + target.getClass().getSimpleName());
      System.out.println("  事务超时时间: " + transactionTimeout + "秒");

      try {
        System.out.println("  数据库连接: " + connection.getClass().getSimpleName());
        System.out.println("  自动提交: " + connection.getAutoCommit());
        System.out.println("  事务隔离级别: " + connection.getTransactionIsolation());
      } catch (SQLException e) {
        System.out.println("  无法获取连接详细信息: " + e.getMessage());
      }

      // 获取并显示SQL语句
      if (target instanceof StatementHandler) {
        try {
          StatementHandler statementHandler = (StatementHandler) target;
          BoundSql boundSql = statementHandler.getBoundSql();
          if (boundSql != null) {
            String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
            System.out.println("  SQL语句: " + sql);

            // 执行SQL安全检查
            if (enableSqlSecurityCheck) {
              performSqlSecurityCheck(sql);
            }
          }
        } catch (Exception e) {
          System.out.println("  无法获取SQL语句: " + e.getMessage());
        }
      }
    }
  }

  /**
   * 处理parameterize方法的预处理逻辑
   *
   * @param args   方法参数 [Statement]
   * @param target 目标对象
   */
  private void handleParameterizePreProcess(Object[] args, Object target) {
    if (enableDetailLog && args.length >= 1) {
      Statement statement = (Statement) args[0];

      System.out.println("⚙️  设置SQL参数:");
      System.out.println("  StatementHandler类型: " + target.getClass().getSimpleName());
      System.out.println("  Statement类型: " + statement.getClass().getSimpleName());

      // 获取参数处理器信息
      if (target instanceof StatementHandler) {
        try {
          StatementHandler statementHandler = (StatementHandler) target;
          ParameterHandler parameterHandler = statementHandler.getParameterHandler();
          if (parameterHandler != null) {
            System.out.println("  ParameterHandler类型: " + parameterHandler.getClass().getSimpleName());
            Object parameterObject = parameterHandler.getParameterObject();
            if (parameterObject != null) {
              System.out.println("  参数对象: " + parameterObject.getClass().getSimpleName());
              System.out.println("  参数值: " + parameterObject.toString());
            }
          }
        } catch (Exception e) {
          System.out.println("  无法获取参数详细信息: " + e.getMessage());
        }
      }
    }
  }

  /**
   * 处理batch方法的预处理逻辑
   *
   * @param args   方法参数 [Statement]
   * @param target 目标对象
   */
  private void handleBatchPreProcess(Object[] args, Object target) {
    if (enableDetailLog && args.length >= 1) {
      Statement statement = (Statement) args[0];

      System.out.println("📦 执行批处理操作:");
      System.out.println("  StatementHandler类型: " + target.getClass().getSimpleName());
      System.out.println("  Statement类型: " + statement.getClass().getSimpleName());
    }
  }

  /**
   * 处理update方法的预处理逻辑
   *
   * @param args   方法参数 [Statement]
   * @param target 目标对象
   */
  private void handleUpdatePreProcess(Object[] args, Object target) {
    if (enableDetailLog && args.length >= 1) {
      Statement statement = (Statement) args[0];

      System.out.println("📝 执行更新操作:");
      System.out.println("  StatementHandler类型: " + target.getClass().getSimpleName());
      System.out.println("  Statement类型: " + statement.getClass().getSimpleName());

      // 显示即将执行的SQL
      displaySqlInfo(target, "更新");
    }
  }

  /**
   * 处理query方法的预处理逻辑
   *
   * @param args   方法参数 [Statement, ResultHandler]
   * @param target 目标对象
   */
  private void handleQueryPreProcess(Object[] args, Object target) {
    if (enableDetailLog && args.length >= 2) {
      Statement statement = (Statement) args[0];
      ResultHandler resultHandler = (ResultHandler) args[1];

      System.out.println("🔍 执行查询操作:");
      System.out.println("  StatementHandler类型: " + target.getClass().getSimpleName());
      System.out.println("  Statement类型: " + statement.getClass().getSimpleName());
      System.out.println("  ResultHandler: " + (resultHandler != null ? "自定义" : "默认"));

      // 显示即将执行的SQL
      displaySqlInfo(target, "查询");
    }
  }

  /**
   * 处理queryCursor方法的预处理逻辑
   *
   * @param args   方法参数 [Statement]
   * @param target 目标对象
   */
  private void handleQueryCursorPreProcess(Object[] args, Object target) {
    if (enableDetailLog && args.length >= 1) {
      Statement statement = (Statement) args[0];

      System.out.println("📄 执行游标查询:");
      System.out.println("  StatementHandler类型: " + target.getClass().getSimpleName());
      System.out.println("  Statement类型: " + statement.getClass().getSimpleName());

      // 显示即将执行的SQL
      displaySqlInfo(target, "游标查询");
    }
  }

  /**
   * 处理getBoundSql方法的预处理逻辑
   *
   * @param target 目标对象
   */
  private void handleGetBoundSqlPreProcess(Object target) {
    if (enableDetailLog) {
      System.out.println("📋 获取绑定SQL:");
      System.out.println("  StatementHandler类型: " + target.getClass().getSimpleName());
    }
  }

  /**
   * 处理getParameterHandler方法的预处理逻辑
   *
   * @param target 目标对象
   */
  private void handleGetParameterHandlerPreProcess(Object target) {
    if (enableDetailLog) {
      System.out.println("🔗 获取参数处理器:");
      System.out.println("  StatementHandler类型: " + target.getClass().getSimpleName());
    }
  }

  // ==================== 辅助方法 ====================

  /**
   * 显示SQL信息
   *
   * @param target        目标对象
   * @param operationType 操作类型
   */
  private void displaySqlInfo(Object target, String operationType) {
    if (target instanceof StatementHandler) {
      try {
        StatementHandler statementHandler = (StatementHandler) target;
        BoundSql boundSql = statementHandler.getBoundSql();
        if (boundSql != null) {
          String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
          System.out.println("  " + operationType + "SQL: " + sql);

          // 显示参数信息
          Object parameterObject = boundSql.getParameterObject();
          if (parameterObject != null) {
            System.out.println("  参数对象: " + parameterObject.toString());
          }
        }
      } catch (Exception e) {
        System.out.println("  无法获取SQL信息: " + e.getMessage());
      }
    }
  }

  /**
   * 根据不同方法显示特定的结果信息
   *
   * @param methodName 方法名
   * @param result     执行结果
   * @param target     目标对象
   */
  private void displayMethodResult(String methodName, Object result, Object target) {
    switch (methodName) {
      case "prepare":
        if (result instanceof Statement) {
          Statement stmt = (Statement) result;
          System.out.println("  SQL语句准备完成:");
          System.out.println("    Statement类型: " + stmt.getClass().getSimpleName());
        }
        break;
      case "parameterize":
        System.out.println("  参数设置完成");
        break;
      case "batch":
        System.out.println("  批处理添加完成");
        break;
      case "update":
        if (result instanceof Integer) {
          System.out.println("  更新操作完成:");
          System.out.println("    影响行数: " + result);
        }
        break;
      case "query":
        if (result instanceof List) {
          List<?> resultList = (List<?>) result;
          System.out.println("  查询操作完成:");
          System.out.println("    结果数量: " + resultList.size());
          if (!resultList.isEmpty()) {
            System.out.println("    结果类型: " + resultList.get(0).getClass().getSimpleName());
          }
        }
        break;
      case "queryCursor":
        if (result instanceof Cursor) {
          System.out.println("  游标查询完成:");
          System.out.println("    游标类型: " + result.getClass().getSimpleName());
        }
        break;
      case "getBoundSql":
        if (result instanceof BoundSql) {
          BoundSql boundSql = (BoundSql) result;
          System.out.println("  绑定SQL获取完成:");
          System.out.println("    SQL语句: " + boundSql.getSql().replaceAll("\\s+", " ").trim());
        }
        break;
      case "getParameterHandler":
        if (result instanceof ParameterHandler) {
          System.out.println("  参数处理器获取完成:");
          System.out.println("    ParameterHandler类型: " + result.getClass().getSimpleName());
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
   * 执行SQL安全检查
   *
   * @param sql SQL语句
   */
  private void performSqlSecurityCheck(String sql) {
    if (sql == null || sql.trim().isEmpty()) {
      return;
    }

    String lowerSql = sql.toLowerCase().trim();

    // 检查危险的SQL操作
    String[] dangerousOperations = {
      "drop table", "drop database", "truncate table", "delete from",
      "alter table", "create table", "create database", "grant", "revoke"
    };

    for (String operation : dangerousOperations) {
      if (lowerSql.contains(operation)) {
        System.out.println("⚠️  SQL安全警告: 检测到潜在危险操作: " + operation);
        System.out.println("   SQL语句: " + sql);
        break;
      }
    }

    // 检查SQL注入风险
    String[] injectionPatterns = {
      "union select", "or 1=1", "and 1=1", "' or '", "\" or \"",
      "exec(", "execute(", "sp_", "xp_", "/*", "--"
    };

    for (String pattern : injectionPatterns) {
      if (lowerSql.contains(pattern)) {
        System.out.println("⚠️  SQL注入风险警告: 检测到可疑模式: " + pattern);
        System.out.println("   SQL语句: " + sql);
        break;
      }
    }

    // 检查SQL长度
    if (sql.length() > 10000) {
      System.out.println("⚠️  SQL长度警告: SQL语句过长(" + sql.length() + "字符)，可能存在性能问题");
    }
  }

  /**
   * 提供SQL优化建议
   *
   * @param methodName 方法名
   * @param target     目标对象
   */
  private void provideSqlOptimizationAdvice(String methodName, Object target) {
    System.out.println("💡 SQL优化建议:");

    if (target instanceof StatementHandler) {
      try {
        StatementHandler statementHandler = (StatementHandler) target;
        BoundSql boundSql = statementHandler.getBoundSql();
        if (boundSql != null) {
          String sql = boundSql.getSql().toLowerCase().trim();

          // 基于SQL内容提供建议
          if (sql.contains("select *")) {
            System.out.println("  - 避免使用SELECT *，明确指定需要的列");
          }

          if (sql.contains("like '%") && sql.contains("%'")) {
            System.out.println("  - 避免使用前导通配符的LIKE查询，考虑使用全文索引");
          }

          if (sql.contains("order by") && !sql.contains("limit")) {
            System.out.println("  - ORDER BY查询建议添加LIMIT限制结果数量");
          }

          if (sql.contains("in (") && sql.split("in \\(")[1].split("\\)")[0].split(",").length > 1000) {
            System.out.println("  - IN子句包含过多值，考虑使用临时表或EXISTS");
          }

          if (!sql.contains("where") && (sql.contains("update") || sql.contains("delete"))) {
            System.out.println("  - UPDATE/DELETE操作缺少WHERE条件，请确认是否需要");
          }
        }
      } catch (Exception e) {
        System.out.println("  - 无法分析SQL语句进行优化建议");
      }
    }

    // 基于方法类型提供建议
    switch (methodName) {
      case "query":
        System.out.println("  - 考虑使用分页查询避免大结果集");
        System.out.println("  - 确保查询条件字段上有适当的索引");
        break;
      case "update":
        System.out.println("  - 确保WHERE条件使用索引字段");
        System.out.println("  - 考虑批量更新减少数据库交互");
        break;
    }
  }

  /**
   * 提供SQL错误的解决建议
   *
   * @param sqlException SQL异常
   */
  private void provideSqlErrorAdvice(SQLException sqlException) {
    int errorCode = sqlException.getErrorCode();
    String sqlState = sqlException.getSQLState();

    System.err.println("💡 SQL错误解决建议:");

    // 基于错误代码提供建议
    switch (errorCode) {
      case 1062: // MySQL: Duplicate entry
        System.err.println("  - 重复键错误：检查唯一约束或主键冲突");
        break;
      case 1146: // MySQL: Table doesn't exist
        System.err.println("  - 表不存在：检查表名拼写或确认表已创建");
        break;
      case 1054: // MySQL: Unknown column
        System.err.println("  - 未知列：检查列名拼写或确认列存在");
        break;
      case 1064: // MySQL: SQL syntax error
        System.err.println("  - SQL语法错误：检查SQL语句语法");
        break;
      case 1045: // MySQL: Access denied
        System.err.println("  - 访问被拒绝：检查数据库用户权限");
        break;
      default:
        // 基于SQL状态提供建议
        if (sqlState != null) {
          if (sqlState.startsWith("23")) {
            System.err.println("  - 完整性约束违反：检查外键、唯一约束或非空约束");
          } else if (sqlState.startsWith("42")) {
            System.err.println("  - 语法错误或访问规则违反：检查SQL语法和权限");
          } else if (sqlState.startsWith("08")) {
            System.err.println("  - 连接异常：检查数据库连接配置");
          } else if (sqlState.startsWith("40")) {
            System.err.println("  - 事务回滚：检查事务逻辑和锁冲突");
          }
        }
        break;
    }

    System.err.println("  - 建议查看完整的异常堆栈信息以获取更多详情");
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

    String enableSqlSecurityCheckStr = properties.getProperty("enableSqlSecurityCheck", "true");
    this.enableSqlSecurityCheck = Boolean.parseBoolean(enableSqlSecurityCheckStr);

    String enableSqlOptimizationAdviceStr = properties.getProperty("enableSqlOptimizationAdvice", "true");
    this.enableSqlOptimizationAdvice = Boolean.parseBoolean(enableSqlOptimizationAdviceStr);

    String slowSqlThresholdStr = properties.getProperty("slowSqlThreshold", "1000");
    try {
      this.slowSqlThreshold = Long.parseLong(slowSqlThresholdStr);
    } catch (NumberFormatException e) {
      System.err.println("警告: slowSqlThreshold配置无效，使用默认值1000ms");
      this.slowSqlThreshold = 1000L;
    }

    if (enableDetailLog) {
      System.out.println("=== StatementHandlerInterceptor 配置信息 ===");
      System.out.println("详细日志记录: " + this.enableDetailLog);
      System.out.println("性能监控: " + this.enablePerformanceMonitor);
      System.out.println("SQL安全检查: " + this.enableSqlSecurityCheck);
      System.out.println("SQL优化建议: " + this.enableSqlOptimizationAdvice);
      System.out.println("慢SQL阈值: " + this.slowSqlThreshold + "ms");
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
    // 只对StatementHandler类型的对象进行代理
    if (target instanceof StatementHandler) {
      return Plugin.wrap(target, this);
    }
    return target;
  }

  private String getTraceId() {
    // 用这个代替 traceId
    return String.valueOf(System.currentTimeMillis());
  }

  private String buildMark(String statementId, String traceId, Map<String, String> extras) {
    StringBuilder sb = new StringBuilder();
    sb.append("statementId: ").append(statementId)
      .append(", traceId: ").append(traceId);
    if (extras != null) {
      extras.forEach((k, v) -> sb.append(", ").append(k).append(": ").append(v));
    }
    return sb.toString();
  }

  public static class SqlMarkingThreadLocal {
    private static final ThreadLocal<Map<String, String>> TL = new ThreadLocal<>();
    public static void putAll(Map<String, String> m) { TL.set(m); }
    public static Map<String, String> get() { return TL.get(); }
    public static void clear() { TL.remove(); }
  }

}

