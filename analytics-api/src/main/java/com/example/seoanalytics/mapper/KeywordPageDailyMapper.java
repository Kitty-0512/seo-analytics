package com.example.seoanalytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seoanalytics.entity.KeywordPageDaily;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface KeywordPageDailyMapper extends BaseMapper<KeywordPageDaily> {

    @Insert("""
            INSERT INTO keyword_page_daily
                (site_id, platform, stat_date, keyword, page_url, clicks, impressions, avg_position)
            VALUES
                (#{siteId}, #{platform}::platform_type, #{statDate}, #{keyword}, #{pageUrl},
                 #{clicks}, #{impressions}, #{avgPosition})
            ON CONFLICT (site_id, platform, stat_date, keyword, page_url)
            DO UPDATE SET
                clicks = EXCLUDED.clicks,
                impressions = EXCLUDED.impressions,
                avg_position = EXCLUDED.avg_position
            """)
    int upsert(KeywordPageDaily row);

    @Select("""
            SELECT keyword,
                   page_url,
                   SUM(clicks) AS clicks,
                   SUM(impressions) AS impressions,
                   AVG(avg_position) AS avg_position
            FROM keyword_page_daily
            WHERE site_id = #{siteId}
              AND (#{platform}::text IS NULL OR platform::text = #{platform})
              AND stat_date BETWEEN #{startDate} AND #{endDate}
              AND (#{keyword}::text IS NULL OR keyword = #{keyword})
              AND (#{pageUrl}::text IS NULL OR page_url = #{pageUrl})
            GROUP BY keyword, page_url
            ORDER BY clicks DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> queryPageMap(@Param("siteId") Long siteId,
                                           @Param("platform") String platform,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("keyword") String keyword,
                                           @Param("pageUrl") String pageUrl,
                                           @Param("limit") int limit);
}
