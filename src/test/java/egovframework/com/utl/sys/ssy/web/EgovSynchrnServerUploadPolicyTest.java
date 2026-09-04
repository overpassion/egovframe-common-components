package egovframework.com.utl.sys.ssy.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Optional;

import org.egovframe.rte.fdl.filehandling.upload.EgovUploadPolicy;
import org.egovframe.rte.ptl.mvc.upload.EgovMultipartFiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 동기화 서버 파일 업로드의 거부 판정 검증.
 *
 * <p>확장자 검사({@code checkFileExtension})와 크기 검사({@code checkFileMaxSize}) 두 벌을
 * 실행환경 {@link EgovUploadPolicy} 하나로 합쳤다. 판정 결과와 화면 메시지가
 * 종전과 어긋나지 않는지 고정한다. 컨트롤러의 {@code private} 메서드를 리플렉션으로 부른다 —
 * Spring 컨텍스트와 DB 없이 판정 로직만 보기 위해서다.</p>
 *
 * <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.09.04  실행환경팀        최초 생성 (EgovUploadPolicy 이전 검증)
 * </pre>
 */
class EgovSynchrnServerUploadPolicyTest {

	/** 컨트롤러가 설정에서 만드는 것과 같은 정책. globals.properties 의 값을 그대로 쓴다. */
	private static final EgovUploadPolicy POLICY = EgovUploadPolicy.builder()
			.allowExtensionList(".gif,.jpg,.jpeg,.png,.xls,.xlsx")
			.maxFileSize(104857600L)
			.build();

	private static MultipartFile file(String name, int size) {
		return new MockMultipartFile("file", name, null, new byte[size]);
	}

	private static Optional<EgovUploadPolicy.Reason> check(String name, int size) {
		return EgovMultipartFiles.check(POLICY, file(name, size));
	}

	/** 컨트롤러의 rejectMessage 를 그대로 부른다. */
	private static String message(EgovUploadPolicy.Reason reason, String fileName, long fileSize) throws Exception {
		Method m = EgovSynchrnServerController.class.getDeclaredMethod(
				"rejectMessage", EgovUploadPolicy.Reason.class, String.class, long.class, EgovUploadPolicy.class);
		m.setAccessible(true);
		return (String) m.invoke(new EgovSynchrnServerController(), reason, fileName, fileSize, POLICY);
	}

	@Test
	@DisplayName("허용 확장자는 통과한다")
	void 허용_확장자() {
		assertTrue(check("photo.png", 10).isEmpty());
		assertTrue(check("보고서.xlsx", 10).isEmpty());
		assertTrue(check("PHOTO.PNG", 10).isEmpty());
	}

	@Test
	@DisplayName("허용되지 않은 확장자는 거부한다 — 종전과 같은 판정")
	void 금지_확장자() {
		assertEquals(EgovUploadPolicy.Reason.EXTENSION_NOT_ALLOWED, check("evil.jsp", 10).orElse(null));
	}

	@Test
	@DisplayName("최대 크기를 넘으면 거부한다")
	void 크기_초과() {
		EgovUploadPolicy small = EgovUploadPolicy.builder()
				.allowExtensionList(".png")
				.maxFileSize(99L)
				.build();

		assertTrue(EgovMultipartFiles.check(small, file("photo.png", 99)).isEmpty(), "경계값은 허용");
		assertEquals(EgovUploadPolicy.Reason.SIZE_EXCEEDED,
				EgovMultipartFiles.check(small, file("photo.png", 100)).orElse(null));
	}

	@Test
	@DisplayName("빈 파일은 거부한다 — 종전에는 두 검사를 통과해 writeFile 에서 터졌다")
	void 빈_파일() {
		assertEquals(EgovUploadPolicy.Reason.EMPTY_FILE, check("photo.png", 0).orElse(null));
	}

	@Test
	@DisplayName("확장자가 없으면 거부한다 — 종전에는 이름 전체를 확장자로 봤다")
	void 확장자_없음() {
		assertEquals(EgovUploadPolicy.Reason.NO_EXTENSION, check("readme", 10).orElse(null));
		assertEquals(EgovUploadPolicy.Reason.NO_EXTENSION, check("png", 10).orElse(null));
	}

	@Test
	@DisplayName("크기 초과 메시지는 종전 문구를 그대로 쓴다")
	void 크기_메시지() throws Exception {
		String msg = message(EgovUploadPolicy.Reason.SIZE_EXCEEDED, "big.png", 200L);

		assertEquals("* 허용되지 않는 파일 사이즈 입니다.[big.png : 200 bytes / 104857600 bytes]", msg);
	}

	@Test
	@DisplayName("확장자 거부 메시지는 종전 문구를 그대로 쓴다")
	void 확장자_메시지() throws Exception {
		assertEquals("* 허용되지 않는 확장자 입니다.[jsp]",
				message(EgovUploadPolicy.Reason.EXTENSION_NOT_ALLOWED, "evil.jsp", 10L));
	}

	@Test
	@DisplayName("빈 파일은 확장자 문구 대신 파일 선택 안내를 낸다")
	void 빈_파일_메시지() throws Exception {
		assertEquals("* 업로드할 파일을 선택하세요.",
				message(EgovUploadPolicy.Reason.EMPTY_FILE, "", 0L));
	}
}
