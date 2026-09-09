package egovframework.com.utl.fcc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 물리적 파일명 생성 계약 검증.
 *
 * <p>생성 방식을 {@code EgovFormBasedUUID.randomUUID()} 에서 JDK
 * {@link java.util.UUID#randomUUID()} 로 바꿨다. 두 구현은 알고리즘이 같지만,
 * <b>저장 파일명 형식</b>이 달라지면 이미 저장된 파일과 규칙이 어긋나므로 형식을 고정한다.</p>
 *
 * <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.09.04  실행환경팀        최초 생성 (JDK UUID 이전 검증)
 * </pre>
 */
class EgovFormBasedFileUtilTest {

	@AfterEach
	void resetLocale() {
		Locale.setDefault(Locale.KOREA);
	}

	@Test
	@DisplayName("하이픈 없는 32자리 대문자 16진 문자열이다")
	void 파일명_형식() {
		String name = EgovFormBasedFileUtil.getPhysicalFileName();

		assertEquals(32, name.length(), name);
		assertTrue(name.matches("[0-9A-F]{32}"), name);
	}

	@Test
	@DisplayName("호출마다 다른 값을 만든다")
	void 중복되지_않는다() {
		Set<String> names = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			names.add(EgovFormBasedFileUtil.getPhysicalFileName());
		}

		assertEquals(1000, names.size(), "1000회 생성에서 중복이 나오면 안 된다");
	}

	@Test
	@DisplayName("기본 로케일(Locale)이 바뀌어도 형식이 같다 — 대문자 변환이 로케일을 타지 않는다")
	void 로케일_무관() {
		Locale.setDefault(new Locale("tr", "TR"));

		String name = EgovFormBasedFileUtil.getPhysicalFileName();

		assertTrue(name.matches("[0-9A-F]{32}"), name);
	}

	@Test
	@DisplayName("종전 구현과 형식이 같다 — 이미 저장된 파일의 이름 규칙이 유지된다")
	void 종전_형식과_동일() {
		@SuppressWarnings("deprecation")
		String legacy = EgovFormBasedUUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
		String current = EgovFormBasedFileUtil.getPhysicalFileName();

		assertEquals(legacy.length(), current.length());
		assertTrue(current.matches("[0-9A-F]{32}"));
		assertTrue(legacy.matches("[0-9A-F]{32}"));
		assertNotEquals(legacy, current, "무작위이므로 값 자체는 달라야 한다");
	}
}
