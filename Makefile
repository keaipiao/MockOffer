COMPOSE := docker compose -f deploy/docker-compose.yml

.PHONY: help up down restart logs ps build clean

help: ## 显示可用命令
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

up: ## 后台启动全部服务（前端/后端/PG/Redis/MinIO）
	$(COMPOSE) up -d
	@echo ""
	@echo "  服务已启动 / Services up:"
	@echo "    前端 Frontend     http://localhost:3000"
	@echo "    后端 Backend      http://localhost:8080   (健康 /actuator/health)"
	@echo "    MinIO 控制台      http://localhost:9001   (minioadmin/minioadmin)"
	@echo "    MinIO API (S3)    http://localhost:9000"
	@echo "    PostgreSQL        localhost:5432          (mockoffer/mockoffer)"
	@echo "    Redis             localhost:6379"

down: ## 停止并移除容器与网络
	$(COMPOSE) down

restart: down up ## 重启全部服务

logs: ## 跟随查看全部服务日志
	$(COMPOSE) logs -f

ps: ## 查看服务状态
	$(COMPOSE) ps

build: ## 重新构建镜像并启动
	$(COMPOSE) up -d --build

clean: ## 停止并清除数据卷（危险：会清空数据库/存储数据）
	$(COMPOSE) down -v
