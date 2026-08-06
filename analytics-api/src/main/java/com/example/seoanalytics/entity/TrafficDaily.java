package com.example.seoanalytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("traffic_daily")
public class TrafficDaily {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long siteId;
    private Platform platform;
    private LocalDate statDate;
    private Long clicks;
    private Long impressions;
    private Double ctr;
    private Double avgPosition;
}
