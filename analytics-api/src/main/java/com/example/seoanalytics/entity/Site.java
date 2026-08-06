package com.example.seoanalytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site")
public class Site {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String domain;
    private String gscProperty;
    private String bingSiteUrl;
    private LocalDateTime createdAt;
}
