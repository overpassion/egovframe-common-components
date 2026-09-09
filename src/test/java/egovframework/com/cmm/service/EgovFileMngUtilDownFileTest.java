package egovframework.com.cmm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.fdl.filehandling.EgovContentDispositions;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

/**
 * downFile(HttpServletRequest, HttpServletResponse)의 원본 파일명 처리(resolveRequestAttribute,
 * buildContentDispositionHeader)만 검증한다. downFile() 자체는 클래스 로딩 시점에 고정되는 정적
 * Globals.fileStorePath(테스트 설정상 "C:/egovframework/upload/")를 기준으로 실제 파일 존재 여부를 확인하므로
 * 이번 범위에서 제외했다.
 *
 * <p>MockHttpServletRequest(spring-test)는 Servlet 6.0 이상을 요구하므로, Servlet 5.0 환경과
 * 호환되는 JDK Proxy 기반 스텁을 사용한다.</p>
 */
class EgovFileMngUtilDownFileTest {

	@Test
	void buildContentDispositionHeader_reflectsOrginFileAttribute() throws Exception {
		HttpServletRequest request = stubRequest("orginFile", "원본파일.txt");

		String header = EgovFileMngUtil.buildContentDispositionHeader(request);

		// 실행환경 EgovContentDispositions 형식: 한글은 RFC 5987 filename* 로 인코딩하고 ASCII 폴백을 병기한다
		assertEquals(EgovContentDispositions.attachment("원본파일.txt"), header);
		assertTrue(header.startsWith("attachment; filename=\""), header);
		assertTrue(header.contains("filename*=UTF-8''%EC%9B%90%EB%B3%B8%ED%8C%8C%EC%9D%BC.txt"), header);
	}

	@Test
	void buildContentDispositionHeader_asciiFileName_keepsLegacyShape() throws Exception {
		HttpServletRequest request = stubRequest("orginFile", "report.pdf");

		assertEquals("attachment; filename=\"report.pdf\"", EgovFileMngUtil.buildContentDispositionHeader(request));
	}

	@Test
	void buildContentDispositionHeader_stripsCarriageReturnAndNewLine() throws Exception {
		HttpServletRequest request = stubRequest("orginFile", "원본\r\n파일.txt");

		String header = EgovFileMngUtil.buildContentDispositionHeader(request);

		assertEquals(EgovContentDispositions.attachment("원본파일.txt"), header);
		assertFalse(header.contains("\r") || header.contains("\n"), "헤더 인젝션 문자가 남으면 안 된다");
	}

	@Test
	void buildContentDispositionHeader_pathIsReducedToFileName() throws Exception {
		HttpServletRequest request = stubRequest("orginFile", "..\\..\\etc\\passwd.txt");

		assertEquals("attachment; filename=\"passwd.txt\"", EgovFileMngUtil.buildContentDispositionHeader(request));
	}

	@Test
	void buildContentDispositionHeader_attachmentOnly_whenAttributeMissing() throws Exception {
		HttpServletRequest request = stubRequest();

		// 종전 "attachment; filename=" 처럼 값이 빈 파라미터를 내지 않는다
		assertEquals("attachment", EgovFileMngUtil.buildContentDispositionHeader(request));
	}

	@Test
	void contentDispositionOf_attachmentOnly_whenNothingUsableRemains() {
		assertEquals("attachment", EgovFileMngUtil.contentDispositionOf("\r\n"));
		assertEquals("attachment", EgovFileMngUtil.contentDispositionOf("   "));
		assertEquals("attachment", EgovFileMngUtil.contentDispositionOf(null));
	}

	@Test
	void resolveRequestAttribute_returnsValue_whenPresent() {
		HttpServletRequest request = stubRequest("downFile", "stored-name.dat");

		assertEquals("stored-name.dat", EgovFileMngUtil.resolveRequestAttribute(request, "downFile"));
	}

	@Test
	void resolveRequestAttribute_returnsEmptyString_whenAbsent() {
		HttpServletRequest request = stubRequest();

		assertEquals("", EgovFileMngUtil.resolveRequestAttribute(request, "downFile"));
	}

	// -----------------------------------------------------------------------
	// 헬퍼: JDK Proxy 기반 HttpServletRequest 스텁 (Servlet 5.0 호환)
	// getAttribute(name)만 구현하고 나머지는 null 반환
	// -----------------------------------------------------------------------
	private static HttpServletRequest stubRequest(String... attributePairs) {
		Map<String, String> attributes = new HashMap<>();
		for (int i = 0; i + 1 < attributePairs.length; i += 2) {
			attributes.put(attributePairs[i], attributePairs[i + 1]);
		}
		return (HttpServletRequest) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> {
					if ("getAttribute".equals(method.getName())) {
						return attributes.get((String) args[0]);
					}
					return null;
				});
	}
}
