package com.example.seoanalytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seoanalytics.entity.KeywordDaily;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface KeywordDailyMapper extends BaseMapper<KeywordDaily> {

    @Insert("""
            INSERT INTO keyword_daily (site_id, platform, stat_date, keyword, clicks, impressions, avg_position)
            VALUES (#{siteId}, #{platform}::platform_type, #{statDate}, #{keyword}, #{clicks}, #{impressions}, #{avgPosition})
            ON CONFLICT (site_id, platform, stat_date, keyword)
            DO UPDATE SET
                clicks = EXCLUDED.clicks,
                impressions = EXCLUDED.impressions,
                avg_position = EXCLUDED.avg_position
            """)
    int upsert(KeywordDaily row);

    @Select("""
            SELECT keyword,
                   SUM(clicks) AS clicks,
                   SUM(impressions) AS impressions,
                   AVG(avg_position) AS avg_position
            FROM keyword_daily
            WHERE site_id = #{siteId}
              AND (#{platform}::text IS NULL OR platform::text = #{platform})
              AND stat_date BETWEEN #{startDate} AND #{endDate}
            GROUP BY keyword
            ORDER BY clicks DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> topKeywords(@Param("siteId") Long siteId,
                                          @Param("platform") String platform,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("limit") int limit);

    @Select("""
            SELECT stat_date,
                   AVG(avg_position) AS avg_position,
                   SUM(clicks) AS clicks,
                   SUM(impressions) AS impressions
            FROM keyword_daily
            WHERE site_id = #{siteId}
              AND keyword = #{keyword}
              AND (#{platform}::text IS NULL OR platform::text = #{platform})
              AND stat_date BETWEEN #{startDate} AND #{endDate}
            GROUP BY stat_date
            ORDER BY stat_date
            """)
    List<Map<String, Object>> keywordTrend(@Param("siteId") Long siteId,
                                           @Param("keyword") String keyword,
                                           @Param("platform") String platform,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COUNT(DISTINCT keyword) AS keyword_count
            FROM keyword_daily
            WHERE site_id = #{siteId}
              AND (#{platform}::text IS NULL OR platform::text = #{platform})
              AND stat_date BETWEEN #{startDate} AND #{endDate}
            """)
    Long countKeywords(@Param("siteId") Long siteId,
                       @Param("platform") String platform,
                       @Param("startDate") LocalDate startDate,
                       @Param("endDate") LocalDate endDate);
}
