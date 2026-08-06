package com.example.seoanalytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seoanalytics.entity.TrafficDaily;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface TrafficDailyMapper extends BaseMapper<TrafficDaily> {

    @Insert("""
            INSERT INTO traffic_daily (site_id, platform, stat_date, clicks, impressions, ctr, avg_position)
            VALUES (#{siteId}, #{platform}::platform_type, #{statDate}, #{clicks}, #{impressions}, #{ctr}, #{avgPosition})
            ON CONFLICT (site_id, platform, stat_date)
            DO UPDATE SET
                clicks = EXCLUDED.clicks,
                impressions = EXCLUDED.impressions,
                ctr = EXCLUDED.ctr,
                avg_position = EXCLUDED.avg_position
            """)
    int upsert(TrafficDaily row);

    @Select("""
            SELECT COALESCE(SUM(clicks), 0) AS clicks,
                   COALESCE(SUM(impressions), 0) AS impressions,
                   CASE WHEN SUM(impressions) > 0
                        THEN SUM(clicks)::float / SUM(impressions)
                        ELSE 0 END AS ctr,
                   COALESCE(AVG(avg_position), 0) AS avg_position
            FROM traffic_daily
            WHERE site_id = #{siteId}
              AND (#{platform}::text IS NULL OR platform::text = #{platform})
              AND stat_date BETWEEN #{startDate} AND #{endDate}
            """)
    Map<String, Object> sumKpi(@Param("siteId") Long siteId,
                               @Param("platform") String platform,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT stat_date,
                   SUM(clicks) AS clicks,
                   SUM(impressions) AS impressions,
                   CASE WHEN SUM(impressions) > 0
                        THEN SUM(clicks)::float / SUM(impressions)
                        ELSE 0 END AS ctr,
                   AVG(avg_position) AS avg_position
            FROM traffic_daily
            WHERE site_id = #{siteId}
              AND (#{platform}::text IS NULL OR platform::text = #{platform})
              AND stat_date BETWEEN #{startDate} AND #{endDate}
            GROUP BY stat_date
            ORDER BY stat_date
            """)
    List<Map<String, Object>> trend(@Param("siteId") Long siteId,
                                    @Param("platform") String platform,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT platform::text AS platform,
                   stat_date,
                   SUM(clicks) AS clicks,
                   SUM(impressions) AS impressions
            FROM traffic_daily
            WHERE site_id = #{siteId}
              AND stat_date BETWEEN #{startDate} AND #{endDate}
            GROUP BY platform, stat_date
            ORDER BY stat_date, platform
            """)
    List<Map<String, Object>> trendByPlatform(@Param("siteId") Long siteId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT platform::text AS platform,
                   COALESCE(SUM(clicks), 0) AS clicks,
                   COALESCE(SUM(impressions), 0) AS impressions,
                   CASE WHEN SUM(impressions) > 0
                        THEN SUM(clicks)::float / SUM(impressions)
                        ELSE 0 END AS ctr,
                   COALESCE(AVG(avg_position), 0) AS avg_position
            FROM traffic_daily
            WHERE site_id = #{siteId}
              AND stat_date BETWEEN #{startDate} AND #{endDate}
            GROUP BY platform
            """)
    List<Map<String, Object>> kpiByPlatform(@Param("siteId") Long siteId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
}
