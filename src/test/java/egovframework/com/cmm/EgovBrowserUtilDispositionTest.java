package egovframework.com.cmm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.egovframe.rte.fdl.filehandling.EgovContentDispositions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link EgovBrowserUtil#getDisposition(String, String, String)} 이 실행환경 EgovContentDispositions 위임으로 바뀐 뒤의
 * 계약을 고정한다.
 *
 * <p>종전에는 판별되지 않는 브라우저(예: curl·모바일 앱)에서 {@code RuntimeException("Not supported browser")} 로
 * 다운로드가 막혔고, IE 8 이하에는 값 앞에 헤더 이름까지 붙은 잘못된 형식이 나갔으며, 그 외 브라우저에는 ASCII 폴백 없는
 * {@code filename*} 만 실렸다.</p>
 */
@SuppressWarnings("deprecation")
class EgovBrowserUtilDispositionTest {

	private static final String CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36";
	private static final String IE8 = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1)";
	private static final String UNKNOWN = "curl/8.5.0";

	@Test
	@DisplayName("판별되지 않는 브라우저에서도 예외 없이 표준 헤더를 만든다")
	void unknownBrowserIsSupported() {
		String header = EgovBrowserUtil.getDisposition("보고서.hwp", UNKNOWN, "UTF-8");

		assertEquals(EgovContentDispositions.attachment("보고서.hwp"), header);
	}

	@Test
	@DisplayName("브라우저 종류와 무관하게 같은 값이다 — RFC 6266 형식 하나로 통일")
	void sameForEveryBrowser() {
		String expected = EgovContentDispositions.attachment("보고서.hwp");

		assertEquals(expected, EgovBrowserUtil.getDisposition("보고서.hwp", CHROME, "UTF-8"));
		assertEquals(expected, EgovBrowserUtil.getDisposition("보고서.hwp", IE8, "UTF-8"));
		assertFalse(expected.startsWith("Content-Disposition:"), "헤더 이름이 값에 섞이면 안 된다");
	}

	@Test
	@DisplayName("한글 파일명은 filename* 인코딩과 ASCII 폴백을 함께 싣는다")
	void koreanFileName() {
		String header = EgovBrowserUtil.getDisposition("보고서.hwp", CHROME, "UTF-8");

		assertTrue(header.startsWith("attachment; filename=\"___.hwp\""), header);
		assertTrue(header.endsWith("filename*=UTF-8''%EB%B3%B4%EA%B3%A0%EC%84%9C.hwp"), header);
	}

	@Test
	@DisplayName("ASCII 파일명은 종전 형식(filename=\"...\")을 유지한다")
	void asciiFileName() {
		assertEquals("attachment; filename=\"report.pdf\"", EgovBrowserUtil.getDisposition("report.pdf", CHROME, "UTF-8"));
	}

	@Test
	@DisplayName("CR/LF 는 헤더에 실리지 않는다")
	void noHeaderInjection() {
		String header = EgovBrowserUtil.getDisposition("a.txt\r\nSet-Cookie: x=1", CHROME, "UTF-8");

		assertFalse(header.contains("\r") || header.contains("\n"), header);
	}
}
