/**
 *    Copyright 2009-2024 the original author or authors.
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
package org.apache.ibatis.executor.statement;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility for {@link java.sql.Statement}.
 *
 * @since 3.4.0
 * @author Kazuki Shimizu
 */
public class StatementUtil {

  private StatementUtil() {
    // NOP
  }

  /**
   * Apply a transaction timeout.
   * <p>
   * Update a query timeout to apply a transaction timeout.
   * </p>
   * @param statement a target statement
   * @param queryTimeout a query timeout
   * @param transactionTimeout a transaction timeout
   * @throws SQLException if a database access error occurs, this method is called on a closed <code>Statement</code>
   */
  public static void applyTransactionTimeout(Statement statement, Integer queryTimeout, Integer transactionTimeout) throws SQLException {
    if (transactionTimeout == null) {
      return;
    }

    // 将比较后较小的值设置为最终结果值
    if (queryTimeout == null || queryTimeout == 0 || transactionTimeout < queryTimeout) {

      /**
       * spring事务注解设置的超时时间，最终在mybatis里面是设置到了statement对象中了，
       * 是用jdbc对象来控制sql的执行时间的，如果执行时间超过了设置时间就会抛出异常，这个异常就会被spring事务切面捕获到最终导致事务回滚
       */
      statement.setQueryTimeout(transactionTimeout);
    }
  }

}
