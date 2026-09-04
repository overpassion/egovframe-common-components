package egovframework.com.sym.ccm.cde.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.egovframe.rte.fdl.cmmn.code.EgovCode;
import org.egovframe.rte.fdl.cmmn.code.EgovCodeCache;
import org.egovframe.rte.fdl.cmmn.code.EgovCodeLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import egovframework.com.sym.ccm.cde.service.CmmnDetailCodeVO;

/**
 * 공통상세코드 저장 뒤 코드 캐시 갱신 시점 검증 — DB 없이 트랜잭션 동기화만 확인한다.
 *
 * <p>갱신을 트랜잭션 <b>안</b>에서 하면 안 되는 이유가 있다. {@code context-transaction.xml} 이
 * {@code egovframework.com..*Impl.*(..)} 를 트랜잭션으로 감싸고, 캐시 로더가 쓰는
 * {@code egov.dataSource} 는 {@code dataSource} 의 별칭이라 같은 인스턴스다. 그래서
 * {@code JdbcTemplate} 이 진행 중인 트랜잭션에 합류해 <b>미커밋 행</b>까지 읽고,
 * 롤백되면 DB 에 없는 코드가 캐시에만 남는다. 실제 MySQL 에서 캐시 417건 vs DB 416건으로 재현했다.</p>
 *
 * <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.09.04  실행환경팀        최초 생성 (커밋 후 갱신 검증)
 * </pre>
 */
class EgovCcmCmmnDetailCodeManageServiceReloadTest {

	/** reload() 가 몇 번 불렸는지 센다. */
	private static final class CountingLoader implements EgovCodeLoader {
		private final AtomicInteger calls = new AtomicInteger();

		@Override
		public List<EgovCode> loadAll() {
			calls.incrementAndGet();
			return Collections.emptyList();
		}
	}

	/** DB 를 타지 않는 DAO. 저장 호출만 받아넘긴다. */
	private static final class StubDao extends CmmnDetailCodeManageDAO {
		@Override
		public void insertCmmnDetailCode(CmmnDetailCodeVO vo) {
			// DB 접근 없음
		}

		@Override
		public void updateCmmnDetailCode(CmmnDetailCodeVO vo) {
			// DB 접근 없음
		}

		@Override
		public void deleteCmmnDetailCode(CmmnDetailCodeVO vo) {
			// DB 접근 없음
		}
	}

	private EgovCcmCmmnDetailCodeManageServiceImpl service;
	private CountingLoader loader;

	/** 생성 시 1회 적재하므로, 이후 증가분만 reload 횟수다. */
	private int reloads() {
		return loader.calls.get() - 1;
	}

	@BeforeEach
	void setUp() throws Exception {
		loader = new CountingLoader();
		service = new EgovCcmCmmnDetailCodeManageServiceImpl();
		inject("cmmnDetailCodeManageDAO", new StubDao());
		inject("egovCodeCache", new EgovCodeCache(loader));
	}

	@AfterEach
	void tearDown() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	private void inject(String name, Object value) throws Exception {
		Field field = EgovCcmCmmnDetailCodeManageServiceImpl.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(service, value);
	}

	/** 트랜잭션 매니저가 커밋 후 호출하는 콜백을 흉내낸다. */
	private void fireCompletion(int status) {
		for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
			sync.afterCompletion(status);
		}
		TransactionSynchronizationManager.clearSynchronization();
	}

	@Test
	@DisplayName("트랜잭션이 없으면 저장 즉시 갱신한다")
	void 트랜잭션_없으면_즉시_갱신() throws Exception {
		service.insertCmmnDetailCode(new CmmnDetailCodeVO());

		assertEquals(1, reloads());
	}

	@Test
	@DisplayName("트랜잭션 안에서는 커밋 전까지 갱신하지 않는다 — 미커밋 행을 읽지 않도록")
	void 커밋_전에는_갱신하지_않는다() throws Exception {
		TransactionSynchronizationManager.initSynchronization();

		service.insertCmmnDetailCode(new CmmnDetailCodeVO());

		assertEquals(0, reloads(), "커밋 전에 갱신하면 미커밋 행이 캐시에 들어간다");
		assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size(),
				"갱신을 커밋 시점으로 미루는 콜백이 등록돼야 한다");
	}

	@Test
	@DisplayName("커밋되면 갱신한다")
	void 커밋되면_갱신한다() throws Exception {
		TransactionSynchronizationManager.initSynchronization();
		service.insertCmmnDetailCode(new CmmnDetailCodeVO());

		fireCompletion(TransactionSynchronization.STATUS_COMMITTED);

		assertEquals(1, reloads());
	}

	@Test
	@DisplayName("롤백되면 갱신하지 않는다 — 롤백된 코드가 캐시에 남으면 안 된다")
	void 롤백되면_갱신하지_않는다() throws Exception {
		TransactionSynchronizationManager.initSynchronization();
		service.updateCmmnDetailCode(new CmmnDetailCodeVO());

		fireCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

		assertEquals(0, reloads(), "롤백됐는데 갱신하면 DB 에 없는 코드가 캐시에 남는다");
	}

	@Test
	@DisplayName("삭제도 같은 규칙을 따른다")
	void 삭제도_커밋_후에만_갱신() throws Exception {
		TransactionSynchronizationManager.initSynchronization();
		service.deleteCmmnDetailCode(new CmmnDetailCodeVO());

		assertEquals(0, reloads());
		fireCompletion(TransactionSynchronization.STATUS_COMMITTED);
		assertEquals(1, reloads());
	}
}
