package com.example.seoanalytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("platform_auth")
public class PlatformAuth {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long siteId;
    private Platform platform;
    private String accessToken;
    private String refreshToken;
    private String apiKey;
    private LocalDateTime tokenExpiry;
    private LocalDateTime createdAt;
}
