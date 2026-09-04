package egovframework.com.cmm.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.fdl.cmmn.code.EgovCode;
import org.egovframe.rte.fdl.cmmn.code.EgovCodeCache;
import org.egovframe.rte.fdl.cmmn.code.EgovInMemoryCodeLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import egovframework.com.cmm.ComDefaultCodeVO;
import egovframework.com.cmm.service.CmmnDetailCode;

/**
 * 공통코드 조회의 캐시 이전 검증 — DB 없이 매핑 계약만 확인한다.
 *
 * <p>{@link EgovCmmUseServiceImplTest} 는 Spring 컨텍스트와 DB 가 필요해 로컬에서 늘
 * 돌릴 수 없다. 이 테스트는 인메모리 로더로 캐시를 만들어, 캐시 이전이 <b>기존 VO 계약</b>을
 * 그대로 지키는지만 본다 — 호출부 231곳과 화면 131곳이 {@code code}·{@code codeNm} 을
 * 직접 참조하므로 이 계약이 깨지면 화면이 빈다.</p>
 *
 * <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.09.04  실행환경팀        최초 생성 (EgovCodeCache 이전 검증)
 * </pre>
 */
class EgovCmmUseServiceCodeCacheTest {

	private EgovCmmUseServiceImpl service;

	@BeforeEach
	void setUp() throws Exception {
		List<EgovCode> codes = Arrays.asList(
				new EgovCode("COM001", "01", "정상", "정상 상태", true, 1),
				new EgovCode("COM001", "02", "중지", "중지 상태", true, 2),
				new EgovCode("COM001", "09", "폐기", "폐기 상태", false, 9),
				new EgovCode("COM002", "A", "가", "가 항목", true, 1));

		EgovCodeCache cache = new EgovCodeCache(new EgovInMemoryCodeLoader(codes));

		service = new EgovCmmUseServiceImpl();
		Field field = EgovCmmUseServiceImpl.class.getDeclaredField("egovCodeCache");
		field.setAccessible(true);
		field.set(service, cache);
	}

	private ComDefaultCodeVO vo(String codeId) {
		ComDefaultCodeVO vo = new ComDefaultCodeVO();
		vo.setCodeId(codeId);
		return vo;
	}

	@Test
	@DisplayName("코드그룹으로 조회하면 기존 VO 형태로 돌아온다")
	void 기존_VO_계약() {
		List<CmmnDetailCode> result = service.selectCmmCodeDetail(vo("COM001"));

		assertEquals(2, result.size(), "사용 안 함(09)은 제외된다");
		assertEquals("COM001", result.get(0).getCodeId());
		assertEquals("01", result.get(0).getCode());
		assertEquals("정상", result.get(0).getCodeNm());
		assertEquals("정상 상태", result.get(0).getCodeDc());
	}

	@Test
	@DisplayName("사용 안 함 코드는 셀렉트박스에 나오지 않는다 — USE_AT='Y' 종전 조건과 동일")
	void 사용중만_반환() {
		List<CmmnDetailCode> result = service.selectCmmCodeDetail(vo("COM001"));

		for (CmmnDetailCode code : result) {
			assertTrue(!"09".equals(code.getCode()), "폐기 코드가 섞이면 안 된다");
		}
	}

	@Test
	@DisplayName("정렬 순서를 따른다")
	void 정렬_순서() {
		List<CmmnDetailCode> result = service.selectCmmCodeDetail(vo("COM001"));

		assertEquals("01", result.get(0).getCode());
		assertEquals("02", result.get(1).getCode());
	}

	@Test
	@DisplayName("없는 코드그룹은 빈 목록 — 예외를 던지지 않는다(화면이 깨지지 않도록)")
	void 없는_그룹은_빈_목록() {
		assertEquals(0, service.selectCmmCodeDetail(vo("NOT_EXIST")).size());
	}

	@Test
	@DisplayName("복수 그룹 조회도 그룹별로 담아 돌려준다 — 종전 N+1 이 사라진다")
	void 복수_그룹_조회() {
		List<ComDefaultCodeVO> vos = new ArrayList<>();
		vos.add(vo("COM001"));
		vos.add(vo("COM002"));

		Map<String, List<CmmnDetailCode>> map = service.selectCmmCodeDetails(vos);

		assertEquals(2, map.size());
		assertEquals(2, map.get("COM001").size());
		assertEquals(1, map.get("COM002").size());
		assertEquals("가", map.get("COM002").get(0).getCodeNm());
	}
}
