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
package com.example;

import com.example.demo.entity.Department;

import java.sql.*;

public class SelectJdbcTest {

  public static void main(String[] args) {
    String jdbcUrl = "jdbc:mysql://localhost:3306/mybatis?characterEncoding=utf-8&verifyServerCertificate=false&useSSL=false&allowPublicKeyRetrieval=true";
    String username = "root";
    String password = "hello.world123";

    // 1. 建立数据库连接
    // 面试题：MyBatis中是在什么时候创建的数据库连接？
    try(Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {

      // 2. 创建PreparedStatement
      // 面试题：PreparedStatement和Statement对象的区别？
      // 高级面试题：MyBatis中PreparedStatementHandler的作用是什么？
      String sql = "select * from tbl_department where id = ?";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setString(1, "18ec781fbefd727923b0d35740b177ab");


      // 3. 执行查询操作
      ResultSet resultSet = preparedStatement.executeQuery();

      ResultSetMetaData metaData = resultSet.getMetaData();
      System.out.println(metaData.getColumnName(1));
      System.out.println(metaData.getColumnType(1));

      System.out.println("=========================================");

      // 4. 获取查询结果
      // 面试题：MyBatis中TypeHandler的作用是什么？
      // 面试题：MyBatis中ResultMap的作用是什么？
      // 高级面试题：MyBatis中的AutoMapping是什么意思？
      if (resultSet.next()) {
        String id = resultSet.getString("id"); // userId
        String name = resultSet.getString("name");
        String tel = resultSet.getString("tel");

        Department department = new Department()
          .setId(id)
          .setName(name)
          .setTel(tel);

        System.out.println(department);
      }

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

}
