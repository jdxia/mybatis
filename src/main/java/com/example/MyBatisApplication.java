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
import com.example.demo.interceptor.ExecutorInterceptor;
import com.example.demo.mapper.DepartmentMapper;
import org.apache.ibatis.executor.SimpleExecutor;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.session.*;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

public class MyBatisApplication {

  public static void main(String[] args) throws Exception {
    InputStream xml = Resources.getResourceAsStream("mybatis-config.xml");

    // build 往下
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(xml);

//    Configuration configuration = sqlSessionFactory.getConfiguration();
//
//    // 创建并配置ExecutorInterceptor
//    ExecutorInterceptor executorInterceptor = new ExecutorInterceptor();
//    Properties executorProps = new Properties();
//    executorProps.setProperty("enableDetailLog", "false"); // 关闭详细日志
//    executorProps.setProperty("slowQueryThreshold", "2000"); // 设置慢查询阈值为2秒
//    executorInterceptor.setProperties(executorProps);
//    // 添加拦截器
//    configuration.addInterceptor(executorInterceptor);

    /**
     * 创建 SqlSession, Executor插件也在这里包装起来, 是在里面的 configuration.newExecutor(tx, execType);
     * 生成 SqlSession 对象而已, 创建 JdbcTransactionFactory
     */
    SqlSession sqlSession = sqlSessionFactory.openSession();

    /**
     * plugin是在 {@link Plugin#wrap(Object, Interceptor)} 这里被包装的
     * 在 {@link Plugin#invoke(Object, Method, Object[])} 这里被调用的
     *
     * 一个例子参考 {@link SimpleExecutor#doQuery(MappedStatement, Object, RowBounds, ResultHandler, BoundSql)}
     */

    System.out.println("========================> 开始");

    // 连续查询两次同一个Department
    DepartmentMapper departmentMapper = sqlSession.getMapper(DepartmentMapper.class);
    Department department = departmentMapper.findById("18ec781fbefd727923b0d35740b177ab");
    System.out.println(department);
//
////    sqlSession.clearCache();
//
//    Department department2 = departmentMapper.findById("18ec781fbefd727923b0d35740b177ab");
//    System.out.println("department == department2 : " + (department == department2));   // true
//    // 关闭第一个SqlSession使二级缓存保存
//    sqlSession.close();
//
//    System.out.println("========================> 第一个SqlSession关闭, 第二个SqlSession开启");
//    handleTwo(sqlSessionFactory, department2);
//
//    System.out.println("=====================> 执行完毕");

//    sqlSession.commit();
    // 手动关闭sqlSession，归还连接
    sqlSession.close();

  }

  private static void handleTwo(SqlSessionFactory sqlSessionFactory, Department department2) {
    SqlSession sqlSession2 = sqlSessionFactory.openSession();
    DepartmentMapper departmentMapper22 = sqlSession2.getMapper(DepartmentMapper.class);
    Department department22 = departmentMapper22.findById("18ec781fbefd727923b0d35740b177ab");
    System.out.println("department22 == department2 : " + (department22 == department2));
  }

}
