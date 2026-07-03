package com.finance.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资金跑道趋势：把已保存的跑道报告快照汇总成按 savedAt 升序的指标点，
 * 以及最新报告的分类支出明细（富化了名称/颜色）。
 */
public record RunwayTrendDTO(
        List<TrendPoint> points,
        List<CategoryItem> categories,
        List<CategoryItem> previousCategories
) {
    /** 单份报告抽取出的趋势点（金额单位 USD，保存时已折算）。 */
    public record TrendPoint(
            LocalDateTime savedAt,
            String reportName,
            BigDecimal liquidTotal,
            BigDecimal monthlyBurn,
            Integer runwayMonths,
            String depletionDate
    ) {}

    /** 最新报告的分类支出项（按 code join ExpenseCategoryMajor 富化 name/color）。 */
    public record CategoryItem(
            String code,
            String name,
            String color,
            BigDecimal amount
    ) {}
}
