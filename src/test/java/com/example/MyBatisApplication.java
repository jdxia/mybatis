/**
 * Copyright 2009-2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example;

import com.example.demo.entity.Department;
import com.example.demo.entity.User;
import com.example.demo.mapper.DepartmentMapper;
import com.example.demo.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

public class MyBatisApplication {

  public static void main(String[] args) throws Exception {
    InputStream xml = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(xml);

    // 创建SqlSession, Executor插件也在这里包装起来, 是在里面的 configuration.newExecutor(tx, execType);
    SqlSession sqlSession = sqlSessionFactory.openSession();

    System.out.println("========================> 开始");

    // 连续查询两次同一个Department
    DepartmentMapper departmentMapper = sqlSession.getMapper(DepartmentMapper.class);
    Department department = departmentMapper.findById("18ec781fbefd727923b0d35740b177ab");
    System.out.println(department);

//    sqlSession.clearCache();

    Department department2 = departmentMapper.findById("18ec781fbefd727923b0d35740b177ab");
    System.out.println("department == department2 : " + (department == department2));   // true
    // 关闭第一个SqlSession使二级缓存保存
    sqlSession.close();

    System.out.println("========================> 第一个SqlSession关闭, 第二个SqlSession开启");
    SqlSession sqlSession2 = sqlSessionFactory.openSession();
    DepartmentMapper departmentMapper22 = sqlSession2.getMapper(DepartmentMapper.class);
    Department department22 = departmentMapper22.findById("18ec781fbefd727923b0d35740b177ab");
    System.out.println("department22 == department2 : " + (department22 == department2));

    System.out.println("=====================> 执行完毕");

//    sqlSession.commit();
    // 手动关闭sqlSession，归还连接
    sqlSession.close();

  }

}
