# Google Sheets集成功能说明

## 功能概述

财务管理系统现已支持将年度财务报表自动同步到Google Sheets，方便在线分享和协作。

## 主要功能

### 导出内容

年度财务报表包含以下5个工作表：

1. **资产负债表** - 年底资产、负债和净资产情况，按货币分组显示
2. **开支表-USD** - 美元账户的全年支出明细，分日常开支和大项开支
3. **开支表-CNY** - 人民币账户的全年支出明细
4. **投资账户明细** - 非退休账户的投资记录
5. **退休账户明细** - 退休账户（401k, IRA等）的投资记录

### 权限设置

支持两种访问权限：

- **只读（reader）** - 其他人只能查看，不能编辑（推荐）
- **可编辑（writer）** - 其他人可以查看和编辑

## 使用方法

### 1. 配置Service Account凭证

首先需要在Google Cloud Platform创建Service Account：

1. 访问 [Google Cloud Console](https://console.cloud.google.com/)
2. 创建新项目或选择现有项目
3. 启用以下API：
   - Google Sheets API
   - Google Drive API
4. 创建Service Account并下载JSON密钥文件
5. 将JSON文件移动到：`backend/src/main/resources/config/google-credentials.json`

### 2. 前端操作

1. 登录系统，进入"年度趋势分析"页面
2. 点击页面右上角的"同步到Google Sheets"按钮（绿色按钮）
3. 在弹窗中选择：
   - 年份（默认为当前年份）
   - 访问权限（只读或可编辑）
4. 点击"开始同步"
5. 等待同步完成（通常需要5-10秒）
6. 同步成功后，点击"打开Google Sheets"查看报表

### 3. API调用（开发者）

#### 同步年度报表

```bash
POST /api/google-sheets/sync-annual-report

参数：
- familyId: 家庭ID（必填）
- year: 年份（必填）
- permission: 权限设置，reader或writer（可选，默认reader）

响应：
{
  "spreadsheetId": "1abc...",
  "shareUrl": "https://docs.google.com/spreadsheets/d/1abc...",
  "permission": "reader",
  "message": "报表已成功同步到Google Sheets"
}
```

#### 测试连接

```bash
GET /api/google-sheets/test-connection

响应：
{
  "status": "success",
  "message": "Google Sheets API连接正常"
}
```

## 技术实现

### 后端架构

```
GoogleSheetsController  // REST API
    ↓
GoogleSheetsExportService  // 业务逻辑，复用ExcelExportService的数据查询
    ↓
GoogleSheetsService  // Google API封装，处理认证和基础操作
```

### 前端组件

- `frontend/src/api/googleSheets.js` - API客户端
- `frontend/src/components/GoogleSheetsSync.vue` - 同步弹窗组件
- `frontend/src/views/analysis/AnnualTrend.vue` - 集成同步按钮

### 依赖库

Maven依赖：
- `com.google.apis:google-api-services-sheets:v4-rev20220927-2.0.0`
- `com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0`
- `com.google.auth:google-auth-library-oauth2-http:1.19.0`

## API配额

Google Sheets API（免费）配额：

- **读取请求**：500次/天
- **写入请求**：300次/100秒

对于个人财务管理应用，这个配额完全足够。

## 常见问题

### 1. 认证失败（401错误）

**原因**：Service Account凭证文件缺失或配置错误

**解决**：
- 检查 `backend/src/main/resources/config/google-credentials.json` 是否存在
- 验证JSON文件格式是否正确
- 确认已启用Google Sheets API和Google Drive API

### 2. 同步失败（500错误）

**原因**：可能是网络问题或API配额超限

**解决**：
- 检查网络连接
- 查看后端日志：`tail -f backend/backend.log`
- 检查Google Cloud Console的API配额使用情况

### 3. 创建的表格无法访问

**原因**：Service Account创建的表格默认只有Service Account自己可以访问

**解决**：系统已自动设置权限为"anyone with link"，确保选择了正确的权限级别（reader或writer）

## 安全注意事项

1. **凭证文件保护**
   - `google-credentials.json` 已添加到 `.gitignore`
   - 切勿将凭证文件提交到Git仓库
   - 服务器部署时需要单独配置凭证文件

2. **权限管理**
   - 建议默认使用"只读"权限
   - 仅在需要协作编辑时使用"可编辑"权限
   - Service Account本身对表格有完全控制权

3. **数据隐私**
   - 导出的表格包含完整财务数据
   - 分享链接前请确认接收方的可信度
   - 可以随时在Google Drive中删除或修改权限

## 未来改进方向

- [ ] 支持更新现有表格（而非每次创建新表格）
- [ ] 支持自定义导出字段和格式
- [ ] 支持定时自动同步
- [ ] 添加更多图表和可视化内容
- [ ] 支持导出历史数据对比

## 维护记录

- **2025-01-02**: 完成Google Sheets集成功能，支持年度报表导出
