package egovframework.com.cmm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 첨부파일 저장 파일명 생성(storedFileName)이 실행환경 EgovStoredFileNames 로 이전된 뒤의 계약을 고정한다.
 *
 * <p>종전 17자리 시각 문자열({@code yyyyMMddhhmmssSSS})은 12시간제라 오전·오후가 같은 이름을 만들고, 같은 밀리초의
 * 동시 업로드가 조용히 덮어써졌다. 이제 저장명은 {@code keyStr + UUID 32자(소문자 16진) + fileKey} 다.</p>
 */
class EgovFileMngUtilStoredFileNameTest {

	private static final Pattern SHAPE = Pattern.compile("^FILE_[0-9a-f]{32}1$");

	@Test
	@DisplayName("저장명은 접두 + UUID 32자 + 순번 형식이다")
	void shape() {
		String name = EgovFileMngUtil.storedFileName("FILE_", 1);

		assertTrue(SHAPE.matcher(name).matches(), name);
		assertEquals("FILE_".length() + 32 + 1, name.length());
	}

	@Test
	@DisplayName("연속 생성해도 이름이 충돌하지 않는다 — 종전 시각 기반 이름은 같은 밀리초면 같았다")
	void unique() {
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 2_000; i++) {
			assertTrue(seen.add(EgovFileMngUtil.storedFileName("FILE_", 0)), "중복 저장명");
		}
	}

	@Test
	@DisplayName("저장명에 시각이 드러나지 않는다 — 다음 이름을 추측할 수 없다")
	void notTimeBased() {
		String name = EgovFileMngUtil.storedFileName("FILE_", 0);
		String year = String.valueOf(java.time.Year.now().getValue());

		assertFalse(name.substring("FILE_".length()).startsWith(year), name);
	}

	@Test
	@DisplayName("접두가 없어도 동작한다 — 종전과 같이 그대로 이어 붙인다")
	void emptyPrefix() {
		String name = EgovFileMngUtil.storedFileName("", 7);

		assertTrue(name.matches("^[0-9a-f]{32}7$"), name);
	}
}
