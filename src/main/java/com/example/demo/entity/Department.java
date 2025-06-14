package com.example.demo.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Set;

@Data
@Accessors(chain = true)
public class Department implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;

  private String name;

  private String tel;

  private Set<User> users;
}
