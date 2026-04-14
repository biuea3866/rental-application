# Rental Commerce Grafana Dashboards

이 디렉토리에는 rental-commerce 프로젝트의 Grafana 10.x 모니터링 대시보드 JSON 파일이 포함되어 있습니다.

## 대시보드 목록

| 파일 | 제목 | UID | 메트릭 소스 | 주요 패널 |
|------|------|-----|------------|-----------|
| `mysql.json` | MySQL 모니터링 | `rental-mysql` | `mysql_*` (mysqld_exporter) | 커넥션, 쿼리/초, Slow Query, InnoDB Buffer Pool, 복제 지연 |
| `mongodb.json` | MongoDB 모니터링 | `rental-mongodb` | `mongodb_*` (mongodb_exporter) | Ops 처리량, 커넥션, 복제 지연, WiredTiger 캐시, 메모리 |
| `kafka.json` | Kafka 모니터링 | `rental-kafka` | `kafka_*` (JMX/kafka_exporter) | 브로커 처리량, 파티션 수, Consumer Lag, Under-Replicated Partitions |
| `application.json` | Spring Boot 애플리케이션 모니터링 | `rental-application` | `http_server_requests_*`, `process_*` (Micrometer) | HTTP 요청률, 지연 p50/p95/p99, 에러율, 활성 스레드 |
| `jvm.json` | JVM 모니터링 | `rental-jvm` | `jvm_*` (Micrometer) | 힙 Used/Committed/Max, GC 일시정지, GC 횟수, 스레드, 로드된 클래스 |
| `hikaricp.json` | HikariCP 커넥션 풀 모니터링 | `rental-hikaricp` | `hikaricp_*` (Micrometer) | Active/Idle/Total 커넥션, Pending 스레드, 획득 시간, 타임아웃 |
| `redis.json` | Redis 모니터링 | `rental-redis` | `redis_*` (redis_exporter) | 명령/초, 메모리, Hit/Miss Rate, 연결된 클라이언트, Keyspace 크기 |
| `system.json` | 시스템 (Node Exporter) 모니터링 | `rental-system` | `node_*` (node_exporter) | CPU, 메모리, 디스크, 네트워크, Load Average |

## 사전 요구 사항

- Grafana 10.x 이상
- Prometheus 데이터 소스 (UID: `prometheus`)
- 각 구성요소에 대응하는 Exporter 설치 및 실행:
  - MySQL: [mysqld_exporter](https://github.com/prometheus/mysqld_exporter)
  - MongoDB: [mongodb_exporter](https://github.com/percona/mongodb_exporter)
  - Kafka: [kafka_exporter](https://github.com/danielqsj/kafka_exporter) 또는 JMX Exporter
  - Spring Boot / JVM / HikariCP: [Micrometer](https://micrometer.io/) (Spring Boot Actuator 포함)
  - Redis: [redis_exporter](https://github.com/oliver006/redis_exporter)
  - 시스템: [node_exporter](https://github.com/prometheus/node_exporter)

## 대시보드 가져오기 방법

### 방법 1: Grafana UI를 통한 직접 임포트

1. Grafana 웹 UI에 접속합니다.
2. 좌측 메뉴에서 **Dashboards > Import**를 클릭합니다.
3. **Upload JSON file**을 클릭하고 원하는 `.json` 파일을 선택합니다.
4. 데이터 소스로 `prometheus`를 선택합니다.
5. **Import**를 클릭합니다.

### 방법 2: Grafana Provisioning (권장)

Kubernetes 환경에서는 ConfigMap을 통해 자동으로 프로비저닝할 수 있습니다.

```yaml
# grafana-dashboards-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-dashboards
  namespace: monitoring
data:
  mysql.json: |
    # mysql.json 내용 붙여넣기
  mongodb.json: |
    # mongodb.json 내용 붙여넣기
  # ... 나머지 대시보드
```

Grafana 프로비저닝 설정 (`/etc/grafana/provisioning/dashboards/`):

```yaml
# dashboards.yaml
apiVersion: 1
providers:
  - name: rental-dashboards
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    options:
      path: /var/lib/grafana/dashboards
```

### 방법 3: Grafana API를 통한 임포트

```bash
# 예: mysql.json 임포트
curl -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <API_TOKEN>" \
  -d "{\"dashboard\": $(cat mysql.json), \"overwrite\": true, \"folderId\": 0}" \
  http://<GRAFANA_HOST>/api/dashboards/db
```

## 대시보드 구조

모든 대시보드는 다음 공통 구조를 따릅니다:

- **schemaVersion**: 38 (Grafana 10.x)
- **refresh**: 30초 자동 갱신
- **timezone**: browser (로컬 타임존 사용)
- **datasource**: `{"type": "prometheus", "uid": "prometheus"}`
- **panels**: 8개 패널 (timeseries, gauge, stat 타입 혼합)
- **gridPos**: 각 패널 24칸 그리드 레이아웃

## 알림(Alert) 설정 권장 임계값

| 메트릭 | 경고 (Warning) | 위험 (Critical) |
|--------|---------------|-----------------|
| MySQL Active Connections | 100+ | 200+ |
| MySQL Slow Queries/sec | 1+ | 10+ |
| MongoDB Replication Lag | 5s+ | 30s+ |
| Kafka Consumer Lag | 1000+ | 10000+ |
| Under-Replicated Partitions | 1+ | 1+ (즉시 조치) |
| HTTP p99 Latency | 500ms+ | 1s+ |
| HTTP 5xx Error Rate | 1%+ | 5%+ |
| JVM Heap Usage | 70%+ | 90%+ |
| HikariCP Pending Threads | 5+ | 20+ |
| Redis Cache Hit Rate | <95% | <80% |
| CPU Usage | 70%+ | 90%+ |
| Memory Usage | 70%+ | 90%+ |
