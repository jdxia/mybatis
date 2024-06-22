/**
 *    Copyright 2009-2020 the original author or authors.
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
package org.apache.ibatis.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 */
public class MapperRegistry {

  private final Configuration config;

  /**
   * 在MapperRegistry中维护了接口类与代理工程的映射关系，knownMappers
   * 在 org.apache.ibatis.binding.MapperRegistry#addMapper(java.lang.Class) 里面 存的
   */
  private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<>();

  public MapperRegistry(Configuration config) {
    this.config = config;
  }

  //MapperRegistry 中的 g etMapper
  @SuppressWarnings("unchecked")
  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    //这个MapperProxyFactory是调用addMapper方法时加到knownMappers中的，
    final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
    if (mapperProxyFactory == null) {
      //说明这个Mapper接口没有注册
      throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
    }
    try {
      //生成一个MapperProxy对象
      //通过动态代理⼯⼚⽣成
      return mapperProxyFactory.newInstance(sqlSession);
    } catch (Exception e) {
      throw new BindingException("Error getting mapper instance. Cause: " + e, e);
    }
  }

  public <T> boolean hasMapper(Class<T> type) {
    return knownMappers.containsKey(type);
  }

  public <T> void addMapper(Class<T> type) {
    // 只有接口才会解析
    // 对于mybatis mapper接口文件，必须是interface，不能是class
    if (type.isInterface()) {
      // 重复注册的检查
      if (hasMapper(type)) {
        throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
      }
      boolean loadCompleted = false;
      try {
        // 记录在Map中，留意value的类型是MapperProxyFactory
        // 为mapper接口创建一个MapperProxyFactory代理
        knownMappers.put(type, new MapperProxyFactory<>(type));
        // It's important that the type is added before the parser is run
        // otherwise the binding may automatically be attempted by the
        // mapper parser. If the type is already known, it won't try.
        //重要的是，必须在运行 Mapper 解析器之前添加 Mapper 接口类型，否则 Mapper 的解析器可能会自动尝试进行绑定。如果 Mapper 类型是已知的，则不会尝试。
        // type就是mapper接口
        MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
        /**
         * 利用MapperAnnotationBuilder解析Mapper接口
         * MapperAnnotationBuilder 会扫描mapper接口中的注解（如 @Select、@Insert 等），并将这些注解中的SQL语句注册到MyBatis的配置
         *
         * 实际在curd的时候, sqlSession select的时候会
         * 根据传⼊的全限定名+⽅法名从映射的Map中取出MappedStatement对象
         * statement: com.example.demo.mapper.DepartmentMapper.findById
         * MappedStatement ms = configuration.getMappedStatement(statement);
         */
        parser.parse();
        loadCompleted = true;
      } finally {
        if (!loadCompleted) {
          knownMappers.remove(type);
        }
      }
    }
  }

  /**
   * Gets the mappers.
   *
   * @return the mappers
   * @since 3.2.2
   */
  public Collection<Class<?>> getMappers() {
    return Collections.unmodifiableCollection(knownMappers.keySet());
  }

  /**
   * Adds the mappers.
   *
   * @param packageName
   *          the package name
   * @param superType
   *          the super type
   * @since 3.2.2
   */
  public void addMappers(String packageName, Class<?> superType) {
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
    resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
    Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
    for (Class<?> mapperClass : mapperSet) {
      addMapper(mapperClass);
    }
  }

  /**
   * Adds the mappers.
   *
   * @param packageName
   *          the package name
   * @since 3.2.2
   */
  public void addMappers(String packageName) {
    addMappers(packageName, Object.class);
  }

}
