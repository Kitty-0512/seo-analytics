package com.example.seoanalytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seoanalytics.entity.Site;
import com.example.seoanalytics.mapper.SiteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteMapper siteMapper;

    public List<Site> listAll() {
        return siteMapper.selectList(new LambdaQueryWrapper<Site>().orderByDesc(Site::getCreatedAt));
    }

    public Site getById(Long id) {
        return siteMapper.selectById(id);
    }

    public Site create(Site site) {
        site.setCreatedAt(LocalDateTime.now());
        siteMapper.insert(site);
        return site;
    }

    public Site update(Long id, Site input) {
        Site existing = siteMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Site not found: " + id);
        }
        if (input.getName() != null) existing.setName(input.getName());
        if (input.getDomain() != null) existing.setDomain(input.getDomain());
        if (input.getGscProperty() != null) existing.setGscProperty(input.getGscProperty());
        if (input.getBingSiteUrl() != null) existing.setBingSiteUrl(input.getBingSiteUrl());
        siteMapper.updateById(existing);
        return existing;
    }

    public void delete(Long id) {
        siteMapper.deleteById(id);
    }
}
