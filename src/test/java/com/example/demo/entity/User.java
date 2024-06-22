package com.example.demo.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class User implements Serializable {
  private static final long serialVersionUID = 1L;

  private String id;

  private String name;

  private Integer age;

  private Date birthday;

  private Department department;
}
