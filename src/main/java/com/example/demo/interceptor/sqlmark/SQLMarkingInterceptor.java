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
package com.example.demo.interceptor.sqlmark;

import org.apache.ibatis.executor.statement.BaseStatementHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.LinkedHashMap;

@Intercepts({
  @Signature(method = "prepare", type = StatementHandler.class, args = {Connection.class, Integer.class})
})
public class SQLMarkingInterceptor implements Interceptor {

  private static final Log log = LogFactory.getLog(SQLMarkingInterceptor.class);

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    try {
      // 1. 找到 StatementHandler（SQL 执行时，StatementHandler 的实际类型为 RoutingStatementHandler）
      RoutingStatementHandler routingStatementHandler = getRoutingStatementHandler(invocation.getTarget());

      if (routingStatementHandler != null) {
        // 其中 delegate 是实际类型的 StatementHandler （静态代理模式），获取到实际的 StatementHandler
        StatementHandler delegate = getFieldValue(
          RoutingStatementHandler.class, routingStatementHandler, "delegate", StatementHandler.class
        );
        // 2. 找到 StatementHandler 之后便能拿到 SQL 相关信息，现在对 SQL 信息打标即可
        marking(delegate);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return invocation.proceed();
  }

  private RoutingStatementHandler getRoutingStatementHandler(Object target)
    throws NoSuchFieldException, IllegalAccessException {
    // 如果被代理，那么一直找到具体被代理的对象
    while (Proxy.isProxyClass(target.getClass())) {
      target = Proxy.getInvocationHandler(target);
    }
    while (target instanceof Plugin) {
      Plugin plugin = (Plugin) target;
      target = getFieldValue(Plugin.class, plugin, "target", Object.class);
    }
    // 找到了 RoutingStatementHandler
    if (target instanceof RoutingStatementHandler) {
      return (RoutingStatementHandler) target;
    }

    return null;
  }

  private void marking(StatementHandler delegate) throws NoSuchFieldException, IllegalAccessException {
    BoundSql boundSql = delegate.getBoundSql();
    // 实际的 SQL
    String sql = boundSql.getSql().trim();
    // 只对 select 打标
    if (containsIgnoreCase(sql, "select")) {
      // 获取其基类中的 MappedStatement 即定义的 SQL 声明对象，获取它的 ID 值表示它是哪条 SQL
      MappedStatement mappedStatement = getFieldValue(
        BaseStatementHandler.class, delegate, "mappedStatement", MappedStatement.class
      );
      String mappedStatementId = mappedStatement.getId();
      // 方法调用栈
      String trace = trace();

      // 按顺序创建打标的内容
      LinkedHashMap<String, Object> markingMap = new LinkedHashMap<>();
      markingMap.put("STATEMENT_ID", mappedStatementId);
      markingMap.put("STACK_TRACE", trace);
      String marking = "[SQLMarking] ".concat(markingMap.toString());

      // 打标
      sql = String.format(" /* %s */ %s", marking, sql);

      // 反射更新
      Field field = getField(BoundSql.class, "sql");
      field.set(boundSql, sql);
    }
  }

  /**
   * 通用的反射获取字段值方法
   * Linus: "好代码没有特殊情况" - 这个方法处理所有反射获取字段的场景
   *
   * @param clazz      字段所在的类
   * @param instance   实例对象
   * @param fieldName  字段名
   * @param returnType 返回类型
   * @return 字段值
   */
  private <T> T getFieldValue(Class<?> clazz, Object instance, String fieldName, Class<T> returnType)
    throws NoSuchFieldException, IllegalAccessException {
    Field field = getField(clazz, fieldName);
    Object value = field.get(instance);
    return returnType.cast(value);
  }

  /**
   * 获取字段（包括私有字段和父类字段）
   * 注意：这里破坏了封装，但这是拦截器的必要之恶
   *
   * @param clazz     类
   * @param fieldName 字段名
   * @return 字段对象
   */
  private Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Field field = null;
    Class<?> currentClass = clazz;

    // 向上查找父类，直到找到字段或到达 Object 类
    while (currentClass != null && currentClass != Object.class) {
      try {
        field = currentClass.getDeclaredField(fieldName);
        break;
      } catch (NoSuchFieldException e) {
        currentClass = currentClass.getSuperclass();
      }
    }

    if (field == null) {
      throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy of " + clazz.getName());
    }

    // 设置可访问，破坏封装但这是拦截器的需要
    field.setAccessible(true);
    return field;
  }

  /**
   * 获取当前方法调用栈（用于追踪 SQL 的调用来源）
   * 实际生产中需要过滤掉框架层的调用栈，只保留业务代码
   *
   * @return 调用栈字符串
   */
  private String trace() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    StringBuilder sb = new StringBuilder();

    // 从索引 3 开始，跳过 getStackTrace、trace、marking 方法本身
    // 实际生产中建议：
    // 1. 只保留业务包路径（如 com.example.demo）
    // 2. 限制栈深度（如只取前 5 层）
    // 3. 过滤掉 MyBatis 和 Spring 框架层调用
    for (int i = 3; i < Math.min(stackTrace.length, 8); i++) {
      StackTraceElement element = stackTrace[i];
      String className = element.getClassName();

      // 简单过滤：跳过 MyBatis 和 Java 内部类
      if (className.startsWith("org.apache.ibatis") ||
        className.startsWith("java.") ||
        className.startsWith("sun.") ||
        className.startsWith("jdk.")) {
        continue;
      }

      if (sb.length() > 0) {
        sb.append(" -> ");
      }
      sb.append(element.getClassName())
        .append(".")
        .append(element.getMethodName())
        .append("(")
        .append(element.getFileName())
        .append(":")
        .append(element.getLineNumber())
        .append(")");
    }

    return sb.length() > 0 ? sb.toString() : "unknown";
  }

  /**
   * 简单的字符串包含判断（忽略大小写）
   * 避免引入 Apache Commons 等第三方库，保持简洁
   *
   * @param str       源字符串
   * @param searchStr 搜索字符串
   * @return 是否包含
   */
  private boolean containsIgnoreCase(String str, String searchStr) {
    if (str == null || searchStr == null) {
      return false;
    }
    return str.toLowerCase().contains(searchStr.toLowerCase());
  }
}
