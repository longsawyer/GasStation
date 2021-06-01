# 운영
## CI/CD 설정
### 빌드/배포(AWS)
각 프로젝트 jar를 Dockerfile을 통해 Docker Image 만들어 ECR저장소에 올린다.   
EKS 클러스터에 접속한 뒤, 각 서비스의 deployment.yaml, service.yaml을 kuectl명령어로 서비스를 배포한다.   
  - 코드 형상관리 : https://github.com/longsawyer/GasStation 하위 repository에 각각 구성   
  - 운영 플랫폼 : AWS의 EKS(Elastic Kubernetes Service)   
  - Docker Image 저장소 : AWS의 ECR(Elastic Container Registry)
  
##### 배포 명령어(AWS)
```
cd d:\projects\gasstation\Order\
mvn package -B
docker build -t laios/order:1 .
docker push laios/order:1
cd d:\projects\gasstation\Order\kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd d:\projects\gasstation\Station\
mvn package -B
docker build -t laios/station:1 .
docker push laios/station:1
cd d:\projects\gasstation\station\kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd d:\projects\gasstation\POS\
mvn package -B
docker build -t laios/pos:1 .
docker push laios/pos:1
cd d:\projects\gasstation\pos\kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd d:\projects\gasstation\Logistics\
mvn package -B
docker build -t laios/logistics:1 .
docker push laios/logistics:1
cd d:\projects\gasstation\logistics\kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd d:\projects\gasstation\Gateway\
mvn package -B
docker build -t laios/Gateway:1.
docker push laios/Gateway:1
kubectl create deploy gateway --image=laios/gateway:1
kubectl expose deployment gateway --type=LoadBalancer --port=8080
```

##### 배포 결과(AWS)
![image](https://user-images.githubusercontent.com/76420081/119082405-fa95fa80-ba38-11eb-8ad5-c7cd5b4f736a.png)


## 동기식호출 /서킷브레이킹 /장애격리
- 서킷 브레이킹 프레임워크의 선택
  - Spring FeignClient + Hystrix 옵션을 사용하여 구현할 경우, 도메인 로직과 부가 기능 로직이 서비스에 같이 구현된다.
  - istio를 사용해서 서킷 브레이킹 적용이 가능하다.
- istio 설치

![image](https://user-images.githubusercontent.com/76420081/119083009-2665b000-ba3a-11eb-8a43-aeb9b7e7db98.png)
![image](https://user-images.githubusercontent.com/76420081/119083153-6331a700-ba3a-11eb-9543-475bb812c176.png)
![image](https://user-images.githubusercontent.com/76420081/119083538-1b5f4f80-ba3b-11eb-952d-89e7d7adec23.png)
http://acdf28d4a2a744330ad8f7db4e05aeac-1896393867.ap-southeast-2.elb.amazonaws.com:20001/
![image](https://user-images.githubusercontent.com/76420081/119086647-c292b580-ba40-11eb-9450-7b47e4128157.png)

```
 root@labs--2007877942:/home/project# curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.7.1 TARGET_ARCH=x86_64 sh -
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   102  100   102    0     0    153      0 --:--:-- --:--:-- --:--:--   152
100  4573  100  4573    0     0   4880      0 --:--:-- --:--:-- --:--:--  4880

Downloading istio-1.7.1 from https://github.com/istio/istio/releases/download/1.7.1/istio-1.7.1-linux-amd64.tar.gz ...

Istio 1.7.1 Download Complete!

Istio has been successfully downloaded into the istio-1.7.1 folder on your system.

Next Steps:
See https://istio.io/latest/docs/setup/install/ to add Istio to your Kubernetes cluster.

To configure the istioctl client tool for your workstation,
add the /home/project/istio-1.7.1/bin directory to your environment path variable with:
         export PATH="$PATH:/home/project/istio-1.7.1/bin"

Begin the Istio pre-installation check by running:
         istioctl x precheck 

Need more information? Visit https://istio.io/latest/docs/setup/install/ 
root@labs--2007877942:/home/project# ㅣㅣ
bash: ㅣㅣ: command not found
root@labs--2007877942:/home/project# ll
total 24
drwxr-xr-x 4 root root  6144 May 21 04:37 ./
drwxrwxr-x 1 root root    19 May  3 04:35 ../
-rwx------ 1 root root 11248 May 21 03:06 get_helm.sh*
drwxr-x--- 6 root root  6144 Sep  9  2020 istio-1.7.1/
drwxr-xr-x 4 root root  6144 May 21 02:37 team/
root@labs--2007877942:/home/project# cd istio-1.7.1/
root@labs--2007877942:/home/project/istio-1.7.1# export PATH=$PWD/bin:$PATH
root@labs--2007877942:/home/project/istio-1.7.1# istioctl install --set profile=demo --set hub=gcr.io/istio-release

Istio core installed                                                                            
Istiod installed                                                                                
Ingress gateways installed                                                                                                                                                   
Egress gateways installed                                                                                                                                                       
Installation complete                                                                                                                 
```

- istio 에서 서킷브레이커 설정(DestinationRule)
```
cat <<EOF | kubectl apply -f -
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: order
spec:
  host: order
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 1           # 목적지로 가는 HTTP, TCP connection 최대 값. (Default 1024)
      http:
        http1MaxPendingRequests: 1  # 연결을 기다리는 request 수를 1개로 제한 (Default 
        maxRequestsPerConnection: 1 # keep alive 기능 disable
        maxRetries: 3               # 기다리는 동안 최대 재시도 수(Default 1024)
    outlierDetection:
      consecutiveErrors: 5          # 5xx 에러가 5번 발생하면
      interval: 1s                  # 1초마다 스캔 하여
      baseEjectionTime: 30s         # 30 초 동안 circuit breaking 처리   
      maxEjectionPercent: 100       # 100% 로 차단
EOF

```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작을 확인한다.
- 동시사용자 100명
- 60초 동안 실시
- 결과 화면
![image](https://user-images.githubusercontent.com/76420081/119089217-c32d4b00-ba44-11eb-8038-9c86b9c92897.png)
![kiali](https://user-images.githubusercontent.com/81946287/119092566-8b74d200-ba49-11eb-8ce1-e38ebfcacd13.png)

### Liveness
pod의 container가 정상적으로 기동되는지 확인하여, 비정상 상태인 경우 pod를 재기동하도록 한다.   

아래의 값으로 liveness를 설정한다.
- 재기동 제어값 : /tmp/healthy 파일의 존재를 확인
- 기동 대기 시간 : 3초
- 재기동 횟수 : 5번까지 재시도

이때, 재기동 제어값인 /tmp/healthy파일을 강제로 지워 liveness가 pod를 비정상 상태라고 판단하도록 하였다.    
5번 재시도 후에도 파드가 뜨지 않았을 경우 CrashLoopBackOff 상태가 됨을 확인하였다.   

##### order에 Liveness 적용한 내용
```yaml
apiVersion: apps/v1
kind: Deployment
...
    spec:
      containers:
        - name: order
          image: 740569282574.dkr.ecr.ap-southeast-2.amazonaws.com/puri-order:v3
          args:
          - /bin/sh
          - -c
          - touch /tmp/healthy; sleep 10; rm -rf /tmp/healthy; sleep 600;
...
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 3
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```



### 오토스케일 아웃
- 주문 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 1프로를 넘어서면 replica 를 10개까지 늘려준다.
```
kubectl autoscale deploy order --min=1 --max=10 --cpu-percent=1
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어준다.
```
kubectl get deploy order -w
kubectl get hpa order -w
```

- 사용자 50명으로 워크로드를 3분 동안 걸어준다.
```
siege -c50 -t180S  -v 'http://a39e59e8f1e324d23b5546d96364dc45-974312121.ap-southeast-2.elb.amazonaws.com:8080/order/joinOrder POST productId=4&productName=PURI4&installationAddress=Dongtan&customerId=504'

```

- 오토스케일 발생하지 않음(siege 실행 결과 오류 없이 수행됨 : Availability 100%)
- 서비스에 복잡한 비즈니스 로직이 포함된 것이 아니어서, CPU 부하를 주지 못한 것으로 추정된다.

![image](https://user-images.githubusercontent.com/76420081/119087445-1ce04600-ba42-11eb-92c8-2f0e2d772562.png)


## 무정지 재배포
* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 서킷브레이커 설정을 제거함
- seige 로 배포작업 직전에 워크로드를 모니터링 한다.
```
siege -c50 -t180S  -v 'http://a39e59e8f1e324d23b5546d96364dc45-974312121.ap-southeast-2.elb.amazonaws.com:8080/order/joinOrder POST productId=4&productName=PURI4&installationAddress=Dongtan&customerId=504'
```

- readinessProbe, livenessProbe 설정되지 않은 상태로 buildspec.yml을 수정한다.
- Github에 buildspec.yml 수정 발생으로 CodeBuild 자동 빌드/배포 수행된다.
- siege 수행 결과 : 

- readinessProbe, livenessProbe 설정하고 buildspec.yml을 수정한다.
- Github에 buildspec.yml 수정 발생으로 CodeBuild 자동 빌드/배포 수행된다.
- siege 수행 결과 : 


## ConfigMap 적용
- 설정의 외부 주입을 통한 유연성을 제공하기 위해 ConfigMap을 적용한다.
- orderstatus 에서 사용하는 mySQL(AWS RDS 활용) 접속 정보를 ConfigMap을 통해 주입 받는다.

```
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: order
data:
  urlstatus: "jdbc:mysql://order.cgzkudckye4b.ap-southeast-2:3306/orderstatus?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8"
EOF
```

## Secret 적용
- username, password와 같은 민감한 정보는 ConfigMap이 아닌 Secret을 적용한다.
- etcd에 암호화 되어 저장되어, ConfigMap 보다 안전하다.
- value는 base64 인코딩 된 값으로 지정한다. (echo root | base64)

```
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Secret
metadata:
  name: order
type: Opaque
data:
  username: xxxxx <- 보안 상, 임의의 값으로 표시함 
  password: xxxxx <- 보안 상, 임의의 값으로 표시함
EOF
```


## 운영 모니터링

### 쿠버네티스 구조
쿠버네티스는 Master Node(Control Plane)와 Worker Node로 구성된다.
![image](https://user-images.githubusercontent.com/64656963/86503139-09a29880-bde6-11ea-8706-1bba1f24d22d.png)


### 1. Master Node(Control Plane) 모니터링
Amazon EKS 제어 플레인 모니터링/로깅은 Amazon EKS 제어 플레인에서 계정의 CloudWatch Logs로 감사 및 진단 로그를 직접 제공한다.

- 사용할 수 있는 클러스터 제어 플레인 로그 유형은 다음과 같다.
```
  - Kubernetes API 서버 컴포넌트 로그(api)
  - 감사(audit) 
  - 인증자(authenticator) 
  - 컨트롤러 관리자(controllerManager)
  - 스케줄러(scheduler)

출처 : https://docs.aws.amazon.com/ko_kr/eks/latest/userguide/logging-monitoring.html
```

- 제어 플레인 로그 활성화 및 비활성화
```
기본적으로 클러스터 제어 플레인 로그는 CloudWatch Logs로 전송되지 않습니다. 
클러스터에 대해 로그를 전송하려면 각 로그 유형을 개별적으로 활성화해야 합니다. 
CloudWatch Logs 수집, 아카이브 스토리지 및 데이터 스캔 요금이 활성화된 제어 플레인 로그에 적용됩니다.

출처 : https://docs.aws.amazon.com/ko_kr/eks/latest/userguide/control-plane-logs.html
```

### 2. Worker Node 모니터링

- 쿠버네티스 모니터링 솔루션 중에 가장 인기 많은 것은 Heapster와 Prometheus 이다.
- Heapster는 쿠버네티스에서 기본적으로 제공이 되며, 클러스터 내의 모니터링과 이벤트 데이터를 수집한다.
- Prometheus는 CNCF에 의해 제공이 되며, 쿠버네티스의 각 다른 객체와 구성으로부터 리소스 사용을 수집할 수 있다.

- 쿠버네티스에서 로그를 수집하는 가장 흔한 방법은 fluentd를 사용하는 Elasticsearch 이며, fluentd는 node에서 에이전트로 작동하며 커스텀 설정이 가능하다.

- 그 외 오픈소스를 활용하여 Worker Node 모니터링이 가능하다. 아래는 istio, mixer, grafana, kiali를 사용한 예이다.

```
아래 내용 출처: https://bcho.tistory.com/1296?category=731548

```
- 마이크로 서비스에서 문제점중의 하나는 서비스가 많아 지면서 어떤 서비스가 어떤 서비스를 부르는지 의존성을 알기가 어렵고, 각 서비스를 개별적으로 모니터링 하기가 어렵다는 문제가 있다. Istio는 네트워크 트래픽을 모니터링함으로써, 서비스간에 호출 관계가 어떻게 되고, 서비스의 응답 시간, 처리량등의 다양한 지표를 수집하여 모니터링할 수 있다.

![image](https://user-images.githubusercontent.com/64656963/86347967-ff738380-bc99-11ea-9b5e-6fb94dd4107a.png)

- 서비스 A가 서비스 B를 호출할때 호출 트래픽은 각각의 envoy 프록시를 통하게 되고, 호출을 할때, 응답 시간과 서비스의 처리량이 Mixer로 전달된다. 전달된 각종 지표는 Mixer에 연결된 Logging Backend에 저장된다.

- Mixer는 위의 그림과 같이 플러그인이 가능한 아답터 구조로, 운영하는 인프라에 맞춰서 로깅 및 모니터링 시스템을 손쉽게 변환이 가능하다.  쿠버네티스에서 많이 사용되는 Heapster나 Prometheus에서 부터 구글 클라우드의 StackDriver 그리고, 전문 모니터링 서비스인 Datadog 등으로 저장이 가능하다.

![image](https://user-images.githubusercontent.com/64656963/86348023-14501700-bc9a-11ea-9759-a40679a6a61b.png)

- 이렇게 저장된 지표들은 여러 시각화 도구를 이용해서 시각화 될 수 있는데, 아래 그림은 Grafana를 이용해서 서비스의 지표를 시각화 한 그림이다.

![image](https://user-images.githubusercontent.com/64656963/86348092-25992380-bc9a-11ea-9d7b-8a7cdedc11fc.png)

- 그리고 근래에 소개된 오픈소스 중에서 흥미로운 오픈 소스중의 하나가 Kiali (https://www.kiali.io/)라는 오픈소스인데, Istio에 의해서 수집된 각종 지표를 기반으로, 서비스간의 관계를 아래 그림과 같이 시각화하여 나타낼 수 있다.  아래는 그림이라서 움직이는 모습이 보이지 않지만 실제로 트래픽이 흘러가는 경로로 에니메이션을 이용하여 표현하고 있고, 서비스의 각종 지표, 처리량, 정상 여부, 응답 시간등을 손쉽게 표현해 준다.

![image](https://user-images.githubusercontent.com/64656963/86348145-3a75b700-bc9a-11ea-8477-e7e7178c51fe.png)


# 시연
 1. 주문/재고주문신청 -> 물류/배송처리 -> 점포/입고예정처리
 2. 점포/입고완료처리 -> 주문/배송완료(received)처리
 3. POS/판매처리 -> 점포/재고감소,판매집계
 4. POS/판매취소 -> 점포/보상처리:재고증가,판매집계취소
 5. EDA 구현
   - Order(본사)시스템 장애상황에서 POS판매처리 정상처리
   - Order(본사)시스템 정상 전환시, 수신받지 못한 이벤트 처리 예)가격변경
 6. 무정지 재배포
 7. 오토 스케일링