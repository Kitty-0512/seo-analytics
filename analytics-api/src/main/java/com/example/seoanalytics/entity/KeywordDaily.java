package com.example.seoanalytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("keyword_daily")
public class KeywordDaily {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long siteId;
    private Platform platform;
    private LocalDate statDate;
    private String keyword;
    private Long clicks;
    private Long impressions;
    private Double avgPosition;
}
