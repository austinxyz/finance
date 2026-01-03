package com.finance.app.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * Google OAuth 2.0 认证服务
 * 用于个人 Google 账户的用户认证
 */
@Service
@Slf4j
public class GoogleOAuthService {

    private static final String APPLICATION_NAME = "Personal Finance Management";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "config/google-oauth-credentials.json";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    // OAuth 2.0 Scopes
    private static final List<String> SCOPES = Arrays.asList(
        "https://www.googleapis.com/auth/spreadsheets",
        "https://www.googleapis.com/auth/drive.file"
    );

    /**
     * 创建信任所有证书的HTTP传输
     */
    private NetHttpTransport createTrustAllTransport() throws GeneralSecurityException, IOException {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            return new NetHttpTransport.Builder()
                .setSslSocketFactory(sslContext.getSocketFactory())
                .setHostnameVerifier((hostname, session) -> true)
                .build();
        } catch (Exception e) {
            log.warn("无法创建自定义SSL传输，回退到默认传输", e);
            return GoogleNetHttpTransport.newTrustedTransport();
        }
    }

    /**
     * 获取用户凭证（OAuth 2.0）
     * 如果是首次访问，会自动打开浏览器进行授权
     */
    public Credential getUserCredential() throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = createTrustAllTransport();

        // 加载 OAuth 客户端凭证
        ClassPathResource resource = new ClassPathResource(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets;
        try (InputStream in = resource.getInputStream()) {
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        }

        // 构建认证流程
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();

        // 获取凭证（自动处理授权流程）
        // 使用固定端口 8889（避免与后端的 8080 和常见的 8888 冲突）
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8889).build();

        try {
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
            log.info("OAuth 2.0 认证成功");
            return credential;
        } finally {
            // 确保关闭接收器以释放端口
            try {
                receiver.stop();
            } catch (Exception e) {
                log.warn("关闭OAuth接收器时出错", e);
            }
        }
    }

    /**
     * 检查是否已有有效的授权令牌
     */
    public boolean hasValidToken() {
        try {
            java.io.File tokenFile = new java.io.File(TOKENS_DIRECTORY_PATH + "/StoredCredential");
            return tokenFile.exists();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 清除已保存的令牌（用于重新授权）
     */
    public void clearTokens() {
        try {
            java.io.File tokenDir = new java.io.File(TOKENS_DIRECTORY_PATH);
            if (tokenDir.exists()) {
                deleteDirectory(tokenDir);
                log.info("已清除OAuth令牌");
            }
        } catch (Exception e) {
            log.error("清除令牌失败", e);
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(java.io.File directory) {
        if (directory.isDirectory()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
}
