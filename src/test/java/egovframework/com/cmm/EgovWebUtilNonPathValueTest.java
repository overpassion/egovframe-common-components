package egovframework.com.cmm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link EgovWebUtil#filePathBlackList(String)} 를 <b>파일 경로가 아닌 값</b>에 쓰면
 * 무슨 일이 벌어지는지 고정한다 — URL·IP·도메인 13곳에서 이 호출을 걷어낸 근거다.
 *
 * <p>이 메서드가 하는 일은 {@code ".."} 삭제 하나뿐이다. 그래서</p>
 * <ul>
 *   <li>URL·도메인에는 <b>방어 효과가 0</b> 이면서 값을 조용히 바꾼다.</li>
 *   <li>IP 에는 아무 일도 하지 않는다 — 형식 검증이 아니다.</li>
 *   <li>뒤에 <b>화이트리스트 완전일치</b> 검사가 오는 자리에서는
 *       <b>없던 일치를 만들어낼 수 있다</b> — 방어가 아니라 우회 통로다.</li>
 * </ul>
 *
 * <p>경로 조작 차단이 필요한 자리는 {@code EgovFiles.resolveSecurely} 를 쓴다
 * (거부하지, 지우지 않는다).</p>
 *
 * <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.09.04  실행환경팀        최초 생성 (비경로 값 오용 제거 근거)
 * </pre>
 */
class EgovWebUtilNonPathValueTest {

	@Test
	@DisplayName("URL 을 조용히 바꾼다 — 주소검색 키워드가 훼손됐다")
	void URL_훼손() {
		String url = "http://www.juso.go.kr/addrlink/addrLinkApi.do?keyword=a..b";

		assertEquals("http://www.juso.go.kr/addrlink/addrLinkApi.do?keyword=ab",
				EgovWebUtil.filePathBlackList(url), "키워드의 점 두 개가 사라진다");
	}

	@Test
	@DisplayName("도메인도 바꾼다")
	void 도메인_훼손() {
		assertEquals("http://ab.example.com", EgovWebUtil.filePathBlackList("http://a..b.example.com"));
	}

	@Test
	@DisplayName("IP 에는 아무 일도 하지 않는다 — 형식 검증이 아니다")
	void IP_는_무의미() {
		List<String> inputs = Arrays.asList("192.168.0.1", "999.999.999.999", "not-an-ip", "'; DROP TABLE--");

		for (String in : inputs) {
			assertEquals(in, EgovWebUtil.filePathBlackList(in), "값이 그대로 통과한다: " + in);
		}
	}

	@Test
	@DisplayName("IP 형태 검사는 별도 메서드가 있다 — 다만 옥텟 범위는 보지 않는다")
	void IP_형태_검사는_따로있다() {
		assertTrue(EgovWebUtil.isIPAddress("192.168.0.1"));
		assertTrue(!EgovWebUtil.isIPAddress("not-an-ip"));

		assertTrue(EgovWebUtil.isIPAddress("999.999.999.999"),
				"자릿수 형태만 보는 정규식이라 범위를 벗어난 값도 통과한다 — 실측");
	}

	@Test
	@DisplayName("화이트리스트 완전일치 앞에서는 없던 일치를 만들어낸다")
	void 없던_일치를_만든다() {
		String whitelisted = "egovframework/com/uss/ion/pwm/popup";
		String attack = "egovframework/com/uss/ion/pwm/po..pup";

		assertNotEquals(whitelisted, attack, "원본은 화이트리스트와 다르다");
		assertEquals(whitelisted, EgovWebUtil.filePathBlackList(attack),
				"살균 후에는 일치해 버린다 — 방어가 아니라 우회 통로다");
	}

	@Test
	@DisplayName("경로 조작을 막지 않는다 — 지울 뿐이다")
	void 차단이_아니라_삭제() {
		assertEquals("/etc/passwd", EgovWebUtil.filePathBlackList("/etc/passwd"),
				"절대 경로는 손대지 않는다");
		assertEquals("//etc/passwd", EgovWebUtil.filePathBlackList("../../etc/passwd"),
				"상위 표기를 지울 뿐 거부하지 않는다");
	}
}
