package egovframework.com.cmm.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockPart;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * 파일 업로드 검증을 실제 multipart 요청으로 확인한다 — 실행환경 {@code EgovUploadPolicy} 이전(A-07) 검증.
 *
 * <p>기준 설정은 {@code globals.properties} 의
 * {@code Globals.fileUpload.Extensions = .gif,.jpg,.jpeg,.png,.xls,.xlsx} 다.
 * 화이트리스트를 바꾸면 이 테스트의 기대값도 함께 바뀐다.</p>
 *
 * <p><b>실행 조건.</b> {@code spring-test} 의 목 객체는 Jakarta Servlet 6.0 API 를 요구하는데
 * 기본 빌드는 5.0.0 을 쓴다. {@code -Ptest} 프로파일이 6.0.0 으로 올려주므로 그 프로파일로 실행해야 한다.</p>
 *
 * <p><b>경로가 섞인 파일명이 통과하는 것은 정상이다.</b> {@code EgovMultipartFiles.fileNameOf} 가
 * 경로를 떼고 이름만 판정하기 때문이다({@code ../../evil.png} → {@code evil.png}).
 * 저장 측인 {@code EgovFileMngUtil} 은 원본 이름을 경로에 쓰지 않고
 * {@code keyStr + UUID + 일련번호}(실행환경 {@code EgovStoredFileNames})로 새 이름을 만들므로 경로 이탈로 이어지지 않는다.</p>
 *
 * <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.09.04  실행환경팀        최초 생성 (EgovUploadPolicy 이전 검증)
 *   2026.09.10  실행환경팀        실행환경 PR 판(보안 재검증 반영)에 맞춤 — ::$DATA 는 INVALID_FILENAME 으로 거부
 * </pre>
 */
class EgovMultipartResolverTest {

	/** 역슬래시 한 글자. 소스에 직접 쓰면 이스케이프가 꼬여 char 로 만든다. */
	private static final String BS = String.valueOf((char) 92);

	private final EgovMultipartResolver resolver = new EgovMultipartResolver();

	@AfterEach
	void resetLocale() {
		Locale.setDefault(Locale.KOREA);
	}

	private MultipartHttpServletRequest resolve(String... fileNames) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/upload.do");
		request.setContentType("multipart/form-data; boundary=----test");
		int index = 0;
		for (String fileName : fileNames) {
			request.addPart(new MockPart("file" + index++, fileName,
					"CONTENT".getBytes(StandardCharsets.UTF_8)));
		}
		return resolver.resolveMultipart(request);
	}

	/** 내용이 0바이트인 파트 — 사용자가 파일 입력칸을 비워 둔 채 보낸 경우다. */
	private MultipartHttpServletRequest resolveWithEmptyPart(String... filledFileNames) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/upload.do");
		request.setContentType("multipart/form-data; boundary=----test");
		request.addPart(new MockPart("emptyInput", "", new byte[0]));
		int index = 0;
		for (String fileName : filledFileNames) {
			request.addPart(new MockPart("file" + index++, fileName,
					"CONTENT".getBytes(StandardCharsets.UTF_8)));
		}
		return resolver.resolveMultipart(request);
	}

	private String rejectionOf(String... fileNames) {
		SecurityException e = assertThrows(SecurityException.class, () -> resolve(fileNames));
		return e.getMessage();
	}

	@Test
	@DisplayName("허용 확장자는 통과한다")
	void 허용_확장자_통과() {
		assertEquals(1, resolve("photo.png").getMultiFileMap().size());
		assertEquals(3, resolve("a.png", "b.jpg", "c.gif").getMultiFileMap().size());
	}

	@Test
	@DisplayName("한글 파일명도 통과한다")
	void 한글_파일명() {
		assertEquals(1, resolve("보고서.xlsx").getMultiFileMap().size());
	}

	@Test
	@DisplayName("대문자 확장자도 통과한다 — 화이트리스트는 소문자로만 적혀 있다")
	void 대문자_확장자() {
		assertEquals(1, resolve("PHOTO.PNG").getMultiFileMap().size());
	}

	@Test
	@DisplayName("비워 둔 파일 입력칸이 섞여도 통과한다 — 정책의 빈 파일 거부가 여기 걸리면 안 된다")
	void 비워둔_입력칸() {
		assertDoesNotThrow(() -> resolveWithEmptyPart());
		assertDoesNotThrow(() -> resolveWithEmptyPart("photo.png"));
	}

	@Test
	@DisplayName("파일이 없으면 통과한다")
	void 파일_없는_요청() {
		assertEquals(0, resolve().getMultiFileMap().size());
	}

	@Test
	@DisplayName("허용되지 않은 확장자는 SecurityException 으로 거부한다")
	void 금지_확장자_거부() {
		assertTrue(rejectionOf("evil.jsp").contains("EXTENSION_NOT_ALLOWED"));
	}

	@Test
	@DisplayName("대소문자를 섞어도 우회되지 않는다")
	void 대소문자_우회_불가() {
		assertTrue(rejectionOf("evil.JsP").contains("EXTENSION_NOT_ALLOWED"));
	}

	@Test
	@DisplayName("확장자 뒤에 점·공백을 붙여도 우회되지 않는다")
	void 끝문자_우회_불가() {
		assertTrue(rejectionOf("evil.jsp.").contains("EXTENSION_NOT_ALLOWED"));
		assertTrue(rejectionOf("evil.jsp ").contains("EXTENSION_NOT_ALLOWED"));
		// 대체 데이터 스트림 표기(::$DATA)는 실행환경 EgovUploadPolicy 가 ':' 자체를 INVALID_FILENAME 으로 거부한다
		// (2026.09.07 보안 재검증 — Windows 드라이브 접두·ADS 표기의 원천 차단). 확장자 판정까지 가지 않는다.
		assertTrue(rejectionOf("evil.jsp::$DATA").contains("INVALID_FILENAME"));
		assertTrue(rejectionOf("evil.jsp..").contains("NO_EXTENSION"));
	}

	@Test
	@DisplayName("확장자가 없으면 거부한다")
	void 확장자_없음_거부() {
		assertTrue(rejectionOf("readme").contains("NO_EXTENSION"));
	}

	@Test
	@DisplayName("널바이트가 섞인 파일명은 거부한다")
	void 널바이트_거부() {
		assertTrue(rejectionOf("evil.jsp" + (char) 0 + ".png").contains("INVALID_FILENAME"));
	}

	@Test
	@DisplayName("여러 파일 중 하나만 위반해도 요청 전체를 거부한다")
	void 하나만_위반해도_전체_거부() {
		assertTrue(rejectionOf("ok.png", "evil.jsp").contains("EXTENSION_NOT_ALLOWED"));
	}

	@Test
	@DisplayName("경로가 섞인 파일명은 경로를 뗀 이름으로 판정한다 — 저장 이름은 따로 생성되므로 이탈하지 않는다")
	void 경로는_떼고_판정한다() {
		assertEquals(1, resolve("../../evil.png").getMultiFileMap().size());
		assertEquals(1, resolve(".." + BS + ".." + BS + "evil.png").getMultiFileMap().size());
		assertTrue(rejectionOf("../../evil.jsp").contains("EXTENSION_NOT_ALLOWED"),
				"경로를 떼어도 확장자 판정은 그대로 걸려야 한다");
	}

	@Test
	@DisplayName("판정이 기본 로케일(Locale)에 흔들리지 않는다 — 터키어 로케일에서 확인")
	void 로케일_무관() {
		Locale.setDefault(new Locale("tr", "TR"));

		assertEquals(1, resolve("PHOTO.PNG").getMultiFileMap().size());
		assertEquals(1, resolve("IMAGE.GIF").getMultiFileMap().size());
		assertTrue(rejectionOf("evil.JSP").contains("EXTENSION_NOT_ALLOWED"));
	}
}
