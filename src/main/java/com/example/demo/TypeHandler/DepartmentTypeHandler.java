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
package com.example.demo.TypeHandler;

import com.example.demo.entity.Department;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DepartmentTypeHandler implements TypeHandler<Department> {

  @Override
  public void setParameter(PreparedStatement ps, int i, Department department, JdbcType jdbcType) throws SQLException {
    ps.setString(i, department.getId());
  }

  @Override
  public Department getResult(ResultSet rs, String columnName) throws SQLException {
    Department department = new Department();
    department.setId(rs.getString(columnName));
    return department;
  }

  @Override
  public Department getResult(ResultSet rs, int columnIndex) throws SQLException {
    Department department = new Department();
    department.setId(rs.getString(columnIndex));
    return department;
  }

  @Override
  public Department getResult(CallableStatement cs, int columnIndex) throws SQLException {
    Department department = new Department();
    department.setId(cs.getString(columnIndex));
    return department;
  }
}
