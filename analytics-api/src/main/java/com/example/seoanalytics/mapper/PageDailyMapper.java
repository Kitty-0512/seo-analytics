package com.example.seoanalytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seoanalytics.entity.PageDaily;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface PageDailyMapper extends BaseMapper<PageDaily> {

    @Insert("""
            INSERT INTO page_daily (site_id, platform, stat_date, page_url, clicks, impressions)
            VALUES (#{siteId}, #{platform}::platform_type, #{statDate}, #{pageUrl}, #{clicks}, #{impressions})
            ON CONFLICT (site_id, platform, stat_date, page_url)
            DO UPDATE SET
                clicks = EXCLUDED.clicks,
                impressions = EXCLUDED.impressions
            """)
    int upsert(PageDaily row);

    @Select("""
            SELECT page_url,
                   SUM(clicks) AS clicks,
                   SUM(impressions) AS impressions
            FROM page_daily
            WHERE site_id = #{siteId}
              AND (#{platform}::text IS NULL OR platform::text = #{platform})
              AND stat_date BETWEEN #{startDate} AND #{endDate}
            GROUP BY page_url
            ORDER BY clicks DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> topPages(@Param("siteId") Long siteId,
                                       @Param("platform") String platform,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("limit") int limit);

    @Select("""
            SELECT stat_date,
                   SUM(clicks) AS clicks,
                   SUM(impressions) AS impressions
            FROM page_daily
            WHERE site_id = #{siteId}
              AND page_url = #{pageUrl}
              AND (#{platform}::text IS NULL OR platform::text = #{platform})
              AND stat_date BETWEEN #{startDate} AND #{endDate}
            GROUP BY stat_date
            ORDER BY stat_date
            """)
    List<Map<String, Object>> pageTrend(@Param("siteId") Long siteId,
                                        @Param("pageUrl") String pageUrl,
                                        @Param("platform") String platform,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}
