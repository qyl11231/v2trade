#!/bin/bash

# 时间管理修复验证脚本
# 用于快速验证修复是否生效

echo "=========================================="
echo "  v2trade 时间管理修复验证脚本"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. 检查关键文件是否存在
echo "1. 检查关键文件..."
if [ -f "src/main/java/com/qyl/v2trade/market/model/dto/KlineResponse.java" ]; then
    echo -e "${GREEN}✓${NC} KlineResponse.java 存在"
else
    echo -e "${RED}✗${NC} KlineResponse.java 不存在"
    exit 1
fi

if [ -f "src/main/java/com/qyl/v2trade/common/util/TimeUtil.java" ]; then
    echo -e "${GREEN}✓${NC} TimeUtil.java 存在"
else
    echo -e "${RED}✗${NC} TimeUtil.java 不存在"
    exit 1
fi

# 2. 检查 @JsonFormat 注解是否存在
echo ""
echo "2. 检查 @JsonFormat 注解..."
if grep -q "@JsonFormat(shape = JsonFormat.Shape.NUMBER)" src/main/java/com/qyl/v2trade/market/model/dto/KlineResponse.java; then
    echo -e "${GREEN}✓${NC} @JsonFormat 注解已添加"
else
    echo -e "${RED}✗${NC} @JsonFormat 注解未找到"
    echo -e "${YELLOW}提示：请确保在 timestamp 字段上添加了 @JsonFormat(shape = JsonFormat.Shape.NUMBER) 注解${NC}"
    exit 1
fi

# 3. 检查 Jackson 导入
echo ""
echo "3. 检查 Jackson 导入..."
if grep -q "import com.fasterxml.jackson.annotation.JsonFormat;" src/main/java/com/qyl/v2trade/market/model/dto/KlineResponse.java; then
    echo -e "${GREEN}✓${NC} Jackson 导入已添加"
else
    echo -e "${RED}✗${NC} Jackson 导入未找到"
    exit 1
fi

# 4. 编译检查
echo ""
echo "4. 编译项目..."
echo -e "${YELLOW}正在编译，请稍候...${NC}"
./mvnw clean compile -DskipTests > /tmp/compile.log 2>&1

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} 编译成功"
else
    echo -e "${RED}✗${NC} 编译失败"
    echo "查看详细错误信息："
    tail -50 /tmp/compile.log
    exit 1
fi

# 5. 检查文档
echo ""
echo "5. 检查修复文档..."
if [ -f "docs/时间管理修复说明.md" ]; then
    echo -e "${GREEN}✓${NC} 修复说明文档存在"
else
    echo -e "${YELLOW}⚠${NC} 修复说明文档不存在"
fi

# 总结
echo ""
echo "=========================================="
echo -e "${GREEN}✓ 所有检查通过！${NC}"
echo "=========================================="
echo ""
echo "下一步操作："
echo "1. 启动服务: ./mvnw spring-boot:run"
echo "2. 访问前端: http://localhost:8080/market-center.html"
echo "3. 检查浏览器控制台是否有错误"
echo "4. 验证图表是否正常渲染"
echo ""
echo "如果一切正常，请推送到远程仓库："
echo "  git push origin devlop"
echo ""
