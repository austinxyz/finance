# Service 层需要更新的方法

由于 Service 层的修改较为复杂，涉及到数据库查询逻辑的修改，我需要一一更新这些方法，为它们添加 `familyId` 参数支持。

## 需要更新的方法清单:

1. **getAssetAccountsWithBalancesByType** (行904)
   - 当前签名: `(String categoryType, Long userId, LocalDate asOfDate)`
   - 需要改为: `(String categoryType, Long userId, Long familyId, LocalDate asOfDate)`
   - 需要添加 familyId 过滤逻辑

2. **getLiabilityAccountsWithBalancesByType** (行940)
   - 当前签名: `(String categoryType, Long userId, LocalDate asOfDate)`
   - 需要改为: `(String categoryType, Long userId, Long familyId, LocalDate asOfDate)`
   - 需要添加 familyId 过滤逻辑

3. **getNetAssetCategoryAccounts** (行1098)
   - 当前签名: `(String categoryCode, Long userId, LocalDate asOfDate)`
   - 需要改为: `(String categoryCode, Long userId, Long familyId, LocalDate asOfDate)`
   - 需要添加 familyId 过滤逻辑

4. **getNetWorthByTaxStatus** (行1197)
   - 当前签名: `(Long userId, LocalDate asOfDate)`
   - 需要改为: `(Long userId, Long familyId, LocalDate asOfDate)`
   - 需要使用 familyId 调用 getAssetSummary

5. **getNetWorthByMember** (行1343)
   - 当前签名: `(Long familyId, LocalDate asOfDate)`
   - 需要改为: `(Long userId, Long familyId, LocalDate asOfDate)` (添加 userId 参数)
   - 保持现有逻辑，因为它已经使用 familyId

6. **getNetWorthByCurrency** (行2646)
   - 当前签名: `(Long userId, LocalDate asOfDate)`
   - 需要改为: `(Long userId, Long familyId, LocalDate asOfDate)`
   - 需要使用 familyId 调用其他方法

## 实现策略:

对于所有这些方法，添加 familyId 参数后，过滤逻辑应该是:
- 如果 familyId 不为 null，查询该 family 的所有用户，然后过滤这些用户的账户
- 如果 familyId 为 null 但 userId 不为 null，使用现有的 userId 过滤逻辑
- 如果两个参数都为 null，返回所有数据

需要添加辅助方法来获取 family 的所有用户 ID。
