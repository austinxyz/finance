#!/bin/bash

# 测试资产配置页面的 API 调用是否正确支持 familyId 参数

BASE_URL="http://localhost:3000/api"
FAMILY_ID=1  # Austin Family

echo "========================================="
echo "测试资产配置页面 API 调用 (Austin Family)"
echo "========================================="
echo ""

echo "1. 测试资产总览 API"
echo "GET $BASE_URL/analysis/summary?familyId=$FAMILY_ID"
curl -s "$BASE_URL/analysis/summary?familyId=$FAMILY_ID" | jq '.'
echo ""
echo "========================================="
echo ""

echo "2. 测试净资产配置 API"
echo "GET $BASE_URL/analysis/allocation/net?familyId=$FAMILY_ID"
curl -s "$BASE_URL/analysis/allocation/net?familyId=$FAMILY_ID" | jq '.'
echo ""
echo "========================================="
echo ""

echo "3. 测试总资产配置 API"
echo "GET $BASE_URL/analysis/allocation/type?familyId=$FAMILY_ID"
curl -s "$BASE_URL/analysis/allocation/type?familyId=$FAMILY_ID" | jq '.'
echo ""
echo "========================================="
echo ""

echo "4. 测试总负债配置 API"
echo "GET $BASE_URL/analysis/allocation/liability?familyId=$FAMILY_ID"
curl -s "$BASE_URL/analysis/allocation/liability?familyId=$FAMILY_ID" | jq '.'
echo ""
echo "========================================="
echo ""

echo "5. 测试税收状态配置 API"
echo "GET $BASE_URL/analysis/allocation/net-worth-by-tax-status?familyId=$FAMILY_ID"
curl -s "$BASE_URL/analysis/allocation/net-worth-by-tax-status?familyId=$FAMILY_ID" | jq '.'
echo ""
echo "========================================="
echo ""

echo "6. 测试家庭成员配置 API"
echo "GET $BASE_URL/analysis/allocation/net-worth-by-member?familyId=$FAMILY_ID"
curl -s "$BASE_URL/analysis/allocation/net-worth-by-member?familyId=$FAMILY_ID" | jq '.'
echo ""
echo "========================================="
echo ""

echo "7. 测试货币分布配置 API"
echo "GET $BASE_URL/analysis/allocation/net-worth-by-currency?familyId=$FAMILY_ID"
curl -s "$BASE_URL/analysis/allocation/net-worth-by-currency?familyId=$FAMILY_ID" | jq '.'
echo ""
echo "========================================="
echo ""

echo "所有测试完成！"
