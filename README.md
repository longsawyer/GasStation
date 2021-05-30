# GasStation Project
- 주유소 판매/물류 프로젝트입니다
<p align="left"><img src="https://user-images.githubusercontent.com/76420081/120092243-5b65b700-c14c-11eb-8356-03083e54f0c2.png"></p>

# Table of contents

- [GasStation Project (주유소 판매시스템)](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

# 서비스 시나리오

개요
1. 주유소 관련 주문/주문취소/상품마스터/판매 업무처리를 한다
2. 시스템
    - 주유소 점포시스템: BOS - BackOffice System
    - 주유소 판매시스템: POS - Point Of Sale
    - 주문시스템: order Market
    - 물류시스템: Logistics 
      - 본래는 배차업무와 배송은 별개업무임
      - 배송회사(예:글로비스)와 물류회사(예:송유관공사)는 별개로 존재함
      - 그러나 구현모델을 간단하게 하기 위해서 합친다
    - 구현범위 외( 현 구현범위 아님)
      - 위틱수수료정산
      - 배차
      - 본사ERP
      - 비고
        - 구 시스템들 역시 MSMQ로 시스템이 격리되어 있으며, 어찌보면 옛날기술도 구현된 MSA로 볼 수도 있다
        - 타 시스템들이 장애생겨도, 각각 독립운영가능했었다 (실 현업요구사항이였음)

![image](https://user-images.githubusercontent.com/76420081/120093071-af739a00-c152-11eb-89cf-e3232023e7bd.png)


기능적 요구사항
1. 유류입고
    - 점포담당자는 주문시스템을 통해서 주문한다
    - 주문시스템에선 물류시스템에 배차와 배송을 동시에 요구하나, 여기선 하나로 합쳐서 배송으로 처리한다
    - 배차된 유류차는 물류기지에서 점포까지 배송하며, 이는 배송(shipment)로 표현한다
    - 배송처리가 되면 주유소에선 입고예정자료를 받아볼 수 있다
    - 유류차가 도착하여 배송물량을 확인하고 입고되면, 점포시스템 최종 입고확정하며, 입고재고로 확정된다
2. 상품정보
    - 상품정보는 상품원장 or 상품마스터하며 판매자료의 근거가 된다
    - 상품원장은 본래 ERP 등 본사스템에서 관리하나, 여기에서는 주문시스템이 본사시스템을 겸한다
    - 상품원장의 가격정보가 변경되면, 주문시스템->점포시스템->POS 순으로 변경정보가 반영된다
3. 판매
    - POS시스템에서 고객과 대면하며 판매처리한다.
    - 판매처리되면, 즉시 재고감소와 판매집계를 해야한다. (Req/Res 테스트를 위한 동기처리)
    - 판매취소할 수 있으며, 판매취소되면 재고는 다시 증가, 판매집계에선 제외되야 한다.
        - 금융에선 이를 보상처리, 물류에선 자료보정처리라고 한다.
4. 계정(추후구현범위, 현범위 아님)
    - 점포에 있는 계정은 외상장부의 표현이다. 
    - 계정은 외상계정이며 외상의 금액(잔액)은 곧 고객에게 받을돈=미수금=채권이다.
    - 매출은 곧 외상의 발생이다. 
    - 외상계정의 외상잔액은 입금처리하면 감소한다
    - 현금또한 외상계정으로 처리하며, 현금은 외상발생과 동시에 입금처리된다

비기능적 요구사항
1. 트랜잭션
    - 판매처리와 동시에, 점포시스템의 재고감소처리한다
    - 판매취소와 동시에, 점포시스템의 재고증가처리한다
1. 장애격리
    - 점포시스템은 본사시스템과 별개로 운영가능하다
    - 주문/배송 또한 별개 시스템
1. 성능
    - 주문자는 주문 진행상태를 수시로 확인한다 (CQRS)
    - 점포매니저는 재고총계/판매총계을 수시로 확인한다 (CQRS)


# 체크포인트

- 분석 설계
  - 이벤트스토밍: 
    - 스티커 색상별 객체의미를 제대로 이해하여 헥사고날 아키텍처의 연계설계에 반영하고 있는가?
    - 각 도메인 이벤트가 의미있게 정의되었는가?
    - 어그리게잇: Command와 Event를 ACID 트랜잭션 단위의 Aggregate로 제대로 묶었는가?
    - 기능요구사항과 비기능요구사항을 누락 없이 반영하였는가?    
  - 서브 도메인, 바운디드 컨텍스트 분리
    - 항목 
      - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  
      - Sub-domain이나 Bounded Context를 분리하였고 그 분리기준이 합리적인가?
      - 3개 이상 서비스 분리
    - 폴리글랏 설계: 
      - 각 마이크로 서비스들의 구현목표와 기능특성에 따른 각자의 기술 Stack과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과 도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?
  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern과 Repository Pattern을 적용하여 JPA를 사용한 데이터 접근 어댑터를 개발하였는가?
    - [헥사고날 아키텍처] 
      - REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 
      - 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지(업무현장에서 쓰는 용어)를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? 
      - Service Discovery, REST, FeignClient
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key
      - 각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 
      - Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out
      - Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS
      - Materialized View 를 구현하여
      - 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이)도 
      - 내 서비스의 화면 구성과 잦은 조회가 가능한가?
  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형을 선택하여 구현하였는가?
      - RDB, NoSQL, File System 등
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링
      - Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 
      - 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 
      - 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?

# 분석/설계


## AS-IS 조직 (Horizontally-Aligned)
![as_is](https://user-images.githubusercontent.com/76420081/120093786-886b9700-c157-11eb-9775-fd865b4cb781.png)

## TO-BE 조직 (Vertically-Aligned)
![to_be](https://user-images.githubusercontent.com/76420081/120093800-9ae5d080-c157-11eb-8058-99b87b60bddd.png)


## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  
  - [GasStation.zip](https://github.com/longsawyer/GasStation/files/6565788/GasStation.zip)

### 이벤트
![Event](https://user-images.githubusercontent.com/76420081/120093917-4e4ec500-c158-11eb-9764-8414454df296.png)

### 어그리게잇
![Policy](https://user-images.githubusercontent.com/76420081/120093951-7807ec00-c158-11eb-9b06-b73fe786264d.png)
- 처리내역
  - 주문, 재고, 계정, 판매, 배송 등을 정의함

### 폴리시,커맨드
![Command](https://user-images.githubusercontent.com/76420081/120093981-a38ad680-c158-11eb-8291-0f90f28e126a.png)
![Command](https://user-images.githubusercontent.com/76420081/120094000-b9989700-c158-11eb-99dc-9461762d92e7.png)

### 액터
![actor](https://user-images.githubusercontent.com/76420081/120094009-c5845900-c158-11eb-86c0-d35a72043817.png)

### 바운디드 컨텍스트, 컨텍스트 매핑 (파란색점선 Pub/Sub, 빨간색실선 Req/Res)
![BoundedContext](https://user-images.githubusercontent.com/76420081/120094015-d2a14800-c158-11eb-9431-7ae2b46f8779.png)
-처리내역
  - 도메인 서열 분리 : 점포->주문-> 물류->판매 순으로 정리
       
### 1차 완료
![firstDesign](https://user-images.githubusercontent.com/76420081/120094067-22800f00-c159-11eb-8572-0c2be3148bdc.png)

### 2차 수정
![2ndDesign](https://user-images.githubusercontent.com/76420081/120094078-375ca280-c159-11eb-9585-e9b75b84611f.png)
    - 판매취소에 대한 보상처리 추가

### 요구사항 검증 (기능적/비기능적 )
#### 주문(기능)
![1stReview](https://user-images.githubusercontent.com/76420081/120095402-cf11bf00-c160-11eb-81cc-8c05c45744dc.png)
- 처리내역
    - 주문에 따른 입고예정까지 잘 오는지 확인
    - 최종 입고확정시, 주문시스템의 주문상태 변경되는지 확인

#### 상품마스터(기능)
![2ndReview](https://user-images.githubusercontent.com/76420081/120095417-ddf87180-c160-11eb-9845-bbde16214946.png)
- 처리내역
    - 본사(주문)시스템에서 가격정책 변경시, 점포=>판매시스템까지 잘 변경되는지 확인
    
#### 상품마스터(기능)
![2ndReview](https://user-images.githubusercontent.com/76420081/120094350-ccac6680-c15a-11eb-8c51-75855ca1eb5b.png)
- 처리내역
    - 본사(주문)시스템에서 가격정책 변경시, 점포=>판매시스템까지 잘 변경되는지 확인
    
#### 판매처리(비기능)
![3rdReview](https://user-images.githubusercontent.com/76420081/120095251-edc38600-c15f-11eb-85c8-51ee546f242e.png)
- 처리내역
    - 판매 즉시 재고에 반영
    - 점포시스템등은 본사와 별개로 운영가능

#### 판매취소에 따른 보상처리
![3rdReview](https://user-images.githubusercontent.com/76420081/120095305-33804e80-c160-11eb-9f47-63e337db2c01.png)
- 처리내역
    - 판매취소되면, 보상처리로 취소된 판매분만큼 재고를 증가시킨다
    - 판매취소되면, 보상처리로 취소된 판매분만큼 매출집계에서 제외한다

## 헥사고날 아키텍처 다이어그램 도출
![hexagonal1](https://user-images.githubusercontent.com/76420081/120095897-7c85d200-c163-11eb-868d-a802b71a1386.png)


## 신규 서비스 추가 시 기존 서비스에 영향이 없도록 열린 아키택처 설계
- 신규 개발 조직 추가시
  - 기존의 마이크로 서비스에 수정이 발생하지 않도록 Inbund 요청을 REST 가 아닌 Event를 Subscribe 하는 방식으로 구현하였다.
- 기존 마이크로 서비스에 대하여 아키텍처, 데이터베이스 구조와 관계 없이 추가할 수 있다.
- 예시는, 위탁수수료 정산시스템이다
  - 매장의 종류에는 가맹점과 직영점이 있다
  - 직영점의 경우 본사로부터 운영을 위탁맡은 관리인이 운영한다
  - 위탁관리인은 판매내역에 근거하여, 매장운영에 대한 대가를 위탁수수료를 정산받는다

![hexagonal2](https://user-images.githubusercontent.com/76420081/120096055-4a28a480-c164-11eb-86d6-4fc52c86db3e.png)



# 구현:
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8083 이다)

```
- Local
	cd Order
	mvn spring-boot:run

	cd Station
	mvn spring-boot:run

	cd POS
	mvn spring-boot:run
	
	cd Logistics
	mvn spring-boot:run


- EKS : CI/CD 통해 빌드/배포 ("운영 > CI-CD 설정" 부분 참조)
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다
  - Order, Shipment, StockFlow, Sale, Product, Account
- Order(주문) 마이크로서비스 예시

```
package gasstation;
...
import gasstation.event.Ordered;

/**
 * 주문
 * @author Administrator
 */
@Entity
@Table(name="T_ORDER")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long 	orderId;
    private String 	productId;		// 유종
    private String 	productName;	// 유종명
    private Double 	qty;			// 수량			
    private String 	destAddr;		// 배송지
    private String 	orderDate;		// 주문일자
    
    @PostPersist
    public void onPostPersist(){
        Ordered ordered = new Ordered();
        BeanUtils.copyProperties(this, ordered);
        ordered.publishAfterCommit();
    }
   
    ...
}
```

REST API 테스트
1) 휘발유 주문 & 입고확정
- (a) http -f POST http://localhost:8083/orders/placeOrder productId=CD1001 qty=20000 destAddr="SK Imme Station"
- (b) http -f POST http://localhost:8082/stocks/confirmStock orderId=1
![image](https://user-images.githubusercontent.com/76420081/118930671-00c8a000-b981-11eb-9af5-3619d4ceaedd.png)

2) 카프카 메시지 확인
- (a) 서비스 신청 후 : JoinOrdered -> EngineerAssigned -> InstallationAccepted
- (b) 설치완료 처리 후 : InstallationCompleted
![image](https://user-images.githubusercontent.com/76420081/118930569-df67b400-b980-11eb-8ad2-66e33a3a5993.png)


## 폴리글랏 퍼시스턴스
- order, Assignment, installation 서비스 모두 H2 메모리DB를 적용하였다.  
다양한 데이터소스 유형 (RDB or NoSQL) 적용 시 데이터 객체에 @Entity 가 아닌 @Document로 마킹 후, 기존의 Entity Pattern / Repository Pattern 적용과 데이터베이스 제품의 설정 (application.yml) 만으로 가능하다.

```
--application.yml // mariaDB 추가 예시
spring:
  profiles: real-db
  datasource:
        url: jdbc:mariadb://rds주소:포트명(기본은 3306)/database명
        username: db계정
        password: db계정 비밀번호
        driver-class-name: org.mariadb.jdbc.Driver
```

## 동기식 호출 과 Fallback 처리

- 분석 단계에서의 조건 중 하나로 배정(Assignment) 서비스에서 인터넷 가입신청 취소를 요청 받으면, 
설치(installation) 서비스 취소 처리하는 부분을 동기식 호출하는 트랜잭션으로 처리하기로 하였다. 
- 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어 있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

설치 서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현
```
# (Assignment) InstallationService.java

	package purifierrentalpjt.external;
	
	import org.springframework.cloud.openfeign.FeignClient;
	import org.springframework.web.bind.annotation.RequestBody;
	import org.springframework.web.bind.annotation.RequestMapping;
	import org.springframework.web.bind.annotation.RequestMethod;

	/**
 	 * 설치subsystem 동기호출
 	 * @author Administrator
 	 * 아래 주소는 Gateway주소임
 	*/


	@FeignClient(name="Installation", url="http://installation:8080")
	//@FeignClient(name="Installation", url="http://localhost:8083")
	public interface InstallationService {

		@RequestMapping(method= RequestMethod.POST, path="/installations")
    		public void cancelInstallation(@RequestBody Installation installation);

	}
```

정수기 렌탈 서비스 가입 취소 요청(cancelRequest)을 받은 후, 처리하는 부분
```
# (Installation) InstallationController.java

	package purifierrentalpjt;

	@RestController
	public class InstallationController {

    	  @Autowired
    	  InstallationRepository installationRepository;

    	  /**
     	   * 설치취소
     	   * @param installation
           */
	  @RequestMapping(method=RequestMethod.POST, path="/installations")
    	  public void installationCancellation(@RequestBody Installation installation) {
    	
    		System.out.println( "### 동기호출 -설치취소=" +ToStringBuilder.reflectionToString(installation) );

    		Optional<Installation> opt = installationRepository.findByOrderId(installation.getOrderId());
    		if( opt.isPresent()) {
    			Installation installationCancel =opt.get();
    			installationCancel.setStatus("installationCanceled");
    			installationRepository.save(installationCancel);
    		} else {
    			System.out.println("### 설치취소 - 못찾음");
    		}
    	}
```

## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트

가입 신청(order)이 이루어진 후에 배정(Assignment) 서비스로 이를 알려주는 행위는 비동기식으로 처리하여, 배정(Assignment) 서비스의 처리를 위하여 가입신청(order)이 블로킹 되지 않도록 처리한다.
 
- 이를 위하여 가입 신청에 기록을 남긴 후에 곧바로 가입 신청이 되었다는 도메인 이벤트를 카프카로 송출한다.(Publish)
```
# (order) Order.java

    @PostPersist
    public void onPostPersist(){

        JoinOrdered joinOrdered = new JoinOrdered();
        BeanUtils.copyProperties(this, joinOrdered);
        joinOrdered.publishAfterCommit();
    }
```
- 배정 서비스에서는 가입신청 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다.
```
# (Assignment) PolicyHandler.java

@Service
public class PolicyHandler{
    @Autowired AssignmentRepository assignmentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverJoinOrdered_OrderRequest(@Payload JoinOrdered joinOrdered){

        if(!joinOrdered.validate()) return;

        System.out.println("\n\n##### listener OrderRequest : " + joinOrdered.toJson() + "\n\n");

        Assignment assignment = new Assignment();

        assignment.setId(joinOrdered.getId());
        assignment.setInstallationAddress(joinOrdered.getInstallationAddress());
        assignment.setStatus("orderRequest");
        assignment.setEngineerName("Enginner" + joinOrdered.getId());
        assignment.setEngineerId(joinOrdered.getId());
        assignment.setOrderId(joinOrdered.getId());

        assignmentRepository.save(assignment);
        }
    }
}
```
가입신청은 배정 서비스와 완전히 분리되어 있으며, 이벤트 수신에 따라 처리되기 때문에, 배정 서비스가 유지보수로 인해 잠시 내려간 상태라도 가입신청을 받는데 문제가 없다.


## CQRS

가입신청 상태 조회를 위한 서비스를 CQRS 패턴으로 구현하였다.
- Order, Assignment, Installation 개별 aggregate 통합 조회로 인한 성능 저하를 막을 수 있다.
- 모든 정보는 비동기 방식으로 발행된 이벤트를 수신하여 처리된다.
- 설계 : MSAEz 설계의 view 매핑 설정 참조

- 주문생성

![image](https://user-images.githubusercontent.com/76420081/119001165-b23df480-b9c6-11eb-9d62-bed7406f0709.png)

- 카프카 메시지

![image](https://user-images.githubusercontent.com/76420081/119001370-df8aa280-b9c6-11eb-867f-fbd78ab89031.png)

- 주문취소

![image](https://user-images.githubusercontent.com/76420081/119001667-25476b00-b9c7-11eb-8609-c6a7e9a02dfe.png)

- 카프카 메시지

![image](https://user-images.githubusercontent.com/76420081/119001720-32645a00-b9c7-11eb-81aa-58191e7bef1d.png)

- 뷰테이블 수신처리

![image](https://user-images.githubusercontent.com/76420081/119002598-fa114b80-b9c7-11eb-9aac-ed6ac136be4c.png)


## API Gateway

API Gateway를 통하여, 마이크로 서비스들의 진입점을 통일한다.

```
# application.yml 파일에 라우팅 경로 설정

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: Order
          uri: http://localhost:8081
          predicates:
            - Path=/orders/**,/order/**,/orderStatuses/**
        - id: Assignment
          uri: http://localhost:8082
          predicates:
            - Path=/assignments/**,/assignment/** 
        - id: Installation
          uri: http://localhost:8083
          predicates:
            - Path=/installations/**,/installation/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
```

- EKS에 배포 시, MSA는 Service type을 ClusterIP(default)로 설정하여, 클러스터 내부에서만 호출 가능하도록 한다.
- API Gateway는 Service type을 LoadBalancer로 설정하여 외부 호출에 대한 라우팅을 처리한다.



# 운영

## CI/CD 설정
### 빌드/배포
각 프로젝트 jar를 Dockerfile을 통해 Docker Image 만들어 ECR저장소에 올린다.   
EKS 클러스터에 접속한 뒤, 각 서비스의 deployment.yaml, service.yaml을 kuectl명령어로 서비스를 배포한다.   
  - 코드 형상관리 : https://github.com/llyyjj99/PurifierRentalPJT 하위 repository에 각각 구성   
  - 운영 플랫폼 : AWS의 EKS(Elastic Kubernetes Service)   
  - Docker Image 저장소 : AWS의 ECR(Elastic Container Registry)
##### 배포 명령어
```
$ kubectl apply -f deployment.yml
$ kubectl apply -f service.yaml
```

##### 배포 결과
![image](https://user-images.githubusercontent.com/76420081/119082405-fa95fa80-ba38-11eb-8ad5-c7cd5b4f736a.png)

## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이킹 프레임워크의 선택
  - Spring FeignClient + Hystrix 옵션을 사용하여 구현할 경우, 도메인 로직과 부가 기능 로직이 서비스에 같이 구현된다.
  - istio를 사용해서 서킷 브레이킹 적용이 가능하다.

- istio 설치


![image](https://user-images.githubusercontent.com/76420081/119083009-2665b000-ba3a-11eb-8a43-aeb9b7e7db98.png)

![image](https://user-images.githubusercontent.com/76420081/119083153-6331a700-ba3a-11eb-9543-475bb812c176.png)

![image](https://user-images.githubusercontent.com/76420081/119083538-1b5f4f80-ba3b-11eb-952d-89e7d7adec23.png)
http://acdf28d4a2a744330ad8f7db4e05aeac-1896393867.ap-southeast-2.elb.amazonaws.com:20001/

![image](https://user-images.githubusercontent.com/76420081/119086647-c292b580-ba40-11eb-9450-7b47e4128157.png)


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

✔ Istio core installed                                                                            
✔ Istiod installed                                                                                
✔ Ingress gateways installed                                                                                                                                                                                         
✔ Egress gateways installed                                                                                                                                                                                          
✔ Installation complete                                                                                                                 


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

- 가입신청 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 1프로를 넘어서면 replica 를 10개까지 늘려준다.
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
 1. 정수기 렌탈 서비스 가입신청 -> installation 접수 완료 상태
 2. 설치 기사 설치 완료 처리 -> 가입 신청 완료 상태
 3. 가입 취소
 4. EDA 구현
   - Assignment 장애 상황에서 order(가입 신청) 정상 처리
   - Assignment 정상 전환 시 수신 받지 못한 이벤트 처리
 5. 무정지 재배포
 6. 오토 스케일링
