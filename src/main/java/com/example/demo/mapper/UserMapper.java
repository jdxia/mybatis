package com.example.demo.mapper;


import com.example.demo.entity.User;

import java.util.List;

public interface UserMapper {

  List<User> findAll();

  List<User> findAllLazy();

  List<User> findAllUseTypeHandler();

  Integer cleanCache();

}
