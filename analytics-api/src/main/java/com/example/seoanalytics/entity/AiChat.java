package com.example.seoanalytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_chat")
public class AiChat {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long siteId;
    private String question;
    private String sqlGenerated;
    private String dataJson;
    private String answer;
    private LocalDateTime createdAt;
}
