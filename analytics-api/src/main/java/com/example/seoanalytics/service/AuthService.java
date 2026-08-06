package com.example.seoanalytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seoanalytics.connector.GscConnector;
import com.example.seoanalytics.entity.Platform;
import com.example.seoanalytics.entity.PlatformAuth;
import com.example.seoanalytics.mapper.PlatformAuthMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PlatformAuthMapper platformAuthMapper;
    private final GscConnector gscConnector;

    public String getGscAuthorizeUrl(Long siteId) {
        return gscConnector.buildAuthorizeUrl(siteId);
    }

    @Transactional
    public PlatformAuth handleGscCallback(String code, Long siteId) {
        PlatformAuth auth = gscConnector.exchangeCode(code);
        auth.setSiteId(siteId);
        saveOrUpdate(auth);
        return auth;
    }

    @Transactional
    public PlatformAuth saveBingApiKey(Long siteId, String apiKey) {
        PlatformAuth auth = new PlatformAuth();
        auth.setSiteId(siteId);
        auth.setPlatform(Platform.BING);
        auth.setApiKey(apiKey);
        auth.setCreatedAt(LocalDateTime.now());
        saveOrUpdate(auth);
        return auth;
    }

    public List<PlatformAuth> listBySite(Long siteId) {
        return platformAuthMapper.selectList(
                new LambdaQueryWrapper<PlatformAuth>().eq(PlatformAuth::getSiteId, siteId));
    }

    public PlatformAuth getAuth(Long siteId, Platform platform) {
        return platformAuthMapper.selectOne(
                new LambdaQueryWrapper<PlatformAuth>()
                        .eq(PlatformAuth::getSiteId, siteId)
                        .eq(PlatformAuth::getPlatform, platform));
    }

    public void persistAuth(PlatformAuth auth) {
        saveOrUpdate(auth);
    }

    private void saveOrUpdate(PlatformAuth auth) {
        PlatformAuth existing = getAuth(auth.getSiteId(), auth.getPlatform());
        if (existing != null) {
            existing.setAccessToken(auth.getAccessToken());
            existing.setRefreshToken(auth.getRefreshToken() != null ? auth.getRefreshToken() : existing.getRefreshToken());
            existing.setApiKey(auth.getApiKey() != null ? auth.getApiKey() : existing.getApiKey());
            existing.setTokenExpiry(auth.getTokenExpiry());
            platformAuthMapper.updateById(existing);
            auth.setId(existing.getId());
        } else {
            if (auth.getCreatedAt() == null) {
                auth.setCreatedAt(LocalDateTime.now());
            }
            platformAuthMapper.insert(auth);
        }
    }
}
